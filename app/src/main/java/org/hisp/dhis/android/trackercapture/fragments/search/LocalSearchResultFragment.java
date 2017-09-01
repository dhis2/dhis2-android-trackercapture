package org.hisp.dhis.android.trackercapture.fragments.search;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.events.OnTrackerItemClick;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.ProgramOverviewFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.ItemStatusDialogFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.TrackedEntityInstanceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocalSearchResultFragment extends Fragment implements LoaderManager.LoaderCallbacks<LocalSearchResultFragmentForm>,
                                                                    View.OnClickListener{
    public static final String EXTRA_PROGRAM = "extra:ProgramId";
    public static final String EXTRA_ORGUNIT = "extra:OrgUnitId";
    public static final String EXTRA_ATTRIBUTEVALUEMAP = "extra:AttributeValueMap";
    private String orgUnitId;
    private String programId;
    private HashMap<String,String> attributeValueMap;
    private ListView searchResultsListView;
    private TrackedEntityInstanceAdapter mAdapter;
    private final int LOADER_ID = 1112222111;
    private LocalSearchResultFragmentForm mForm;
    private ProgressBar progressBar;
    private CardView cardView;

    public static LocalSearchResultFragment newInstance(String orgUnitId, String programId, HashMap<String,String> attributeValueMap) {
        LocalSearchResultFragment fragment = new LocalSearchResultFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ORGUNIT, orgUnitId);
        args.putString(EXTRA_PROGRAM, programId);
        args.putSerializable(EXTRA_ATTRIBUTEVALUEMAP, attributeValueMap);
        fragment.setArguments(args);

        Log.d("HashMap size", attributeValueMap.size() + "");
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        orgUnitId = args.getString(EXTRA_ORGUNIT);
        programId = args.getString(EXTRA_PROGRAM);
        attributeValueMap = (HashMap) args.getSerializable(EXTRA_ATTRIBUTEVALUEMAP);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_search_results,container,false);
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        searchResultsListView = (ListView) view.findViewById(R.id.listview_search_results);
        progressBar = (ProgressBar) view.findViewById(R.id.local_search_progressbar);
        cardView = (CardView) view.findViewById(R.id.search_online_cardview);

        mAdapter = new TrackedEntityInstanceAdapter(getLayoutInflater(savedInstanceState));
        searchResultsListView.setAdapter(mAdapter);
        progressBar.setVisibility(View.VISIBLE);
        cardView.setVisibility(View.GONE);
        cardView.setOnClickListener(this);
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
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info=
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        final TrackedEntityInstanceItemRow itemRow = (TrackedEntityInstanceItemRow) searchResultsListView.getItemAtPosition(info.position);

        if(item.getTitle().toString().equals(getResources().getString(org.hisp.dhis.android.sdk.R.string.go_to_programoverview_fragment))) {
            HolderActivity.navigateToProgramOverviewFragment(getActivity(),
                            orgUnitId, programId, itemRow.getTrackedEntityInstance().getLocalId());
        } else if(item.getTitle().toString().equals(getResources().getString(org.hisp.dhis.android.sdk.R.string.delete))) {
            // if not sent to server, present dialog to user
            if( !(itemRow.getStatus().equals(OnRowClick.ITEM_STATUS.SENT))) {
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
                //if sent to server, be able to soft delete without annoying the user
                performSoftDeleteOfTrackedEntityInstance(itemRow.getTrackedEntityInstance());
            }
        }
        return true;
    }

        public void performSoftDeleteOfTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        List<Enrollment> enrollments = TrackerController.getEnrollments(programId, trackedEntityInstance);
        Enrollment activeEnrollment = null;
        for(Enrollment enrollment : enrollments) {
            if(Enrollment.ACTIVE.equals(enrollment.getStatus())) {
                activeEnrollment = enrollment;
            }
        }

        if(activeEnrollment != null) {
            List<Event> eventsForActiveEnrollment = TrackerController.getEventsByEnrollment(activeEnrollment.getLocalId());

            if(eventsForActiveEnrollment != null) {
                for(Event event : eventsForActiveEnrollment) {
                    event.delete();
                }
            }

            activeEnrollment.delete();
        }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORGUNIT, orgUnitId);
        bundle.putString(EXTRA_PROGRAM, programId);
        bundle.putSerializable(EXTRA_ATTRIBUTEVALUEMAP, attributeValueMap);
        getLoaderManager().initLoader(LOADER_ID, bundle, this);
    }


    @Override
    public Loader<LocalSearchResultFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            String orgUnitId = args.getString(EXTRA_ORGUNIT);
            String programId = args.getString(EXTRA_PROGRAM);
            HashMap<String,String> attributeValueMap = (HashMap) args.getSerializable(EXTRA_ATTRIBUTEVALUEMAP);

            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(TrackedEntityInstance.class);
            modelsToTrack.add(Enrollment.class);
            modelsToTrack.add(Event.class);
            modelsToTrack.add(FailedItem.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new LocalSearchResultFragmentFormQuery(orgUnitId, programId,attributeValueMap));
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<LocalSearchResultFragmentForm> loader, LocalSearchResultFragmentForm data) {
        if (LOADER_ID == loader.getId()) {
            progressBar.setVisibility(View.GONE);
            mForm = data;
            mAdapter.swapData(data.getEventRowList());
        }
    }

    @Subscribe
    public void onItemClick(OnTrackerItemClick eventClick) {
        if (eventClick.isOnDescriptionClick()) {

            HolderActivity.navigateToProgramOverviewFragment(getActivity(),orgUnitId, programId,
                            eventClick.getItem().getLocalId());
        } else {
            showStatusDialog(eventClick.getItem());
        }
    }


    public void showStatusDialog(BaseSerializableModel model) {
        ItemStatusDialogFragment fragment = ItemStatusDialogFragment.newInstance(model);
        fragment.show(getChildFragmentManager());
    }

    @Override
    public void onLoaderReset(Loader<LocalSearchResultFragmentForm> loader) {

    }

    public void searchOnline() {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.search_online_cardview: {
                searchOnline();
            }
        }
    }
}
