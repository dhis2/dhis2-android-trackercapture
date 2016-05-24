package org.hisp.dhis.android.trackercapture.fragments.search;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.events.LoadingMessageEvent;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.job.Job;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.network.ResponseHolder;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.ui.adapters.rows.AbsTextWatcher;
import org.hisp.dhis.android.sdk.ui.dialogs.QueryTrackedEntityInstancesResultDialogAdapter;
import org.hisp.dhis.android.sdk.ui.fragments.progressdialog.ProgressDialogFragment;
import org.hisp.dhis.android.sdk.ui.fragments.settings.SettingsFragment;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OnlineSearchResultFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {
    public static final String TAG = OnlineSearchResultFragment.class.getSimpleName();

    private EditText mFilter;
    private TextView mDialogLabel;
    private QueryTrackedEntityInstancesResultDialogAdapter mAdapter;
    private int mDialogId;
    private ProgressDialogFragment progressDialogFragment;
    private Button mSelectAllButton;
    private ListView mListView;

    public static final String EXTRA_TRACKEDENTITYINSTANCESLIST = "extra:trackedEntityInstances";
    public static final String EXTRA_TRACKEDENTITYINSTANCESSELECTED = "extra:trackedEntityInstancesSelected";
    public static final String EXTRA_ORGUNIT = "extra:orgUnit";
    public static final String EXTRA_SELECTALL = "extra:selectAll";

    public static OnlineSearchResultFragment newInstance(List<TrackedEntityInstance> trackedEntityInstances, String orgUnit) {
        OnlineSearchResultFragment dialogFragment = new OnlineSearchResultFragment();
        Bundle args = new Bundle();

        ParameterSerializible parameterSerializible1 = new ParameterSerializible(trackedEntityInstances);
        ParameterSerializible parameterSerializible2 = new ParameterSerializible(new ArrayList<TrackedEntityInstance>());
        args.putSerializable(EXTRA_TRACKEDENTITYINSTANCESLIST, parameterSerializible1);
        args.putSerializable(EXTRA_TRACKEDENTITYINSTANCESSELECTED,parameterSerializible2);

        args.putString(EXTRA_ORGUNIT, orgUnit);
        args.putBoolean(EXTRA_SELECTALL, false);
        dialogFragment.setArguments(args);
        Dhis2Application.getEventBus().register(dialogFragment);
        return dialogFragment;
    }

    private List<TrackedEntityInstance> getTrackedEntityInstances() {
        ParameterSerializible parameterSerializible = (ParameterSerializible) getArguments().getSerializable(EXTRA_TRACKEDENTITYINSTANCESSELECTED);
        List<TrackedEntityInstance> trackedEntityInstances = parameterSerializible.getTrackedEntityInstances();
//        ParameterParcelable parameterParcelable = getArguments().getParcelable(EXTRA_TRACKEDENTITYINSTANCESLIST);
//        List<TrackedEntityInstance> trackedEntityInstances = parameterParcelable.getTrackedEntityInstances();
        return trackedEntityInstances;
    }

    private List<TrackedEntityInstance> getSelectedTrackedEntityInstances() {
        ParameterSerializible parameterSerializible = (ParameterSerializible) getArguments().getSerializable(EXTRA_TRACKEDENTITYINSTANCESLIST);
        List<TrackedEntityInstance> trackedEntityInstances = parameterSerializible.getTrackedEntityInstances();
//        ParameterParcelable parameterParcelable = getArguments().getParcelable(EXTRA_TRACKEDENTITYINSTANCESSELECTED);
//        List<TrackedEntityInstance> trackedEntityInstances = parameterParcelable.getTrackedEntityInstances();
        return trackedEntityInstances;
    }

    private String getOrgUnit() {
        return getArguments().getString(EXTRA_ORGUNIT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_online_search_result, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_to_device) {
            initiateLoading();
            getActivity().finish();
        }
        else if (id == android.R.id.home) {
            getActivity().finish();
        }

        return super.onOptionsItemSelected(item);
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

        if(getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        mFilter = (EditText) view
                .findViewById(org.hisp.dhis.android.sdk.R.id.filter_options);
        mDialogLabel = (TextView) view
                .findViewById(org.hisp.dhis.android.sdk.R.id.dialog_label);

        UiUtils.hideKeyboard(getActivity());

        mAdapter = new QueryTrackedEntityInstancesResultDialogAdapter(LayoutInflater.from(getActivity()), getSelectedTrackedEntityInstances());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        mFilter.addTextChangedListener(new AbsTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                mAdapter.getFilter().filter(s.toString());
            }
        });

        mSelectAllButton = (Button) view.findViewById(org.hisp.dhis.android.sdk.R.id.teiqueryresult_selectall);
        mSelectAllButton.setOnClickListener(this);
        mSelectAllButton.setVisibility(View.VISIBLE);
        boolean selectall = getArguments().getBoolean(EXTRA_SELECTALL);
        if(selectall) {
            mSelectAllButton.setText(getString(org.hisp.dhis.android.sdk.R.string.deselect_all));
        }




        getAdapter().swapData(getTrackedEntityInstances());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TrackedEntityInstance value = mAdapter.getItem(position);
        List<TrackedEntityInstance> selected = getSelectedTrackedEntityInstances();
        CheckBox checkBox = (CheckBox) view.findViewById(org.hisp.dhis.android.sdk.R.id.checkBoxTeiQuery);
        if(checkBox.isChecked()) {
            selected.remove(value);
            checkBox.setChecked(false);
        } else {
            selected.add(value);
            checkBox.setChecked(true);
        }
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

    public QueryTrackedEntityInstancesResultDialogAdapter getAdapter() {
        return mAdapter;
    }

    public void show(FragmentManager fragmentManager) {
        if(fragmentManager != null) {
            //show(fragmentManager, TAG);
        }
    }

    @Override
    public void onClick(View v) {
        //if(v.getId() == org.hisp.dhis.android.sdk.R.id.load_dialog_button) {
           // initiateLoading();
          //  getFragmentManager().popBackStack();
        //} else
        if(v.getId() == org.hisp.dhis.android.sdk.R.id.teiqueryresult_selectall) {
            toggleSelectAll();
        }
    }

    public void toggleSelectAll() {
        Bundle arguments = getArguments();
        boolean selectall = arguments.getBoolean(EXTRA_SELECTALL);
        if(selectall) {
            mSelectAllButton.setText(getText(org.hisp.dhis.android.sdk.R.string.select_all));
            deselectAll();
        } else {
            mSelectAllButton.setText(getText(org.hisp.dhis.android.sdk.R.string.deselect_all));
            selectAll();
        }
        arguments.putBoolean(EXTRA_SELECTALL, !selectall);
    }

    public void selectAll() {
        List<TrackedEntityInstance> allTrackedEntityInstances = mAdapter.getData();
        List<TrackedEntityInstance> selectedTrackedEntityInstances = getSelectedTrackedEntityInstances();
        selectedTrackedEntityInstances.clear();
        selectedTrackedEntityInstances.addAll(allTrackedEntityInstances);
        View view = null;
        for(int i = 0; i<allTrackedEntityInstances.size(); i++) {
            view = mAdapter.getView(i, view, null);
            CheckBox checkBox = (CheckBox) view.findViewById(org.hisp.dhis.android.sdk.R.id.checkBoxTeiQuery);
            checkBox.setChecked(true);
        }
        refreshListView();
    }

    public void deselectAll() {
        List<TrackedEntityInstance> allTrackedEntityInstances = mAdapter.getData();
        List<TrackedEntityInstance> selectedTrackedEntityInstances = getSelectedTrackedEntityInstances();
        selectedTrackedEntityInstances.clear();
        View view = null;
        for(int i = 0; i<allTrackedEntityInstances.size(); i++) {
            view = mAdapter.getView(i, view, null);
            CheckBox checkBox = (CheckBox) view.findViewById(org.hisp.dhis.android.sdk.R.id.checkBoxTeiQuery);
            checkBox.setChecked(false);
        }
        refreshListView();
    }

    public void refreshListView() {
        int start = mListView.getFirstVisiblePosition();
        int end = mListView.getLastVisiblePosition();
        for (int pos = 0; pos <= end - start; pos++) {
            View view = mListView.getChildAt(pos);
            if (view != null) {
                int adapterPosition = view.getId();
                if (adapterPosition < 0 || adapterPosition >= mAdapter.getCount())
                    continue;
                if (!view.hasFocus()) {
                    mAdapter.getView(adapterPosition, view, mListView);
                }
            }
        }
    }

    public void initiateLoading() {
        Log.d(TAG, "loading: " + getSelectedTrackedEntityInstances().size());

        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.TRACKEDENTITYINSTANCE) {

            @Override
            public Object execute() throws APIException {
                Dhis2Application.getEventBus().post(new UiEvent(UiEvent.UiEventType.SYNCING_START));
                TrackerController.getTrackedEntityInstancesDataFromServer(DhisController.getInstance().getDhisApi(), getSelectedTrackedEntityInstances(), true);
                Dhis2Application.getEventBus().post(new UiEvent(UiEvent.UiEventType.SYNCING_END));
                return new Object();
            }
        });

    }

    @Subscribe
    public void onLoadingMessageEvent(final LoadingMessageEvent event) {
        Log.d(TAG, "Message received" + event.message);
        if(progressDialogFragment!=null && progressDialogFragment.getDialog() != null &&
                progressDialogFragment.getDialog().isShowing()) {
            ((ProgressDialog) progressDialogFragment.getDialog()).setMessage(event.message);
        }
    }
    public static class ParameterSerializible implements Serializable {
        private List<TrackedEntityInstance> trackedEntityInstances;

        public ParameterSerializible(List<TrackedEntityInstance> trackedEntityInstances) {
            this.trackedEntityInstances = trackedEntityInstances;
        }

        public List<TrackedEntityInstance> getTrackedEntityInstances() {
            return trackedEntityInstances;
        }
    }
    public static class ParameterParcelable implements Parcelable {
        public static final String TAG = ParameterParcelable.class.getSimpleName();
        private List<TrackedEntityInstance> trackedEntityInstances;
        public ParameterParcelable(List<TrackedEntityInstance> trackedEntityInstances) {
            Log.d(TAG, "parcelputting " + trackedEntityInstances.size());
            this.trackedEntityInstances = trackedEntityInstances;
        }

        protected ParameterParcelable(Parcel in) {
        }

        public static final Creator<ParameterParcelable> CREATOR = new Creator<ParameterParcelable>() {
            @Override
            public ParameterParcelable createFromParcel(Parcel in) {
                return new ParameterParcelable(in);
            }

            @Override
            public ParameterParcelable[] newArray(int size) {
                return new ParameterParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeList(trackedEntityInstances);
        }

        public List<TrackedEntityInstance> getTrackedEntityInstances() {
            return trackedEntityInstances;
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
}
