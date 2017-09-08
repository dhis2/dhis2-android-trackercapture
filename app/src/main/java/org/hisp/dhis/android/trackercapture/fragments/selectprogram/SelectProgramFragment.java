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

package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.events.OnTeiDownloadedEvent;
import org.hisp.dhis.android.sdk.events.OnTrackerItemClick;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnTrackedEntityInstanceColumnClick;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.ItemStatusDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.QueryTrackedEntityInstancesDialogFragment;
import org.hisp.dhis.android.trackercapture.ui.DownloadEventSnackbar;
import org.hisp.dhis.android.trackercapture.ui.adapters.TrackedEntityInstanceAdapter;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class SelectProgramFragment extends org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragment
        implements SearchView.OnQueryTextListener, SearchView.OnFocusChangeListener, SearchView.OnCloseListener,
        MenuItemCompat.OnActionExpandListener, LoaderManager.LoaderCallbacks<SelectProgramFragmentForm>, IEnroller {
    public static final String TAG = SelectProgramFragment.class.getSimpleName();

    private FloatingActionButton mRegisterEventButton;
    private FloatingActionButton mQueryTrackedEntityInstancesButton;
    private FloatingActionButton mUpcomingEventsButton;
    private FloatingActionButton mLocalSearchButton;
    private SelectProgramFragmentForm mForm;
    protected TextView noRowsTextView;
    private DownloadEventSnackbar snackbar;
    private MenuItem item;

    @Override
    protected TrackedEntityInstanceAdapter getAdapter(Bundle savedInstanceState) {
        return new TrackedEntityInstanceAdapter(getLayoutInflater(savedInstanceState));
    }

    @Override
    protected View getListViewHeader(Bundle savedInstanceState) {

        if (getActivity() instanceof AppCompatActivity) {
            Toolbar toolbar = getParentToolbar();
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleNavigationDrawer();
                }
            });
        }

        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_select_program_header, mListView, false
        );
        mRegisterEventButton = (FloatingActionButton) header.findViewById(R.id.register_new_event);
        mQueryTrackedEntityInstancesButton = (FloatingActionButton) header.findViewById(R.id.query_trackedentityinstances_button);
        mUpcomingEventsButton = (FloatingActionButton) header.findViewById(R.id.upcoming_events_button);
        mLocalSearchButton = (FloatingActionButton) header.findViewById(R.id.local_search_button);
        noRowsTextView = (TextView) header.findViewById(R.id.textview_no_items);
        noRowsTextView.setText(getString(R.string.specify_search_criteria));

        mRegisterEventButton.setOnClickListener(this);
        mQueryTrackedEntityInstancesButton.setOnClickListener(this);
        mUpcomingEventsButton.setOnClickListener(this);
        mLocalSearchButton.setOnClickListener(this);

        mRegisterEventButton.hide();
        mUpcomingEventsButton.hide();
        mQueryTrackedEntityInstancesButton.hide();
        mLocalSearchButton.hide();
        noRowsTextView.setVisibility(View.GONE);
        return header;
    }

    @Override
    protected ProgramType[] getProgramTypes() {
        return new ProgramType[]{
                ProgramType.WITH_REGISTRATION
        };
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_select_program, menu);
        item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        MenuItemCompat.setOnActionExpandListener(item, this);
        searchView.setOnQueryTextListener(this);
        searchView.setOnQueryTextFocusChangeListener(this);
        searchView.setOnCloseListener(this);
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
            HolderActivity.navigateToProgramOverviewFragment(getActivity(), mState.getOrgUnitId(), mState.getProgramId(),
                    eventClick.getItem().getLocalId());
        } else {
            showStatusDialog(eventClick.getItem());
        }
    }

    @Subscribe
    public void onReceivedUiEvent(UiEvent uiEvent) {
        if (uiEvent.getEventType() == UiEvent.UiEventType.SYNCING_END) {
            getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
        }
        super.onReceivedUiEvent(uiEvent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.register_new_event: {
                createEnrollment();
                break;
            }
            case R.id.upcoming_events_button: {
                HolderActivity.navigateToUpcomingEventsFragment(getActivity());
                break;
            }
            case R.id.query_trackedentityinstances_button: {
                showOnlineSearchFragment(mState.getOrgUnitId(), mState.getProgramId());
                break;
            }
            case R.id.local_search_button: {
                HolderActivity.navigateToLocalSearchFragment(getActivity(), mState.getOrgUnitId(), mState.getProgramId());
                break;
            }
        }
    }

    private void createEnrollment() {
        if (mForm != null && mForm.getProgram() != null) {
            EnrollmentDateSetterHelper.createEnrollment(this, getActivity(), mForm.getProgram().
                            getDisplayIncidentDate(), mForm.getProgram().getSelectEnrollmentDatesInFuture(),
                    mForm.getProgram().getSelectIncidentDatesInFuture(), mForm.getProgram().getEnrollmentDateLabel(),
                    mForm.getProgram().getIncidentDateLabel());
        }
    }

    public void showEnrollmentFragment(TrackedEntityInstance trackedEntityInstance, DateTime enrollmentDate, DateTime incidentDate) {
        String enrollmentDateString = enrollmentDate.toString();
        String incidentDateString = null;
        if (incidentDate != null) {
            incidentDateString = incidentDate.toString();
        }
        if (trackedEntityInstance == null) {
            HolderActivity.navigateToEnrollmentDataEntryFragment(getActivity(), mState.getOrgUnitId(), mState.getProgramId(), enrollmentDateString, incidentDateString);

        } else {
            HolderActivity.navigateToEnrollmentDataEntryFragment(getActivity(), mState.getOrgUnitId(), mState.getProgramId(), trackedEntityInstance.getLocalId(), enrollmentDateString, incidentDateString);

        }

    }

    private static final void showQueryTrackedEntityInstancesDialog(FragmentManager fragmentManager, String orgUnit, String program) {
        QueryTrackedEntityInstancesDialogFragment dialog = QueryTrackedEntityInstancesDialogFragment.newInstance(program, orgUnit);
        dialog.show(fragmentManager);
    }

    public final void showOnlineSearchFragment() {
        showOnlineSearchFragment(mState.getOrgUnitId(), mState.getProgramId());
    }

    private final void showOnlineSearchFragment(String orgUnit, String program) {
        HolderActivity.navigateToOnlineSearchFragment(getActivity(), program, orgUnit, false, null);
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
                mLocalSearchButton.hide();
                break;
            case 1:
                mRegisterEventButton.show();
                mUpcomingEventsButton.show();
                mQueryTrackedEntityInstancesButton.show();
                mLocalSearchButton.show();
        }
    }

    @Override
    public void onLoadFinished(Loader<SelectProgramFragmentForm> loader, SelectProgramFragmentForm data) {
        if (LOADER_ID == loader.getId()) {
            mProgressBar.setVisibility(View.GONE);
            mForm = data;
            ((TrackedEntityInstanceAdapter) mAdapter).setData(data.getEventRowList());
            mAdapter.swapData(data.getEventRowList());

            if (data.getProgram() != null && data.getProgram().isDisplayFrontPageList()) {
                noRowsTextView.setVisibility(View.GONE);
                if (item != null)
                    item.setVisible(true);
            } else {
                noRowsTextView.setVisibility(View.VISIBLE);

                if (item != null)
                    item.setVisible(false);
            }
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, query);
        View view = getActivity().getCurrentFocus();
        //hide keyboard
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }


        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, newText);
        ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_SEARCH + newText);

        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view instanceof SearchView) {
            SearchView searchView = (SearchView) view;

            if (searchView.getQuery().length() == 0) {
                ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(""); //show all rows
            }
        }
    }

    private void setFocusSortColumn(int column) {
        TrackedEntityInstanceAdapter teiAdapter = (TrackedEntityInstanceAdapter) mAdapter;
        View view = mForm.getColumnNames().getView();

        switch (column) {
            //todo put UI stuff inside here when list is sorted either ascending or descending
            //first column
            case 1: {
                if (teiAdapter.getFilteredColumn() == column) {

                } else if (teiAdapter.isListIsReversed(column)) {

                }
                break;
            }
            // second column
            case 2: {
                break;
            }
            // third column
            case 3: {
                break;
            }
            // status column
            case 4: {
                break;
            }
        }
    }

    @Subscribe
    public void onItemClick(OnTrackedEntityInstanceColumnClick eventClick) {
        Log.d(TAG, "COLUMN CLICKED : " + eventClick.getColumnClicked());
        switch (eventClick.getColumnClicked()) {
            case OnTrackedEntityInstanceColumnClick.FIRST_COLUMN: {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_FIRST_COLUMN);
                ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_FIRST_COLUMN + "");
                setFocusSortColumn(1);
                break;

            }
            case OnTrackedEntityInstanceColumnClick.SECOND_COLUMN: {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_SECOND_COLUMN);
                ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_SECOND_COLUMN + "");
                break;

            }
            case OnTrackedEntityInstanceColumnClick.THIRD_COLUMN: {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_THIRD_COLUMN);
                ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_THIRD_COLUMN + "");
                break;

            }
            case OnTrackedEntityInstanceColumnClick.STATUS_COLUMN: {
                Log.d(TAG, "Filter column " + TrackedEntityInstanceAdapter.FILTER_STATUS);
                ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(TrackedEntityInstanceAdapter.FILTER_STATUS + "");
                break;
            }
        }
    }

    @Subscribe
    public void onTeiDownloaded(OnTeiDownloadedEvent event) {
        if (getActivity() == null || !isAdded()) {
            return;
        }

        if (snackbar == null) {
            snackbar = new DownloadEventSnackbar(this);
        }

        snackbar.show(event);

    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        Log.d(TAG, "onMenuItemActionExpand");
        return true; //return true to expand
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(""); //showing all rows
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        new MenuInflater(this.getActivity()).inflate(org.hisp.dhis.android.sdk.R.menu.menu_selected_trackedentityinstance, menu);

        // If item has been sent to server, set deletion menu item label as "Remove from device" instead of "delete"
        MenuItem menuItem = menu.findItem(R.id.action_delete);
        if (menuItem != null) {
            AdapterView.AdapterContextMenuInfo info =
                    (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
            final TrackedEntityInstanceItemRow itemRow = (TrackedEntityInstanceItemRow) mListView.getItemAtPosition(info.position);
            if (itemRow != null && itemRow.getStatus().equals(OnRowClick.ITEM_STATUS.SENT)) {
                menuItem.setTitle(R.string.remove);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        final TrackedEntityInstanceItemRow itemRow = (TrackedEntityInstanceItemRow) mListView.getItemAtPosition(info.position);

        Log.d(TAG, "" + itemRow.getTrackedEntityInstance().getTrackedEntityInstance());


        if (item.getItemId() == R.id.action_overview) {
            HolderActivity.navigateToProgramOverviewFragment(getActivity(),
                    mState.getOrgUnitId(), mState.getProgramId(), itemRow.getTrackedEntityInstance().getLocalId());
        } else if (item.getItemId() == R.id.action_delete) {
            // if not sent to server, show dialog with error icon
            if (!(itemRow.getStatus().equals(OnRowClick.ITEM_STATUS.SENT))) {
                UiUtils.showConfirmDialog(getActivity(), getActivity().getString(R.string.confirm),
                        getActivity().getString(R.string.warning_delete_unsent_tei),
                        getActivity().getString(R.string.delete), getActivity().getString(R.string.cancel),
                        (R.drawable.ic_event_error),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                performSoftDeleteOfTrackedEntityInstance(itemRow.getTrackedEntityInstance());
                                dialog.dismiss();
                            }
                        });
            } else {
                // if not sent to server, show dialog with warning icon
                UiUtils.showConfirmDialog(getActivity(), getActivity().getString(R.string.remove_report_entity_dialog_title),
                        getActivity().getString(R.string.remove_report_entity_dialog_message),
                        getActivity().getString(R.string.remove), getActivity().getString(R.string.cancel),
                        (R.drawable.ic_warning_black_24dp),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                performSoftDeleteOfTrackedEntityInstance(itemRow.getTrackedEntityInstance());
                                dialog.dismiss();
                            }
                        });
            }
        }
        return true;
    }

    public void performSoftDeleteOfTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        List<Enrollment> enrollments = TrackerController.getEnrollments(mState.getProgramId(), trackedEntityInstance);
        Enrollment activeEnrollment = null;
        for (Enrollment enrollment : enrollments) {
            if (Enrollment.ACTIVE.equals(enrollment.getStatus())) {
                activeEnrollment = enrollment;
            }
        }

        if (activeEnrollment != null) {
            List<Event> eventsForActiveEnrollment = TrackerController.getEventsByEnrollment(activeEnrollment.getLocalId());

            if (eventsForActiveEnrollment != null) {
                for (Event event : eventsForActiveEnrollment) {
                    event.delete();
                }
            }

            activeEnrollment.delete();
        }
    }

    @Override
    public boolean onClose() {
        ((TrackedEntityInstanceAdapter) mAdapter).getFilter().filter(""); //show all rows
        return false;
    }

    @Override
    public void stateChanged() {
        super.stateChanged();
    }

}