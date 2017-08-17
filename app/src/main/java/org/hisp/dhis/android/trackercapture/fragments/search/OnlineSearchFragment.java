package org.hisp.dhis.android.trackercapture.fragments.search;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.AbsTextWatcher;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EventCoordinatesRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IndicatorRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.StatusRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;

import java.util.ArrayList;
import java.util.List;

public class OnlineSearchFragment extends Fragment implements View.OnClickListener, LoaderManager.LoaderCallbacks<OnlineSearchFragmentForm> {
    public static final String TAG = OnlineSearchFragment.class.getSimpleName();

    private static final int LOADER_ID = 956401;

    private OnlineSearchFragmentForm mForm;
    private EditText mFilter;
    private TextView mDialogLabel;
    private DataValueAdapter mAdapter;
    private ListView mListView;
    private int mDialogId;
    private View progressBar;
    private boolean backNavigation;

    public static final String EXTRA_PROGRAM = "extra:trackedEntityAttributes";
    public static final String EXTRA_ORGUNIT = "extra:orgUnit";
    public static final String EXTRA_DETAILED = "extra:detailed";
    public static final String EXTRA_ARGUMENTS = "extra:Arguments";
    public static final String EXTRA_NAVIGATION = "extra:Navigation";
    public static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    public static OnlineSearchFragment newInstance(String program, String orgUnit) {
        OnlineSearchFragment dialogFragment = new OnlineSearchFragment();
        Bundle args = new Bundle();

        args.putString(EXTRA_ORGUNIT, orgUnit);
        args.putString(EXTRA_PROGRAM, program);
        args.putBoolean(EXTRA_DETAILED, false);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    private String getOrgUnit() {
        return getArguments().getString(EXTRA_ORGUNIT);
    }

    private String getProgram() {
        return getArguments().getString(EXTRA_PROGRAM);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_online_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_to_device) {
            progressBar.setVisibility(View.VISIBLE);
            runQuery();
        } else if (id == android.R.id.home) {
            getActivity().finish();
        }

        return super.onOptionsItemSelected(item);
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
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(org.hisp.dhis.android.sdk.R.layout.dialog_fragment_teiqueryresult, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mListView = (ListView) view
                .findViewById(org.hisp.dhis.android.sdk.R.id.simple_listview);

        View header = getLayoutInflater(savedInstanceState).inflate(
                org.hisp.dhis.android.trackercapture.R.layout.fragmentdialog_querytei_header, mListView, false
        );

        if (getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        FloatingActionButton detailedSearchButton = (FloatingActionButton) header.findViewById(org.hisp.dhis.android.trackercapture.R.id.detailed_search_button);
        detailedSearchButton.setOnClickListener(this);
        mListView.addHeaderView(header, TAG, false);


        mFilter = (EditText) view
                .findViewById(org.hisp.dhis.android.sdk.R.id.filter_options);
        mDialogLabel = (TextView) view
                .findViewById(org.hisp.dhis.android.sdk.R.id.dialog_label);
        UiUtils.hideKeyboard(getActivity());

        mAdapter = new DataValueAdapter(getChildFragmentManager(),
                getActivity().getLayoutInflater(), mListView, getContext());
        mListView.setAdapter(mAdapter);

        mFilter.addTextChangedListener(new AbsTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (mForm != null) {
                    mForm.setQueryString(s.toString());
                }
            }
        });

        progressBar = view.findViewById(R.id.progress_bar);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);
        getActionBar().setTitle(getString(R.string.download_entities_title));
    }

    @Override
    public Loader<OnlineSearchFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String programId = fragmentArguments.getString(EXTRA_PROGRAM);
            String orgUnitId = fragmentArguments.getString(EXTRA_ORGUNIT);
            backNavigation = fragmentArguments.getBoolean(EXTRA_NAVIGATION);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new OnlineSearchFragmentQuery(
                    orgUnitId, programId)
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<OnlineSearchFragmentForm> loader, OnlineSearchFragmentForm data) {

        Log.d(TAG, "load finished");
        if (loader.getId() == LOADER_ID && isAdded()) {
            mListView.setVisibility(View.VISIBLE);

            mForm = data;

            if (mForm.getDataEntryRows() != null) {
                if (getArguments().getBoolean(EXTRA_DETAILED)) {
                    mAdapter.swapData(mForm.getDataEntryRows());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<OnlineSearchFragmentForm> loader) {

    }

    @Subscribe
    public void onShowDetailedInfo(OnDetailedInfoButtonClick eventClick) // may re-use code from DataEntryFragment
    {
        String message = "";

        if (eventClick.getRow() instanceof EventCoordinatesRow)
            message = getResources().getString(org.hisp.dhis.android.sdk.R.string.detailed_info_coordinate_row);
        else if (eventClick.getRow() instanceof StatusRow)
            message = getResources().getString(org.hisp.dhis.android.sdk.R.string.detailed_info_status_row);
        else if (eventClick.getRow() instanceof IndicatorRow)
            message = ""; // need to change ProgramIndicator to extend BaseValue for this to work
        else         // rest of the rows can either be of data element or tracked entity instance attribute
            message = eventClick.getRow().getDescription();

        UiUtils.showConfirmDialog(getActivity(),
                getResources().getString(org.hisp.dhis.android.sdk.R.string.detailed_info_dataelement),
                message, getResources().getString(org.hisp.dhis.android.sdk.R.string.ok_option),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
    }

    public void toggleDetailedSearch(View v) {
        FloatingActionButton button = (FloatingActionButton) v;
        boolean current = getArguments().getBoolean(EXTRA_DETAILED);
        if (current) {
            button.setImageResource(org.hisp.dhis.android.sdk.R.drawable.ic_new);
            mAdapter.swapData(null);
        } else {
            button.setImageResource(org.hisp.dhis.android.trackercapture.R.drawable.ic_close_dialog);

            if (mForm != null && mForm.getDataEntryRows() != null) {
                mAdapter.swapData(mForm.getDataEntryRows());
            }

        }
        getArguments().putBoolean(EXTRA_DETAILED, !current);
    }

    /* This method must be called only after onViewCreated() */
    public void setDialogLabel(int resourceId) {
        if (mDialogLabel != null) {
            mDialogLabel.setText(resourceId);
        }
    }

    /* This method must be called only after onViewCreated() */
    public void setDialogLabel(CharSequence sequence) {
        if (mDialogLabel != null) {
            mDialogLabel.setText(sequence);
        }
    }

    public void setDialogId(int dialogId) {
        mDialogId = dialogId;
    }

    public int getDialogId() {
        return mDialogId;
    }

    /* This method must be called only after onViewCreated() */
    public CharSequence getDialogLabel() {
        if (mDialogLabel != null) {
            return mDialogLabel.getText();
        } else {
            return null;
        }
    }

    public DataValueAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == org.hisp.dhis.android.trackercapture.R.id.detailed_search_button) {
            toggleDetailedSearch(v);
        }
    }

    public void runQuery() {
        final List<TrackedEntityAttributeValue> searchValues = new ArrayList<>();
        if (mForm != null && mForm.getTrackedEntityAttributeValues() != null &&
                mForm.getOrganisationUnit() != null && mForm.getProgram() != null) {
            for (TrackedEntityAttributeValue value : mForm.getTrackedEntityAttributeValues()) {
                searchValues.add(value);
            }
        }
        final boolean detailedSearch = getArguments().getBoolean(EXTRA_DETAILED);

        if (mForm != null) {

            try {
                queryTrackedEntityInstances(getChildFragmentManager(),
                        mForm.getOrganisationUnit(), mForm.getProgram(),
                        mForm.getQueryString
                                (), detailedSearch, searchValues.toArray(new TrackedEntityAttributeValue[]{}));
            } catch (Exception e) {
                showQueryError();
            }
        } else {
            showQueryError();
        }

    }

    private void showQueryError() {
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(getContext(), "Error. Please retry", Toast.LENGTH_SHORT).show();
    }

    /**
     * Queries the server for TrackedEntityInstances and shows a Dialog containing the results
     *
     * @param orgUnit
     * @param program can be null
     * @param params  can be null
     */
    public void queryTrackedEntityInstances(final FragmentManager fragmentManager, final String orgUnit, final String program, final String queryString, final boolean detailedSearch, final TrackedEntityAttributeValue... params)
            throws APIException {

        JobExecutor.enqueueJob(new NetworkJob<Object>(1,
                null) {

            @Override
            public Object execute() throws APIException {

                List<TrackedEntityInstance> trackedEntityInstancesQueryResult = null;
                if (detailedSearch) {
                    trackedEntityInstancesQueryResult = TrackerController.queryTrackedEntityInstancesDataFromAllAccessibleOrgUnits(DhisController.getInstance().getDhisApi(), orgUnit, program, queryString, detailedSearch, params);
                } else {
                    trackedEntityInstancesQueryResult = TrackerController.queryTrackedEntityInstancesDataFromServer(DhisController.getInstance().getDhisApi(), orgUnit, program, queryString, params);
                }

                // showTrackedEntityInstanceQueryResultDialog(fragmentManager, trackedEntityInstancesQueryResult, orgUnit);
                showOnlineSearchResultFragment(trackedEntityInstancesQueryResult, orgUnit, program, backNavigation);
                return new Object();
            }
        });
    }

    public void showOnlineSearchResultFragment(final List<TrackedEntityInstance> trackedEntityInstances, final String orgUnit, final String programId, final boolean backNavigation) {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.INVISIBLE);
                    HolderActivity.navigateToOnlineSearchResultFragment(getActivity(), trackedEntityInstances, orgUnit, programId, backNavigation);
                }
            });
        }
    }
}
