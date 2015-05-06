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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import org.hisp.dhis2.android.sdk.activities.LoginActivity;
import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.fragments.LoadingFragment;
import org.hisp.dhis2.android.sdk.fragments.SettingsFragment;
import org.hisp.dhis2.android.sdk.network.managers.NetworkManager;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.trackercapture.fragments.DataEntryFragment;
import org.hisp.dhis2.android.trackercapture.fragments.EnrollmentFragment;
import org.hisp.dhis2.android.trackercapture.fragments.ProgramOverviewFragment;
import org.hisp.dhis2.android.trackercapture.fragments.SelectProgramFragment;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.UpcomingEventsFragment;


public class MainActivity extends AppCompatActivity implements INavigationHandler {

    public final static String CLASS_TAG = "MainActivity";

    private CharSequence title;

    private Fragment currentFragment = null;
    private SelectProgramFragment selectProgramFragment;
    private ProgramOverviewFragment programOverviewFragment;
    private DataEntryFragment dataEntryFragment;
    private SettingsFragment settingsFragment;
    private LoadingFragment loadingFragment;
    private EnrollmentFragment enrollmentFragment;
    private Fragment previousFragment; //workaround for back button since the backstack sucks
    private int lastSelectedOrgUnit = 0;
    private int lastSelectedProgram = 0;

    private OrganisationUnit currentlySelectedOrganisationUnit;
    private Program currentlySelectedProgram;
    private ProgramStage currentlySelectedProgramStage;
    private Enrollment currentlySelectedEnrollment;
    private TrackedEntityInstance currentlySelectedTrackedEntityInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_sdk);

        Dhis2.getInstance().enableLoading(this, Dhis2.LOAD_TRACKER);
        NetworkManager.getInstance().setCredentials(Dhis2.getCredentials(this));
        NetworkManager.getInstance().setServerUrl(Dhis2.getServer(this));
        Dhis2Application.bus.register(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Dhis2.isInitialDataLoaded(this)) {
            showSelectProgramFragment();
        } else if (Dhis2.isLoadingInitial()) {
            showLoadingFragment();
        } else {
            loadInitialData();
        }
        Dhis2.activatePeriodicSynchronizer(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showSettingsFragment();
        } else if(id == R.id.action_save_event) {
            if (currentFragment.equals(dataEntryFragment )) {
                dataEntryFragment.submit();
            } else if(currentFragment.equals(enrollmentFragment)) {
                enrollmentFragment.submit();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle( CharSequence title )
    {
        this.title = title;
        runOnUiThread(new Runnable() {
            public void run() {
                getSupportActionBar().setTitle( MainActivity.this.title );
            }
        });
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
        Log.d(CLASS_TAG, "onreceivemessage" + event.eventType);

        if(event.eventType == BaseEvent.EventType.showSelectProgramFragment) {
            showSelectProgramFragment();
        } else if(event.eventType == BaseEvent.EventType.showProgramOverviewFragment) {
            showProgramOverviewFragment();
        } else if(event.eventType == BaseEvent.EventType.showDataEntryFragment) {
            showDataEntryFragment();
        } else if(event.eventType == BaseEvent.EventType.showEnrollmentFragment) {
            showEnrollmentFragment();
        } else if(event.eventType == BaseEvent.EventType.showUpcomingEventsFragment) {
            showUpcomingEventsFragment();
        }
        else if(event.eventType == BaseEvent.EventType.showPreviousFragment) {
            showPreviousFragment();
        } else if(event.eventType == BaseEvent.EventType.logout) {
            logout();
        } else if(event.eventType == BaseEvent.EventType.onLoadingInitialDataFinished) {
            if(Dhis2.isInitialDataLoaded(this)) {
                showSelectProgramFragment();
            } else {
                //todo: notify the user that data is missing and request to try to re-load.
            }
        } else if(event.eventType == BaseEvent.EventType.loadInitialDataFailed) {
            showLoginActivity();
        }
    }

    public void logout() {
        Dhis2.logout(this);
        showLoginActivity();
    }

    public void showLoginActivity() {
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(i);
        finish();
    }

    public void showLoadingFragment() {
        setTitle("Loading initial data");
        if(loadingFragment == null) loadingFragment = new LoadingFragment();
        showFragment(loadingFragment);
    }

    public void showSelectProgramFragment() {
        setTitle("Tracker Capture");
        if(selectProgramFragment == null) selectProgramFragment = new SelectProgramFragment();
        showFragment(selectProgramFragment);
        selectProgramFragment.setSelection(lastSelectedOrgUnit, lastSelectedProgram);
    }

    public void showProgramOverviewFragment() {
        setTitle(getString(R.string.program));
        if(programOverviewFragment == null) programOverviewFragment = new ProgramOverviewFragment();
        currentlySelectedOrganisationUnit = selectProgramFragment.getSelectedOrganisationUnit();
        currentlySelectedProgram = selectProgramFragment.getSelectedProgram();
        currentlySelectedTrackedEntityInstance = selectProgramFragment.getSelectedTrackedEntityInstance();
        programOverviewFragment.setSelectedOrganisationUnit(currentlySelectedOrganisationUnit);
        programOverviewFragment.setSelectedProgram(currentlySelectedProgram);
        programOverviewFragment.setSelectedTrackedEntityInstance(currentlySelectedTrackedEntityInstance);
        showFragment(programOverviewFragment);
    }

    public void showSettingsFragment() {
        setTitle("Settings");
        if( settingsFragment == null ) settingsFragment = new SettingsFragment();
        showFragment(settingsFragment);
    }

    public void showDataEntryFragment() {
        dataEntryFragment = new DataEntryFragment();
        setTitle(programOverviewFragment.getSelectedProgramStage().name);
        dataEntryFragment.setSelectedOrganisationUnit(programOverviewFragment.getSelectedOrganisationUnit());
        if(programOverviewFragment.getSelectedEvent() != null)
            dataEntryFragment.setEditingEvent(programOverviewFragment.getSelectedEvent().localId);
        dataEntryFragment.setCurrentTrackedEntityInstance(programOverviewFragment.getSelectedTrackedEntityInstance());
        dataEntryFragment.setSelectedProgramStage(programOverviewFragment.getSelectedProgramStage());
        dataEntryFragment.setCurrentEnrollment(programOverviewFragment.getCurrentEnrollment());
        showFragment(dataEntryFragment);
    }

    public void showEnrollmentFragment() {
        enrollmentFragment = new EnrollmentFragment();
        if(currentFragment.equals(selectProgramFragment)) {
            enrollmentFragment.setCurrentTrackedEntityInstance(null);
            enrollmentFragment.setSelectedOrganisationUnit(selectProgramFragment.getSelectedOrganisationUnit());
            enrollmentFragment.setSelectedProgram(selectProgramFragment.getSelectedProgram());
        } else if(currentFragment.equals(programOverviewFragment)) {
            enrollmentFragment.setSelectedOrganisationUnit(programOverviewFragment.getSelectedOrganisationUnit());
            enrollmentFragment.setSelectedProgram(programOverviewFragment.getSelectedProgram());
            enrollmentFragment.setCurrentTrackedEntityInstance(programOverviewFragment.getSelectedTrackedEntityInstance());
        }
        showFragment(enrollmentFragment);
    }

    public void showUpcomingEventsFragment() {
        UpcomingEventsFragment fragment = new UpcomingEventsFragment();
        showFragment(fragment);
    }

    public void showPreviousFragment() {
        if(previousFragment == null) {
            showSelectProgramFragment();
        } else {
            showFragment(previousFragment);
        }
    }

    public void showFragment(Fragment fragment) {
        if(MainActivity.this.isFinishing()) return;
        FragmentManager fragmentManager = getFragmentManager();
        //FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //fragmentTransaction.replace(R.id.fragment_container, fragment);
        //fragmentTransaction.commitAllowingStateLoss();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(fragment.getTag())
                .commit();
        previousFragment = currentFragment;
        currentFragment = fragment;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_save_event);
        item.setVisible(true);
        if(currentFragment == null) return true;
        if(currentFragment instanceof SettingsFragment)
            item.setVisible(false);
        else if(currentFragment instanceof SelectProgramFragment)
            item.setVisible(false);
        else if(currentFragment instanceof ProgramOverviewFragment)
            item.setVisible(false);
        else if(currentFragment instanceof DataEntryFragment)
            item.setIcon(getResources().getDrawable(R.drawable.ic_save));
        else if(currentFragment == loadingFragment)
            item.setVisible(false);
        else if(currentFragment instanceof UpcomingEventsFragment)
            item.setVisible(false);

        return true;
    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if ( (keyCode == KeyEvent.KEYCODE_BACK) )
        {
            if ( currentFragment == selectProgramFragment )
            {
                Dhis2.getInstance().showConfirmDialog(this, getString(R.string.confirm),
                        getString(R.string.exit_confirmation), getString(R.string.yes_option),
                        getString(R.string.no_option),
                 new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick( DialogInterface dialog, int which )
                    {
                        finish();
                        System.exit( 0 );
                    }
                } );
            } else if (currentFragment == dataEntryFragment ) {
                showProgramOverviewFragment();
            }
            else if ( currentFragment instanceof SettingsFragment ) {
                if(previousFragment == null) showSelectProgramFragment();
                else showFragment(previousFragment);
            } else if ( currentFragment instanceof ProgramOverviewFragment ) {
                showSelectProgramFragment();
            } else if(currentFragment == enrollmentFragment) {
                showProgramOverviewFragment();
            } else if(currentFragment instanceof UpcomingEventsFragment)
                showSelectProgramFragment();
            return true;
        }

        return super.onKeyDown( keyCode, event );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Dhis2Application.bus.unregister(this);
    }

    @Override
    public void switchFragment(Fragment fragment, String tag) {
        currentFragment = fragment;
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(tag)
                    .commit();
        }
    }
}
