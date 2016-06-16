/*
 * Copyright (c) 2016, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.trackercapture;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.hisp.dhis.android.trackercapture.views.HomeActivity;
import org.hisp.dhis.client.sdk.android.api.D2;
import org.hisp.dhis.client.sdk.android.api.utils.LoggerImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultAppModule;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultUserModule;
import org.hisp.dhis.client.sdk.ui.bindings.commons.Inject;
import org.hisp.dhis.client.sdk.ui.bindings.commons.NavigationHandler;
import org.hisp.dhis.client.sdk.ui.bindings.views.DefaultLoginActivity;
import org.hisp.dhis.client.sdk.utils.Logger;

import io.fabric.sdk.android.Fabric;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class TrackerCaptureApp extends Application {
    private AppComponent appComponent;
    private UserComponent userComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        init(this);

        // enabling crashlytics only for release builds
        Crashlytics crashlytics = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder()
                        .disabled(BuildConfig.DEBUG)
                        .build())
                .build();
        Fabric.with(this, crashlytics);

        final String authority = getString(R.string.authority);
        final String accountType = getString(R.string.account_type);

        final AppModule appModule = new AppModule(this);
        final UserModule userModule = new UserModule(authority, accountType);

        // Global dependency graph
        appComponent = DaggerAppComponent.builder()
                .appModule(appModule)
                .build();

        // adding UserComponent to global dependency graph
        userComponent = appComponent.plus(userModule);

        Inject.init(new Inject.ModuleProvider() {

            @Override
            public DefaultAppModule provideAppModule() {
                return appModule;
            }

            @Override
            public DefaultUserModule provideUserModule(String serverUrl) {
                UserModule userModule = new UserModule(serverUrl, authority, accountType);

                // creating new component
                userComponent = appComponent.plus(userModule);

                // return user module
                return userModule;
            }
        });

        NavigationHandler.loginActivity(DefaultLoginActivity.class);
        NavigationHandler.homeActivity(HomeActivity.class);
    }

    @Override
    protected void attachBaseContext(Context baseContext) {
        super.attachBaseContext(baseContext);
        MultiDex.install(this);
    }

    private void init(Context context) {
        OkHttpClient okHttpClient = providesOkHttpClient();
        D2.Flavor flavor = providesFlavor(okHttpClient, new LoggerImpl());

        D2.init(context, flavor);
    }

    private OkHttpClient providesOkHttpClient() {
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            return new OkHttpClient.Builder()
                    // .addNetworkInterceptor(new StethoInterceptor())
                    .addInterceptor(loggingInterceptor)
                    .build();
        }

        return new OkHttpClient();
    }

    private D2.Flavor providesFlavor(OkHttpClient okHttpClient, Logger logger) {
        return new D2.Builder()
                .okHttp(okHttpClient)
                .logger(logger)
                .build();
    }

    public UserComponent getUserComponent() {
        return userComponent;
    }
}
