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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.events.OnTrackerItemClick;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnTrackedEntityInstanceColumnClick;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.enrollment.EnrollmentDataEntryFragment;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.ProgramOverviewFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.ItemStatusDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.QueryTrackedEntityInstancesDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.upcomingevents.UpcomingEventsFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.TrackedEntityInstanceAdapter;

import java.util.ArrayList;
import java.util.List;

public class SelectProgramFragment extends org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragment
        implements SearchView.OnQueryTextListener, SearchView.OnFocusChangeListener,
        MenuItemCompat.OnActionExpandListener, LoaderManager.LoaderCallbacks<SelectProgramFragmentForm>{
    public static final String TAG = SelectProgramFragment.class.getSimpleName();

    private FloatingActionButton mRegisterEventButton;
    private FloatingActionButton mQueryTrackedEntityInstancesButton;
    private FloatingActionButton mUpcomingEventsButton;
    private SelectProgramFragmentForm mForm;

    @Override
    protected TrackedEntityInstanceAdapter getAdapter(Bundle savedInstanceState) {
        return new TrackedEntityInstanceAdapter(getLayoutInflater(savedInstanceState));
    }

    @Override
    protected View getListViewHeader(Bundle savedInstanceState) {
        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_select_program_header, mListView, false
        );
        mRegisterEventButton = (FloatingActionButton) header.findViewById(R.id.register_new_event);
        mQueryTrackedEntityInstancesButton = (FloatingActionButton) header.findViewById(R.id.query_trackedentityinstances_button);
        mUpcomingEventsButton = (FloatingActionButton) header.findViewById(R.id.upcoming_events_button);
        mRegisterEventButton.setOnClickListener(this);
        mQueryTrackedEntityInstancesButton.setOnClickListener(this);
        mUpcomingEventsButton.setOnClickListener(this);

        mRegisterEventButton.hide();
        mUpcomingEventsButton.hide();
        mQueryTrackedEntityInstancesButton.hide();
        return header;
    }

    @Override
    protected Program.ProgramType[] getProgramTypes() {
        return new Program.ProgramType[] {
                Program.ProgramType.MULTIPLE_EVENTS_WITH_REGISTRATION,
                Program.ProgramType.SINGLE_EVENT_WITH_REGISTRATION,
                Program.ProgramType.WITH_REGISTRATION
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_select_program, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        MenuItemCompat.setOnActionExpandListener(item, this);
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
    }

    @Override
    public Loader<SelectProgramFragmentForm> onCreateLoader(int id, Bundle args) {
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

    @Subscribe
    public void onItemClick(OnTrackerItemClick eventClick) {
        if (eventClick.isOnDescriptionClick()) {

            ProgramOverviewFragment fragment = ProgramOverviewFragment.
                    newInstance(mState.getOrgUnitId(), mState.getProgramId(),
                            eventClick.getItem().getLocalId());

            mNavigationHandler.switchFragment(fragment, ProgramOverviewFragment.CLASS_TAG, true);
        } else {
            showStatusDialog(eventClick.getItem());
        }
    }

    @Subscribe
    public void onReceivedUiEvent(UiEvent uiEvent) {
        super.onReceivedUiEvent(uiEvent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register_new_event: {
                EnrollmentDataEntryFragment enrollmentDataEntryFragment = EnrollmentDataEntryFragment.newInstance(mState.getOrgUnitId(), mState.getProgramId());
                mNavigationHandler.switchFragment(enrollmentDataEntryFragment, EnrollmentDataEntryFragment.class.getName(), true);

                break;
            }
            case R.id.upcoming_events_button: {
                UpcomingEventsFragment fragment = new UpcomingEventsFragment();
                mNavigationHandler.switchFragment(fragment, UpcomingEventsFragment.class.getName(), true);
                break;
            }
            case R.id.query_trackedentityinstances_button: {
                showQueryTrackedEntityInstancesDialog(getChildFragmentManager(), mState.getOrgUnitId(), mState.getProgramId());
                break;
            }
        }
    }

    private static final void showQueryTrackedEntityInstancesDialog(FragmentManager fragmentManager, String orgUnit, String program) {
        QueryTrackedEntityInstancesDialogFragment dialog = QueryTrackedEntityInstancesDialogFragment.newInstance(program, orgUnit);
        dialog.show(fragmentManager);
    }
    public void showStatusDialog(BaseSerializableModel model) {

        ItemStatusDialogFragment fragment = ItemStatusDialogFragment.newInstance(model);
        fragment.show(getChildFragmentManager());
    }

    protected void handleViews(int level) {
        mAdapter.swapData(null);
        switch (level) {
            case 0:
                mRegisterEventButton.hide();
                mUpcomingEventsButton.hide();
                mQueryTrackedEntityInstancesButton.hide();
                break;
            case 1:
                mRegisterEventButton.show();
                mUpcomingEventsButton.show();
                mQueryTrackedEntityInstancesButton.show();
        }
    }

    @Override
    public void onLoadFinished(Loader<SelectProgramFragmentForm> loader, SelectProgramFragmentForm data) {
        if (LOADER_ID == loader.getId()) {
            mProgressBar.setVisibility(View.GONE);
            mForm = data;
            ( ( TrackedEntityInstanceAdapter ) mAdapter).setData(data.getEventRowList());
            mAdapter.swapData(data.getEventRowList());
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
        ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_SEARCH + newText);
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus)
    {
        if(view instanceof SearchView)
        {
            if(!hasFocus)
            {
                ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(""); //show all rows
            }
        }
    }

    private void setFocusSortColumn(int column)
    {
        TrackedEntityInstanceAdapter teiAdapter = (TrackedEntityInstanceAdapter) mAdapter;
        View view = mForm.getColumnNames().getView();


        switch (column) {
            //todo put UI stuff inside here when list is sorted either ascending or descending
            case 1: //first column
            {

                if(teiAdapter.getFilteredColumn() == column)
                {


                }
                else if(teiAdapter.isListIsReversed(column))
                {

                }

                break;
            }
            case 2: // second column
            {

                break;
            }
            case 3: // third column
            {

                break;
            }
            case 4: // status column
            {

                break;
            }
        }
    }

    @Subscribe
    public void onItemClick(OnTrackedEntityInstanceColumnClick eventClick)
    {
        Log.d(TAG, "COLUMN CLICKED : " + eventClick.getColumnClicked());
        switch (eventClick.getColumnClicked())
        {
            case OnTrackedEntityInstanceColumnClick.FIRST_COLUMN:
            {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_FIRST_COLUMN);
                ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_FIRST_COLUMN + "");
                setFocusSortColumn(1);
                break;

            }
            case OnTrackedEntityInstanceColumnClick.SECOND_COLUMN:
            {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_SECOND_COLUMN);
                ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_SECOND_COLUMN + "");
                break;

            }
            case OnTrackedEntityInstanceColumnClick.THIRD_COLUMN:
            {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_THIRD_COLUMN);
                ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_THIRD_COLUMN + "");
                break;

            }
            case OnTrackedEntityInstanceColumnClick.STATUS_COLUMN:
            {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_STATUS);
                ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_STATUS + "");
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
        ( ( TrackedEntityInstanceAdapter ) mAdapter ).getFilter().filter(""); //showing all rows
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        new MenuInflater(this.getActivity()).inflate(org.hisp.dhis.android.sdk.R.menu.menu_selected_trackedentityinstance, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info=
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        final TrackedEntityInstanceItemRow itemRow = (TrackedEntityInstanceItemRow) mListView.getItemAtPosition(info.position);

        Log.d(TAG, "" + itemRow.getTrackedEntityInstance().getTrackedEntityInstance());


        if(item.getTitle().toString().equals(getResources().getString(org.hisp.dhis.android.sdk.R.string.go_to_programoverview_fragment)))
        {
            mNavigationHandler.switchFragment(
                    ProgramOverviewFragment.newInstance(
                            mState.getOrgUnitId(), mState.getProgramId(), itemRow.getTrackedEntityInstance().getLocalId()),
                    TAG, true);
        }
        else if(item.getTitle().toString().equals(getResources().getString(org.hisp.dhis.android.sdk.R.string.delete)))
        {
            if( !(itemRow.getStatus().equals(OnRowClick.ITEM_STATUS.SENT))) // if not sent to server, present dialog to user
            {
                UiUtils.showConfirmDialog(getActivity(), getActivity().getString(R.string.confirm),
                        getActivity().getString(R.string.warning_delete_unsent_tei),
                        getActivity().getString(R.string.delete), getActivity().getString(R.string.cancel),
                        (R.drawable.ic_event_error),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                itemRow.getTrackedEntityInstance().delete();
                                dialog.dismiss();
                            }
                        });
            }
            else
            {
                //if sent to server, be able to soft delete without annoying the user
                itemRow.getTrackedEntityInstance().delete();
            }
        }
        return true;
    }
}