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

package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TableLayout;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.fragments.SettingsFragment;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.sdk.utils.Utils;
import org.hisp.dhis2.android.sdk.utils.ui.rows.DatePickerRow;
import org.hisp.dhis2.android.sdk.utils.ui.views.CardTextViewButton;
import org.hisp.dhis2.android.sdk.utils.ui.views.FloatingActionButton;
import org.hisp.dhis2.android.trackercapture.INavigationHandler;
import org.hisp.dhis2.android.trackercapture.R;
import org.hisp.dhis2.android.trackercapture.fragments.ProgramOverviewFragment;
import org.hisp.dhis2.android.trackercapture.fragments.loaders.DbLoader;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.adapters.EventAdapter;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.UpcomingEventRow;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class UpcomingEventsFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        OrgUnitDialogFragment.OnOrgUnitSetListener,
        ProgramDialogFragment.OnProgramSetListener,
        LoaderManager.LoaderCallbacks<List<UpcomingEventRow>>{

    private static final String CLASS_TAG = "UpcomingEventsFragment";
    private static final String STATE = "state:UpcomingEventsFragment";
    private static final int LOADER_ID = 2;

    private List<OrganisationUnit> assignedOrganisationUnits;

    private CardTextViewButton mOrgUnitButton;
    private CardTextViewButton mProgramButton;
    private FloatingActionButton mQueryButton;

    private ListView mListView;
    private ProgressBar mProgressBar;
    private EventAdapter mAdapter;

    private DataValue startDate;
    private DataValue endDate;

    private UpcomingEventsFragmentState mState;

    private INavigationHandler mNavigationHandler;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof INavigationHandler) {
            mNavigationHandler = (INavigationHandler) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement INavigationHandler interface");
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upcomingevents2, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        mListView = (ListView) view.findViewById(R.id.event_listview);
        mAdapter = new EventAdapter(getLayoutInflater(savedInstanceState));
        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_upcomingevents_header, mListView, false
        );
        mProgressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mListView.addHeaderView(header, CLASS_TAG, false);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mOrgUnitButton = (CardTextViewButton) header.findViewById(R.id.select_organisation_unit);
        mProgramButton = (CardTextViewButton) header.findViewById(R.id.select_program);

        mQueryButton = (FloatingActionButton) header.findViewById(R.id.upcoming_query_button);
        assignedOrganisationUnits = Dhis2.getInstance().
                getMetaDataController().getAssignedOrganisationUnits();
        if( assignedOrganisationUnits==null || assignedOrganisationUnits.size() <= 0 ) {
            return;
        }

        mOrgUnitButton.setOnClickListener(this);
        mProgramButton.setOnClickListener(this);
        mQueryButton.setOnClickListener(this);

        mOrgUnitButton.setEnabled(true);
        mProgramButton.setEnabled(false);
        mQueryButton.hide();

        startDate = new DataValue();
        startDate.value = Utils.getCurrentDate();
        endDate = new DataValue();
        endDate.value = new LocalDate(Utils.getCurrentDate()).plusYears(1).toString();
        DatePickerRow startDatePicker = new DatePickerRow(getActivity().getLayoutInflater(), getString(R.string.startdate), getActivity(), startDate);
        DatePickerRow endDatePicker = new DatePickerRow(getActivity().getLayoutInflater(), getString(R.string.enddate), getActivity(), endDate);
        LinearLayout dateFilterContainer = (LinearLayout) header.findViewById(R.id.datefilterlayout);
        View view1 = startDatePicker.getView(null);
        view1.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        View view2 = endDatePicker.getView(null);
        view2.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        dateFilterContainer.addView(view1);
        dateFilterContainer.addView(view2);

        if (savedInstanceState != null &&
                savedInstanceState.getParcelable(STATE) != null) {
            mState = savedInstanceState.getParcelable(STATE);
        }

        if (mState == null) {
            mState = new UpcomingEventsFragmentState();
        }

        onRestoreState(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_upcoming_events, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            mNavigationHandler.switchFragment(
                    new SettingsFragment(), SettingsFragment.TAG);
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

        UpcomingEventsFragmentState backedUpState = new UpcomingEventsFragmentState(mState);
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

    @Override
    public void onUnitSelected(String orgUnitId, String orgUnitLabel) {
        mOrgUnitButton.setText(orgUnitLabel);
        mProgramButton.setEnabled(true);

        mState.setOrgUnit(orgUnitId, orgUnitLabel);
        mState.resetProgram();
        handleViews(0);
    }

    @Override
    public void onProgramSelected(String programId, String programName) {
        mProgramButton.setText(programName);

        mState.setProgram(programId, programName);
        handleViews(1);
    }

    @Override
    public Loader<List<UpcomingEventRow>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            // modelsToTrack.add(Event.class);
            // modelsToTrack.add(FailedItem.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new UpcomingEventsFragmentQuery(mState.getOrgUnitId(), mState.getProgramId(),
                    startDate.value, endDate.value));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<UpcomingEventRow>> loader, List<UpcomingEventRow> data) {
        if (LOADER_ID == loader.getId()) {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<UpcomingEventRow>> loader) {
        mAdapter.swapData(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ProgramOverviewFragment fragment2 = new ProgramOverviewFragment();
        fragment2.setSelectedProgram(MetaDataController.getProgram(mState.getProgramId()));
        fragment2.setSelectedOrganisationUnit(MetaDataController.getOrganisationUnit(mState.getOrgUnitId()));
        Event event = DataValueController.getEvent(id);
        TrackedEntityInstance tei = DataValueController.getTrackedEntityInstance(event.trackedEntityInstance);
        fragment2.setSelectedTrackedEntityInstance(tei);
        mNavigationHandler.switchFragment(fragment2, ProgramOverviewFragment.CLASS_TAG);
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
            case R.id.upcoming_query_button: {
                if(startDate.value==null || startDate.value.isEmpty()
                        || endDate.value==null || endDate.value.isEmpty())
                    break;
                mProgressBar.setVisibility(View.VISIBLE);
                // this call will trigger onCreateLoader method
                getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
                break;
            }
        }
    }

    private void handleViews(int level) {
        mAdapter.swapData(null);
        switch (level) {
            case 0:
                mQueryButton.hide();
                break;
            case 1:
                mQueryButton.show();
        }
    }


}
