package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.controllers.Dhis2;
import org.hisp.dhis.android.sdk.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.network.http.ApiRequestCallback;
import org.hisp.dhis.android.sdk.network.http.Response;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.utils.APIException;
import org.hisp.dhis.android.sdk.utils.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by erling on 5/18/15.
 */
public class TrackedEntityInstanceProfileFragment extends Fragment implements OnBackPressedListener,
        LoaderManager.LoaderCallbacks<TrackedEntityInstanceProfileFragmentForm>
{
    public static final String TAG = TrackedEntityInstanceProfileFragment.class.getName();
    public static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";
    public static final String PROGRAM_ID = "extra:ProgramId";

    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    private static final int LOADER_ID = 95640;

    private boolean edit;
    private boolean editableDataEntryRows;
    private boolean saving;

    private INavigationHandler mNavigationHandler;
    private ProgressBar mProgressBar;
    private ListView mListView;
    private DataValueAdapter mListViewAdapter;
    private TrackedEntityInstanceProfileFragmentForm mForm;

    AdapterView.OnItemClickListener onItemClickListener;

    public TrackedEntityInstanceProfileFragment()
    {
    }

    public static TrackedEntityInstanceProfileFragment newInstance(long mTrackedEntityInstanceId, String mProgramId)
    {
        TrackedEntityInstanceProfileFragment fragment = new TrackedEntityInstanceProfileFragment();
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putLong(TRACKEDENTITYINSTANCE_ID, mTrackedEntityInstanceId);
        fragmentArgs.putString(PROGRAM_ID, mProgramId);

        fragment.setArguments(fragmentArgs);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        editableDataEntryRows = false;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trackedentityinstanceprofile, container, false);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(org.hisp.dhis.android.sdk.R.menu.menu_data_entry, menu);
        Log.d(TAG, "onCreateOptionsMenu");
        final MenuItem editFormButton = menu.findItem(org.hisp.dhis.android.sdk.R.id.action_new_event);


        editFormButton.setEnabled(true);
        editFormButton.setIcon(R.drawable.ic_edit);
        editFormButton.getIcon().setAlpha(0xFF);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            doBack();
            return true;
        }
        else if (menuItem.getItemId() == org.hisp.dhis.android.sdk.R.id.action_new_event)
        {

            if (editableDataEntryRows) {
                setEditableDataEntryRows(false);
            } else {
                setEditableDataEntryRows(true);
            }
            editableDataEntryRows = !editableDataEntryRows;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.getEventBus().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.getEventBus().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        if (activity instanceof INavigationHandler) {
            ((INavigationHandler) activity).setBackPressedListener(this);
        }

        if (activity instanceof INavigationHandler) {
            mNavigationHandler = (INavigationHandler) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement INavigationHandler interface");
        }
    }

    @Override
    public void onDetach() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }

        // we need to nullify reference
        // to parent activity in order not to leak it
        if (getActivity() != null &&
                getActivity() instanceof INavigationHandler) {
            ((INavigationHandler) getActivity()).setBackPressedListener(null);
        }

        mListView = null;
        mNavigationHandler = null;
        Log.d(TAG, "FRAGMENT IS DETACHED");
        super.onDetach();
    }

    @Override
    public void doBack() {
        if(edit)
        {
            Dhis2.getInstance().showConfirmDialog(getActivity(),
                    getString(org.hisp.dhis.android.sdk.R.string.discard), getString(org.hisp.dhis.android.sdk.R.string.discard_confirm_changes),
                    getString(org.hisp.dhis.android.sdk.R.string.discard),
                    getString(org.hisp.dhis.android.sdk.R.string.save_and_close),
                    getString(org.hisp.dhis.android.sdk.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDetach();
                            getFragmentManager().popBackStack();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            submitChanges();
                            onDetach();
                            getFragmentManager().popBackStack();

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        }
        else
        {
            onDetach();
            getFragmentManager().popBackStack();
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.teiprofileprogressbar);
        mProgressBar.setVisibility(View.GONE);

        getActivity().getLayoutInflater();
        mListViewAdapter = new DataValueAdapter(getChildFragmentManager(), getActivity().getLayoutInflater());
        mListView = (ListView) view.findViewById(R.id.profile_listview);
        mListView.setVisibility(View.VISIBLE);
        onItemClickListener = mListView.getOnItemClickListener();

        mListView.setAdapter(mListViewAdapter);

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
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);

        mProgressBar.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    @Override
    public Loader<TrackedEntityInstanceProfileFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String programId = fragmentArguments.getString(PROGRAM_ID);
            long trackedEntityInstance = fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new TrackedEntityInstanceProfileFragmentQuery(
                    trackedEntityInstance, programId
            )
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<TrackedEntityInstanceProfileFragmentForm> loader, TrackedEntityInstanceProfileFragmentForm data) {

        if (loader.getId() == LOADER_ID && isAdded())
        {
            mProgressBar.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);

            mForm = data;

            if(mForm.getDataEntryRows() != null)
            {
                setEditableDataEntryRows(false);
//                mListViewAdapter.swapData(mForm.getDataEntryRows());

            }

        }
    }

    @Override
    public void onLoaderReset(Loader<TrackedEntityInstanceProfileFragmentForm> loader)
    {
        if (mListViewAdapter != null)
            mListViewAdapter.swapData(null);

    }

    public void setEditableDataEntryRows(boolean editable)
    {
        List<DataEntryRow> rows = new ArrayList<>(mForm.getDataEntryRows());
//        List<DataEntryRow> rows = mForm.getDataEntryRows();
        mListViewAdapter.swapData(null);
        if(editable)
        {
            for(DataEntryRow row : rows)
            {
                row.setEditable(true);
            }
        }
        else
        {
            for(DataEntryRow row : rows)
            {
                row.setEditable(false);
            }
        }
        mListView.setAdapter(null);
        mListViewAdapter.swapData(rows);
        mListView.setAdapter(mListViewAdapter);
    }
    public void flagDataChanged(boolean changed)
    {
        edit = changed;
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        Log.d(TAG, "onRowValueChanged");
        flagDataChanged(true);
        if (mForm == null ) {
            return;
        }

//        if (refreshing)
//            return; //we don't want to stack this up since it runs every time a character is entered for example
//        refreshing = true;

    }

    public void submitChanges()
    {
        if(saving) return;

        flagDataChanged(false);

        new Thread() {
            public void run() {
                saving = true;
                if(mForm!=null && isAdded())
                {
                    Log.d("TEIPROFILEFRAGMENT", "IS SAVING CHANGES");

                    final Context context = getActivity().getBaseContext();


                    for(TrackedEntityAttributeValue val : mForm.getTrackedEntityAttributeValues())
                    {
                        val.save();
                    }

                    mForm.getTrackedEntityInstance().setFromServer(true);
                    mForm.getTrackedEntityInstance().save();

                    mForm.getTrackedEntityInstance().setFromServer(false);
                    //mForm.getTrackedEntityInstance().lastUpdated = Utils.getCurrentTime();
                    mForm.getTrackedEntityInstance().save();



                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            ApiRequestCallback callback = new ApiRequestCallback() {
                                @Override
                                public void onSuccess(Response response) {

                                }

                                @Override
                                public void onFailure(APIException exception) {

                                }
                            };
                            Dhis2.sendLocalData(context, callback);
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(timerTask, 5000);
                }
                saving = false;
            }

        }.start();

    }
}
