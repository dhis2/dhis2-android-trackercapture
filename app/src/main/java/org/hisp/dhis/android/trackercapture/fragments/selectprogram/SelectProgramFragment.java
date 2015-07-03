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

package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.internal.widget.TintImageView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.Dhis2;
import org.hisp.dhis.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis.android.sdk.events.InvalidateEvent;
import org.hisp.dhis.android.sdk.fragments.SettingsFragment;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.utils.ui.dialogs.AutoCompleteDialogFragment;
import org.hisp.dhis.android.sdk.utils.ui.views.CardTextViewButton;
import org.hisp.dhis.android.sdk.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.utils.ui.views.FloatingActionButton;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.enrollment.EnrollmentFragment;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.ProgramOverviewFragment;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceRow;
import org.hisp.dhis.android.trackercapture.fragments.upcomingevents.OrgUnitDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.upcomingevents.ProgramDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.upcomingevents.UpcomingEventsFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.TrackedEntityInstanceAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.hisp.dhis.android.sdk.utils.ui.dialogs.AutoCompleteDialogFragment.OnOptionSelectedListener;

public class SelectProgramFragment extends Fragment
        implements View.OnClickListener, OnOptionSelectedListener,
        LoaderManager.LoaderCallbacks<List<TrackedEntityInstanceRow>>, SearchView.OnQueryTextListener, SearchView.OnFocusChangeListener,
        MenuItemCompat.OnActionExpandListener{
    public static final String TAG = SelectProgramFragment.class.getSimpleName();
    private static final String STATE = "state:SelectProgramFragment";
    private static final int LOADER_ID = 1;

    private ListView mListView;
    private ProgressBar mProgressBar;
//    private EventAdapter mAdapter;
    private TrackedEntityInstanceAdapter mAdapter;

    private CardTextViewButton mOrgUnitButton;
    private CardTextViewButton mProgramButton;
    private FloatingActionButton mRegisterEventButton;
    private FloatingActionButton mUpcomingEventsButton;

    private SelectProgramFragmentState mState;
    private SelectProgramFragmentPreferences mPrefs;

    private INavigationHandler mNavigationHandler;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof INavigationHandler) {
            mNavigationHandler = (INavigationHandler) activity;
        } else {
            throw new IllegalArgumentException("Activity must " +
                    "implement INavigationHandler interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // we need to nullify reference
        // to parent activity in order not to leak it
        mNavigationHandler = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_program, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPrefs = new SelectProgramFragmentPreferences(
                getActivity().getApplicationContext());

        mListView = (ListView) view.findViewById(R.id.event_listview);
        mAdapter = new TrackedEntityInstanceAdapter(getLayoutInflater(savedInstanceState));
        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_select_program_header, mListView, false
        );
        mProgressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mListView.addHeaderView(header, TAG, false);
        mListView.setAdapter(mAdapter);

        mOrgUnitButton = (CardTextViewButton) header.findViewById(R.id.select_organisation_unit);
        mProgramButton = (CardTextViewButton) header.findViewById(R.id.select_program);
        mRegisterEventButton = (FloatingActionButton) header.findViewById(R.id.register_new_event);
        mUpcomingEventsButton = (FloatingActionButton) header.findViewById(R.id.upcoming_events_button);
        mOrgUnitButton.setOnClickListener(this);
        mProgramButton.setOnClickListener(this);
        mRegisterEventButton.setOnClickListener(this);
        mUpcomingEventsButton.setOnClickListener(this);

        mOrgUnitButton.setEnabled(true);
        mProgramButton.setEnabled(false);
        mRegisterEventButton.hide();
        mUpcomingEventsButton.hide();

        if (savedInstanceState != null &&
                savedInstanceState.getParcelable(STATE) != null) {
            mState = savedInstanceState.getParcelable(STATE);
        }

        if (mState == null) {
            // restoring last selection of program
            Pair<String, String> orgUnit = mPrefs.getOrgUnit();
            Pair<String, String> program = mPrefs.getProgram();
            mState = new SelectProgramFragmentState();
            if (orgUnit != null) {
                mState.setOrgUnit(orgUnit.first, orgUnit.second);
                if (program != null) {
                    mState.setProgram(program.first, program.second);
                }

            }
        }

        onRestoreState(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.getEventBus().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.getEventBus().register(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_select_program, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        MenuItemCompat.setOnActionExpandListener(item,this);
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (item.getItemId()){
            case R.id.action_settings :{
                mNavigationHandler.switchFragment(
                        new SettingsFragment(), SettingsFragment.TAG, true);
                break;
            }

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putParcelable(STATE, mState);
        super.onSaveInstanceState(out);
    }

    public void onRestoreState(boolean hasUnits) {
        mOrgUnitButton.setEnabled(hasUnits);
        if (!hasUnits) {
            return;
        }

        SelectProgramFragmentState backedUpState = new SelectProgramFragmentState(mState);
        if (!backedUpState.isOrgUnitEmpty()) {
            onUnitSelected(
                    backedUpState.getOrgUnitId(),
                    backedUpState.getOrgUnitLabel()
            );

            if (!backedUpState.isProgramEmpty()) {
                onProgramSelected(
                        backedUpState.getProgramId(),
                        backedUpState.getProgramName()
                );
            }
        }
    }

    public void onUnitSelected(String orgUnitId, String orgUnitLabel) {
        mOrgUnitButton.setText(orgUnitLabel);
        mProgramButton.setEnabled(true);

        mState.setOrgUnit(orgUnitId, orgUnitLabel);
        mState.resetProgram();

        mPrefs.putOrgUnit(new Pair<>(orgUnitId, orgUnitLabel));
        mPrefs.putProgram(null);

        handleViews(0);
    }


    public void onProgramSelected(String programId, String programName) {
        mProgramButton.setText(programName);

        mState.setProgram(programId, programName);
        mPrefs.putProgram(new Pair<>(programId, programName));
        handleViews(1);

        mProgressBar.setVisibility(View.VISIBLE);
        // this call will trigger onCreateLoader method
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public Loader<List<TrackedEntityInstanceRow>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(TrackedEntityInstance.class);
            modelsToTrack.add(Enrollment.class);
            modelsToTrack.add(Event.class);
            modelsToTrack.add(FailedItem.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new SelectProgramFragmentQuery(mState.getOrgUnitId(), mState.getProgramId()));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<TrackedEntityInstanceRow>> loader, List<TrackedEntityInstanceRow> data) {
        if (LOADER_ID == loader.getId()) {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.setData(data);
            mAdapter.swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<TrackedEntityInstanceRow>> loader) {
        mAdapter.swapData(null);
    }

    @Subscribe
    public void onInvalidate(InvalidateEvent event) {
        if(event.eventType == InvalidateEvent.EventType.dataValuesSent ||
                event.eventType == InvalidateEvent.EventType.dataValuesLoaded ||
                event.eventType == InvalidateEvent.EventType.metaDataLoaded) {
            getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
        }
    }

    @Subscribe
    public void onItemClick(OnTrackedEntityInstanceClick eventClick) {
        if (eventClick.isOnDescriptionClick()) {

            ProgramOverviewFragment fragment = ProgramOverviewFragment.
                    newInstance(mState.getOrgUnitId(), mState.getProgramId(),
                            eventClick.getTrackedEntityInstance().localId);

            mNavigationHandler.switchFragment(fragment, ProgramOverviewFragment.CLASS_TAG, true);
        } else {
            switch (eventClick.getStatus()) {
                case SENT:
                    Dhis2.getInstance().showErrorDialog(getActivity(),
                            getString(R.string.event_sent),
                            getString(R.string.status_sent_description),
                            R.drawable.ic_from_server
                    );
                    break;
                case OFFLINE:
                    Dhis2.getInstance().showErrorDialog(getActivity(),
                            getString(R.string.event_offline),
                            getString(R.string.status_offline_description),
                            R.drawable.ic_offline
                    );
                    break;
                case ERROR: {
                    String message = getErrorDescription(eventClick.getTrackedEntityInstance());
                    Dhis2.getInstance().showErrorDialog(getActivity(),
                            getString(R.string.event_error),
                            message, R.drawable.ic_event_error
                    );
                    break;
                }
            }
        }
    }

    @Subscribe
    public void onItemClick(OnTrackedEntityColumnClick eventClick)
    {
        Log.d(TAG, "COLUMN CLICKED : " + eventClick.getColumnClicked());
        switch (eventClick.getColumnClicked())
        {
            case OnTrackedEntityColumnClick.FIRST_COLUMN:
            {

            }
            case OnTrackedEntityColumnClick.SECOND_COLUMN:
            {

            }
            case OnTrackedEntityColumnClick.THIRD_COLUMN:
            {

            }
            case OnTrackedEntityColumnClick.STATUS_COLUMN:
            {
                mAdapter.getFilter().filter(TrackedEntityInstanceAdapter.FILTER_STATUS + "");
            }
        }
    }

    private String getErrorDescription(TrackedEntityInstance trackedEntityInstance) {
        FailedItem failedItem = DataValueController.getFailedItem(FailedItem.TRACKEDENTITYINSTANCE, trackedEntityInstance.localId);
                //Select.byId(FailedItem.class, trackedEntityInstance.localId);

        if (failedItem != null) {
            if (failedItem.getHttpStatusCode() == 200) {
                if(failedItem.getImportSummary()!=null)
                    return failedItem.getImportSummary().getDescription();
            }
            if (failedItem.getHttpStatusCode()== 401) {
                return getString(R.string.error_401_description);
            }

            if (failedItem.getHttpStatusCode()== 408) {
                return getString(R.string.error_408_description);
            }

            if (failedItem.getHttpStatusCode()>= 400 && failedItem.getHttpStatusCode() < 500) {
                return getString(R.string.error_series_400_description);
            }

            if (failedItem.getHttpStatusCode()>= 500) {
                return failedItem.getErrorMessage();
            }
        }

        return getString(R.string.unknown_error);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_organisation_unit: {
                OrgUnitDialogFragment fragment = OrgUnitDialogFragment
                        .newInstance(this);
                fragment.show(getChildFragmentManager());
                break;
            }
            case R.id.select_program: {
                ProgramDialogFragment fragment = ProgramDialogFragment
                        .newInstance(this, mState.getOrgUnitId(),
                                Program.MULTIPLE_EVENTS_WITH_REGISTRATION,
                                Program.SINGLE_EVENT_WITH_REGISTRATION);
                fragment.show(getChildFragmentManager());
                break;
            }
            case R.id.register_new_event: {
                EnrollmentFragment enrollmentFragment = EnrollmentFragment.newInstance(mState.getOrgUnitId(),mState.getProgramId());
                mNavigationHandler.switchFragment(enrollmentFragment, EnrollmentFragment.class.getName(), true);

                break;
            }
            case R.id.upcoming_events_button: {
                UpcomingEventsFragment fragment = new UpcomingEventsFragment();
                mNavigationHandler.switchFragment(fragment, UpcomingEventsFragment.class.getName(), true);
            }
        }
    }

    private void handleViews(int level) {
        mAdapter.swapData(null);
        switch (level) {
            case 0:
                mRegisterEventButton.hide();
                mUpcomingEventsButton.hide();
                break;
            case 1:
                mRegisterEventButton.show();
                mUpcomingEventsButton.show();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, query);
        View view = getActivity().getCurrentFocus();
        if(view != null) //hide keyboard
        {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText)
    {
        Log.d(TAG, newText);
        mAdapter.getFilter().filter(TrackedEntityInstanceAdapter.FILTER_SEARCH + newText);
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
        if(view instanceof SearchView)
        {
            if(!hasFocus)
            {
                mAdapter.getFilter().filter(""); //show all rows

            }
        }
    }

    @Override
    public void onOptionSelected(int dialogId, int position, String id, String name) {
        switch (dialogId) {
            case OrgUnitDialogFragment.ID: {
                onUnitSelected(id, name);
                break;
            }
            case ProgramDialogFragment.ID: {
                onProgramSelected(id, name);
                break;
            }
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true; //return true to expand
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        //
        mAdapter.getFilter().filter(""); //showing all rows
        return true;
    }
}