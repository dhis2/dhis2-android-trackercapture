/*
 *  Copyright (c) 2015, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis2.android.trackercapture;

import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import org.hisp.dhis2.android.sdk.activities.INavigationHandler;
import org.hisp.dhis2.android.sdk.activities.LoginActivity;
import org.hisp.dhis2.android.sdk.activities.OnBackPressedListener;
import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.fragments.LoadingFragment;
import org.hisp.dhis2.android.sdk.fragments.SettingsFragment;
import org.hisp.dhis2.android.sdk.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis2.android.sdk.network.managers.NetworkManager;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.trackercapture.fragments.EnrollmentFragment;
import org.hisp.dhis2.android.trackercapture.fragments.ProgramOverviewFragment;
import org.hisp.dhis2.android.trackercapture.fragments.SelectProgramFragment;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.UpcomingEventsFragment;


public class MainActivity extends AppCompatActivity implements INavigationHandler {
    public final static String TAG = MainActivity.class.getSimpleName();
    private OnBackPressedListener mBackPressedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Dhis2.getInstance().enableLoading(this, Dhis2.LOAD_TRACKER);
        NetworkManager.getInstance().setCredentials(Dhis2.getCredentials(this));
        NetworkManager.getInstance().setServerUrl(Dhis2.getServer(this));
        Dhis2Application.bus.register(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar);

        Dhis2.activatePeriodicSynchronizer(this);
        if (Dhis2.isInitialDataLoaded(this)) {
            showSelectProgramFragment();
        } else if (Dhis2.isLoadingInitial()) {
            showLoadingFragment();
        } else {
            loadInitialData();
        }
    }

    public void loadInitialData() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoadingFragment();
            }
        });
        Dhis2.loadInitialData(this);
    }

    @Subscribe
    public void onReceiveMessage(MessageEvent event) {
        Log.d(TAG, "onReceiveMessage");
        if (event.eventType == BaseEvent.EventType.onLoadingInitialDataFinished) {
            if (Dhis2.isInitialDataLoaded(this)) {
                showSelectProgramFragment();
            } else {
                //todo: notify the user that data is missing and request to try to re-load.
                showSelectProgramFragment();
            }
        } else if (event.eventType == BaseEvent.EventType.loadInitialDataFailed) {
            startActivity(new Intent(MainActivity.this,
                    LoginActivity.class));
            finish();
        }
    }

    public void showLoadingFragment() {
        setTitle("Loading initial data");
        switchFragment(new LoadingFragment(), LoadingFragment.TAG, false);
    }

    public void showSelectProgramFragment() {
        setTitle("Tracker Capture");
        switchFragment(new SelectProgramFragment(), SelectProgramFragment.TAG, true);
    }

    @Override
    public void onBackPressed() {
        if (mBackPressedListener != null) {
            mBackPressedListener.doBack();
            return;
        }

        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            super.onBackPressed();
        } else {
            finish();
        }
    }

    public void setBackPressedListener(OnBackPressedListener listener) {
        mBackPressedListener = listener;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Dhis2Application.bus.unregister(this);
    }

    @Override
    public void switchFragment(Fragment fragment, String fragmentTag, boolean addToBackStack) {
        if (fragment != null) {
            FragmentTransaction transaction =
                    getSupportFragmentManager().beginTransaction();

            transaction
                    .setCustomAnimations(R.anim.open_enter, R.anim.open_exit)
                    .replace(R.id.fragment_container, fragment);
            if (addToBackStack) {
                transaction = transaction
                        .addToBackStack(fragmentTag);
            }

            transaction.commit();
        }
    }
}

