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

import org.hisp.dhis.client.sdk.android.api.utils.LoggerImpl;
import org.hisp.dhis.client.sdk.ui.AppPreferences;
import org.hisp.dhis.client.sdk.ui.AppPreferencesImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.ApiExceptionHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.ApiExceptionHandlerImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultAppModule;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SessionPreferences;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SessionPreferencesImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SyncDateWrapper;
import org.hisp.dhis.client.sdk.utils.Logger;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AppModule implements DefaultAppModule {
    private final Application application;

    public AppModule(Application application) {
        this.application = application;
    }

    @Provides
    @Singleton
    @Override
    public Context providesContext() {
        return application;
    }

    @Provides
    @Singleton
    @Override
    public Logger providesLogger() {
        return new LoggerImpl();
    }

    @Provides
    @Singleton
    @Override
    public ApiExceptionHandler providesApiExceptionHandler(Context context, Logger logger) {
        return new ApiExceptionHandlerImpl(context, logger);
    }

    @Provides
    @Singleton
    @Override
    public AppPreferences providesApplicationPreferences(Context context) {
        return new AppPreferencesImpl(context);
    }

    @Provides
    @Singleton
    @Override
    public SessionPreferences providesSessionPreferences(Context context) {
        return new SessionPreferencesImpl(context);
    }

    @Provides
    @Singleton
    @Override
    public SyncDateWrapper providesSyncDateWrapper(Context context, AppPreferences preferences, Logger logger) {
        return new SyncDateWrapper(context, preferences);
    }
}
