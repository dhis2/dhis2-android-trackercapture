/*
 *  Copyright (c) 2016, University of Oslo
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

package org.hisp.dhis.android.trackercapture;

import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.view.View;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.DhisService;
import org.hisp.dhis.android.sdk.controllers.LoadingController;
import org.hisp.dhis.android.sdk.controllers.PeriodicSynchronizerController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.network.Session;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.UserAccount;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.utils.ScreenSizeConfigurator;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.SelectProgramFragment;
import org.hisp.dhis.client.sdk.ui.activities.AbsHomeActivity;
import org.hisp.dhis.client.sdk.ui.fragments.InformationFragment;
import org.hisp.dhis.client.sdk.ui.fragments.WrapperFragment;

public class MainActivity extends AbsHomeActivity {
    public final static String TAG = MainActivity.class.getSimpleName();

    private static final String APPS_DASHBOARD_PACKAGE =
            "org.hisp.dhis.android.dashboard";
    private static final String APPS_DATA_CAPTURE_PACKAGE =
            "org.dhis2.mobile";
    private static final String APPS_EVENT_CAPTURE_PACKAGE =
            "org.hisp.dhis.android.eventcapture";
    private static final String APPS_TRACKER_CAPTURE_PACKAGE =
            "org.hisp.dhis.android.trackercapture";
    private static final String APPS_TRACKER_CAPTURE_REPORTS_PACKAGE =
            "org.hispindia.bidtrackerreports";
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenSizeConfigurator.init(getWindowManager());

        boolean hasPermissionLocation = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionLocation) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        LoadingController.enableLoading(this, ResourceType.ASSIGNEDPROGRAMS);
        LoadingController.enableLoading(this, ResourceType.OPTIONSETS);
        LoadingController.enableLoading(this, ResourceType.PROGRAMS);
        LoadingController.enableLoading(this, ResourceType.CONSTANTS);
        LoadingController.enableLoading(this, ResourceType.PROGRAMRULES);
        LoadingController.enableLoading(this, ResourceType.PROGRAMRULEVARIABLES);
        LoadingController.enableLoading(this, ResourceType.PROGRAMRULEACTIONS);
        LoadingController.enableLoading(this, ResourceType.RELATIONSHIPTYPES);
        Dhis2Application.bus.register(this);

        PeriodicSynchronizerController.activatePeriodicSynchronizer(this);
        setUpNavigationView(savedInstanceState);
    }

    private void setUpNavigationView(Bundle savedInstanceState) {
        removeMenuItem(R.id.drawer_item_profile);
        addMenuItem(11, R.drawable.ic_add, R.string.enroll);
        if (savedInstanceState == null) {
            onNavigationItemSelected(getNavigationView().getMenu()
                    .findItem(11));
        }

        UserAccount userAccount = MetaDataController.getUserAccount();
        String name = "";
        if (userAccount != null) {
            if (!isEmpty(userAccount.getFirstName()) &&
                    !isEmpty(userAccount.getSurname())) {
                name = String.valueOf(userAccount.getFirstName().charAt(0)) +
                        String.valueOf(userAccount.getSurname().charAt(0));
            } else if (userAccount.getDisplayName() != null &&
                    userAccount.getDisplayName().length() > 1) {
                name = String.valueOf(userAccount.getDisplayName().charAt(0)) +
                        String.valueOf(userAccount.getDisplayName().charAt(1));
            }

            getUsernameTextView().setText(userAccount.getDisplayName());
            getUserInfoTextView().setText(userAccount.getEmail());
        }

        getUsernameLetterTextView().setText(name);
    }

    @NonNull
    @Override
    protected Fragment getProfileFragment() {
        return new Fragment();
//        return WrapperFragment.newInstance(ProfileFragment.class,
//                getString(R.string.drawer_item_profile));
    }

    @NonNull
    @Override
    protected Fragment getSettingsFragment() {
        return new Fragment();
//        return WrapperFragment.newInstance(SettingsFragment.class,
//                getString(R.string.drawer_item_settings));
    }

    @Override
    protected boolean onItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 11) {
            attachFragment(WrapperFragment.newInstance(SelectProgramFragment.class, getString(R.string.app_name)));
            return true;
        }
        return false;
    }

    public void loadInitialData() {
        DhisService.loadInitialData(MainActivity.this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.getEventBus().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ScreenSizeConfigurator.init(getWindowManager());
        Dhis2Application.getEventBus().register(this);
        loadInitialData();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ScreenSizeConfigurator.init(getWindowManager());
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        String lastSynced = DhisController.getInstance().getSyncDateWrapper().getLastSyncedString();
        setSynchronizedMessage(lastSynced);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        boolean isSelected = false;
        int menuItemId = menuItem.getItemId();

        if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_dashboard) {
            isSelected = openApp(APPS_DASHBOARD_PACKAGE);
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_data_capture) {
            isSelected = openApp(APPS_DATA_CAPTURE_PACKAGE);
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_event_capture) {
            isSelected = openApp(APPS_EVENT_CAPTURE_PACKAGE);
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_tracker_capture) {
            isSelected = openApp(APPS_TRACKER_CAPTURE_PACKAGE);
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_tracker_capture_reports) {
            isSelected = openApp(APPS_TRACKER_CAPTURE_REPORTS_PACKAGE);
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_profile) {
            attachFragmentDelayed(getProfileFragment());
            isSelected = true;
        } else if (menuItemId == org.hisp.dhis.client.sdk.ui.R.id.drawer_item_settings) {
            HolderActivity.navigateToSettingsFragment(this);
            isSelected = true;
        } else if (menuItemId == R.id.drawer_item_information) {
            attachFragment(getInformationFragment());
            isSelected = true;
        }
        /*else if (menuItemId == R.id.drawer_item_help) {
            attachFragment(getHelpFragment());
            isSelected = true;
        } else if (menuItemId == R.id.drawer_item_about) {
            attachFragment(getAboutFragment());
            isSelected = true;
        }*/

        isSelected = onItemSelected(menuItem) || isSelected;
        if (isSelected) {
            getNavigationView().setCheckedItem(menuItemId);
            getDrawerLayout().closeDrawers();
        }

        return isSelected;
    }

    protected Fragment getInformationFragment() {
        Bundle args = new Bundle();
        Session session = DhisController.getInstance().getSession();
        if (session != null && session.getCredentials() != null) {
            args.putString(InformationFragment.USERNAME, session.getCredentials().getUsername());
            args.putString(InformationFragment.URL, String.valueOf(session.getServerUrl()));
        }
        return WrapperFragment.newInstance(InformationFragment.class,
                getString(R.string.drawer_item_information),
                args);
    }
}
