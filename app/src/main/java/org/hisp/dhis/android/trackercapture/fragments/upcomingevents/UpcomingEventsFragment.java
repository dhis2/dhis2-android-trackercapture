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

package org.hisp.dhis.android.trackercapture.fragments.upcomingevents;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.ui.adapters.AbsAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis.android.sdk.ui.dialogs.OrgUnitDialogFragment;
import org.hisp.dhis.android.sdk.ui.dialogs.ProgramDialogFragment;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragment;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentState;
import org.hisp.dhis.android.sdk.ui.views.CardTextViewButton;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.ui.adapters.UpcomingEventAdapter;
import org.hisp.dhis.android.sdk.ui.dialogs.UpcomingEventsDialogFilter;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class UpcomingEventsFragment extends SelectProgramFragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<SelectProgramFragmentForm> {

    private static final String CLASS_TAG = "UpcomingEventsFragment";

    private List<OrganisationUnit> assignedOrganisationUnits;
    protected CardTextViewButton filterButton;
    private FloatingActionButton mQueryButton;

    private DataValue startDate;
    private DataValue endDate;

    public UpcomingEventsFragment(){
        super("state:UpcomingEventsFragment", 2);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            getActivity().finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    protected AbsAdapter getAdapter(Bundle savedInstanceState) {
        return new UpcomingEventAdapter(getLayoutInflater(savedInstanceState));
    }

    protected View getListViewHeader(Bundle savedInstanceState) {
        if(getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setTitle(getString(R.string.upcoming_events));
        }

        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_upcomingevents_header, mListView, false
        );

        mListView.setOnItemClickListener(this);
        mQueryButton = (FloatingActionButton) header.findViewById(R.id.upcoming_query_button);
        assignedOrganisationUnits = MetaDataController.getAssignedOrganisationUnits();
        if( assignedOrganisationUnits==null || assignedOrganisationUnits.size() <= 0 ) {
            return header;
        }
        mQueryButton.setOnClickListener(this);
        mQueryButton.hide();

        filterButton = (CardTextViewButton) header.findViewById(R.id.select_filter);
        filterButton.setText(mPrefs.getFilter().second);

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpcomingEventsDialogFilter upcomingEventsDialogFilter = UpcomingEventsDialogFilter.newInstance(UpcomingEventsFragment.this);
                upcomingEventsDialogFilter.show(getChildFragmentManager());
            }
        });

        startDate = new DataValue();
        startDate.setValue(DateUtils.getMediumDateString());
        endDate = new DataValue();
        endDate.setValue(new LocalDate(DateUtils.getMediumDateString()).plusWeeks(1).toString());
        DatePickerRow startDatePicker = new DatePickerRow(getString(R.string.startdate), false, null, startDate, true);
        startDatePicker.setHideDetailedInfoButton(true);
        DatePickerRow endDatePicker = new DatePickerRow(getString(R.string.enddate), false, null, endDate, true);
        endDatePicker.setHideDetailedInfoButton(true);
        LinearLayout dateFilterContainer = (LinearLayout) header.findViewById(R.id.datefilterlayout);
        View view1 = startDatePicker.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, dateFilterContainer);
        view1.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        View view2 = endDatePicker.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, dateFilterContainer);
        view2.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        View detailedInfoButton1 = view1.findViewById(R.id.detailed_info_button_layout);
        View detailedInfoButton2 = view2.findViewById(R.id.detailed_info_button_layout);
        detailedInfoButton1.setVisibility(View.GONE);
        detailedInfoButton2.setVisibility(View.GONE);
        dateFilterContainer.addView(view1);
        dateFilterContainer.addView(view2);
        return header;
    }

    @Override
    protected ProgramType[] getProgramTypes() {
        return new ProgramType[] {
                ProgramType.WITH_REGISTRATION
        };
    }


    @Override
    public Loader<SelectProgramFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(Event.class);
            if(startDate != null && endDate != null) {
                return new DbLoader<>(
                        getActivity().getBaseContext(), modelsToTrack,
                        new UpcomingEventsFragmentQuery(mState.getOrgUnitId(), mState.getProgramId(),
                                mState.getFilterLabel(), startDate.getValue(), endDate.getValue()));
            }
        }
        return null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Event event = TrackerController.getEvent(id);


        HolderActivity.navigateToProgramOverviewFragment(getActivity(),mState.getOrgUnitId(), mState.getProgramId(),
                TrackerController.getEnrollment
                        (event.getLocalEnrollmentId()).getLocalTrackedEntityInstanceId());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.upcoming_query_button: {
                if(startDate.getValue()==null || startDate.getValue().isEmpty()
                        || endDate.getValue()==null || endDate.getValue().isEmpty())
                    break;
                mProgressBar.setVisibility(View.VISIBLE);
                getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
                break;
            }
        }
    }

    protected void handleViews(int level) {
        mAdapter.swapData(null);
        switch (level) {
            case 0: {
                mQueryButton.hide();
                break;
            }
            case 1: {
                if(mPrefs.getFilter() == null) {
                    mQueryButton.hide();
            }
                else {
                    mQueryButton.show();
                }
                break;
            }
            case 2: {
                mQueryButton.show();
                break;
            }
        }
    }

    private ActionBar getActionBar() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            throw new IllegalArgumentException("Fragment should be attached to ActionBarActivity");
        }
    }

    @Override
    public void stateChanged() {
        // stub
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
            case UpcomingEventsDialogFilter.ID : {
                onFilterSelected(id,name);
            }
        }
    }

    private void onFilterSelected(String id, String name) {
        filterButton.setText(name);
        mState.setFilter(id, name);
        mPrefs.putFilter(new Pair<>(id, name));

        handleViews(2);
    }


}
