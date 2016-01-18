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
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragment;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.ProgramOverviewFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.UpcomingEventAdapter;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class UpcomingEventsFragment extends SelectProgramFragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<SelectProgramFragmentForm> {

    private static final String CLASS_TAG = "UpcomingEventsFragment";

    private List<OrganisationUnit> assignedOrganisationUnits;

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
    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    protected AbsAdapter getAdapter(Bundle savedInstanceState) {
        return new UpcomingEventAdapter(getLayoutInflater(savedInstanceState));
    }

    protected View getListViewHeader(Bundle savedInstanceState) {
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

        startDate = new DataValue();
        startDate.setValue(DateUtils.getMediumDateString());
        endDate = new DataValue();
        endDate.setValue(new LocalDate(DateUtils.getMediumDateString()).plusYears(1).toString());
        DatePickerRow startDatePicker = new DatePickerRow(getString(R.string.startdate), false, null, startDate, true);
        DatePickerRow endDatePicker = new DatePickerRow(getString(R.string.enddate), false, null, endDate, true);
        LinearLayout dateFilterContainer = (LinearLayout) header.findViewById(R.id.datefilterlayout);
        View view1 = startDatePicker.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, dateFilterContainer);
        view1.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        View view2 = endDatePicker.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, dateFilterContainer);
        view2.setLayoutParams(new TableLayout.LayoutParams(0, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
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
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new UpcomingEventsFragmentQuery(mState.getOrgUnitId(), mState.getProgramId(),
                    startDate.getValue(), endDate.getValue()));
        }
        return null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Event event = TrackerController.getEvent(id);
        ProgramOverviewFragment fragment = ProgramOverviewFragment.
                newInstance(mState.getOrgUnitId(), mState.getProgramId(),
                        TrackerController.getEnrollment
                                (event.getLocalEnrollmentId()).getLocalTrackedEntityInstanceId());

        mNavigationHandler.switchFragment(fragment, ProgramOverviewFragment.CLASS_TAG, true);
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
            case 0:
                mQueryButton.hide();
                break;
            case 1:
                mQueryButton.show();
        }
    }
}
