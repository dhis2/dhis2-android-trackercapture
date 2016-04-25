package org.hisp.dhis.android.trackercapture.fragments.search;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;

import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocalSearchFragment extends Fragment implements LoaderManager.LoaderCallbacks<LocalSearchFragmentForm> {
    public static final String EXTRA_PROGRAM = "extra:ProgramId";
    public static final String EXTRA_ORGUNIT = "extra:OrgUnitId";
    public static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";
    private final int LOADER_ID = 99898989;
    private String orgUnitId;
    private String programId;
    private ListView trackedEntityAttributeListView;
    private LocalSearchFragmentForm mForm;
    private DataValueAdapter mAdapter;
    private INavigationHandler navigationHandler;


    public static LocalSearchFragment newInstance(String orgUnitId, String programId) {
        LocalSearchFragment fragment = new LocalSearchFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_ORGUNIT, orgUnitId);
        args.putString(EXTRA_PROGRAM, programId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    Bundle arguments = getArguments();
    programId = arguments.getString(EXTRA_PROGRAM);
    orgUnitId = arguments.getString(EXTRA_ORGUNIT);

    setHasOptionsMenu(true);

}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_local_search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {

            buildQuery();
            LocalSearchResultFragment fragment = LocalSearchResultFragment.newInstance(
                    mForm.getOrganisationUnitId(), mForm.getProgram(), mForm.getAttributeValues());
            navigationHandler.switchFragment(
                    fragment, fragment.getClass().getSimpleName(), true);
        }
        else if (id == android.R.id.home) {
            getFragmentManager().popBackStack();
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private ActionBar getActionBar() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            throw new IllegalArgumentException("Fragment should be attached to ActionBarActivity");
        }
    }

    public void buildQuery() {
        HashMap<String, String> attributeValueMap = new HashMap<>();

        for(TrackedEntityAttributeValue value : mForm.getTrackedEntityAttributeValues()) {
            attributeValueMap.put(value.getTrackedEntityAttributeId(), value.getValue());
        }

        mForm.setAttributeValues(attributeValueMap);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_local_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        trackedEntityAttributeListView = (ListView) view.findViewById(R.id.localSearchAttributeListView);
        mAdapter = new DataValueAdapter(getChildFragmentManager(), getActivity().getLayoutInflater());
        trackedEntityAttributeListView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_ORGUNIT, orgUnitId);
        bundle.putString(EXTRA_PROGRAM, programId);
        bundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, bundle, this);

    }

    @Override
    public Loader<LocalSearchFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            String orgUnitId = args.getString(EXTRA_ORGUNIT);
            String programId = args.getString(EXTRA_PROGRAM);

            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(TrackedEntityInstance.class);
            modelsToTrack.add(Enrollment.class);
            modelsToTrack.add(Event.class);
            modelsToTrack.add(FailedItem.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new LocalSearchFragmentFormQuery(orgUnitId, programId));
        }

        return null;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof INavigationHandler) {
            navigationHandler = (INavigationHandler) activity;
        } else {
            throw new IllegalArgumentException("Activity must " +
                    "implement INavigationHandler interface");
        }
    }

    @Override
    public void onLoadFinished(Loader<LocalSearchFragmentForm> loader, LocalSearchFragmentForm data) {
        if (loader.getId() == LOADER_ID && isAdded()) {
            trackedEntityAttributeListView.setVisibility(View.VISIBLE);

            mForm = data;

            if (mForm.getDataEntryRows() != null) {

                mAdapter.swapData(mForm.getDataEntryRows());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<LocalSearchFragmentForm> loader) {

    }
}
















