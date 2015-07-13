/*
 * Copyright (c) 2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.Dhis2;
import org.hisp.dhis.android.sdk.controllers.ResponseHolder;
import org.hisp.dhis.android.sdk.controllers.datavalues.DataValueLoader;
import org.hisp.dhis.android.sdk.controllers.datavalues.TrackedEntityInstancesResultHolder;
import org.hisp.dhis.android.sdk.network.http.ApiRequestCallback;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.utils.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.AbsTextWatcher;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.utils.ui.views.FloatingActionButton;
import org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile.TrackedEntityInstanceProfileFragmentForm;
import org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile.TrackedEntityInstanceProfileFragmentQuery;

import java.util.ArrayList;
import java.util.List;

public class QueryTrackedEntityInstancesDialogFragment extends DialogFragment
        implements View.OnClickListener, LoaderManager.LoaderCallbacks<QueryTrackedEntityInstancesDialogFragmentForm> {
    private static final String TAG = QueryTrackedEntityInstancesDialogFragment.class.getSimpleName();

    private static final int LOADER_ID = 956401;

    private QueryTrackedEntityInstancesDialogFragmentForm mForm;
    private EditText mFilter;
    private TextView mDialogLabel;
    private DataValueAdapter mAdapter;
    private ListView mListView;
    private int mDialogId;

    private static final String EXTRA_PROGRAM = "extra:trackedEntityAttributes";
    private static final String EXTRA_ORGUNIT = "extra:orgUnit";
    private static final String EXTRA_DETAILED = "extra:detailed";
    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    public static QueryTrackedEntityInstancesDialogFragment newInstance(String program, String orgUnit) {
        QueryTrackedEntityInstancesDialogFragment dialogFragment = new QueryTrackedEntityInstancesDialogFragment();
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
        setStyle(DialogFragment.STYLE_NO_TITLE,
                R.style.Theme_AppCompat_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return inflater.inflate(R.layout.dialog_fragment_teiqueryresult, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mListView = (ListView) view
                .findViewById(R.id.simple_listview);

        View header = getLayoutInflater(savedInstanceState).inflate(
                org.hisp.dhis.android.trackercapture.R.layout.fragmentdialog_querytei_header, mListView, false
        );
        FloatingActionButton detailedSearchButton = (FloatingActionButton) header.findViewById(org.hisp.dhis.android.trackercapture.R.id.detailed_search_button);
        detailedSearchButton.setOnClickListener(this);
        mListView.addHeaderView(header, TAG, false);

        ImageView loadDialogButton = (ImageView) view
                .findViewById(R.id.load_dialog_button);
        ImageView closeDialogButton = (ImageView) view
                .findViewById(R.id.close_dialog_button);
        mFilter = (EditText) view
                .findViewById(R.id.filter_options);
        mDialogLabel = (TextView) view
                .findViewById(R.id.dialog_label);
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mFilter.getWindowToken(), 0);

        mAdapter = new DataValueAdapter(getChildFragmentManager(), getActivity().getLayoutInflater());
        mListView.setAdapter(mAdapter);

        mFilter.addTextChangedListener(new AbsTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                mForm.setQueryString(s.toString());
            }
        });

        closeDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dismiss();
            }
        });

        loadDialogButton.setOnClickListener(this);

        setDialogLabel(R.string.query_tracked_entity_instances);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);
    }

    @Override
    public Loader<QueryTrackedEntityInstancesDialogFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String programId = fragmentArguments.getString(EXTRA_PROGRAM);
            String orgUnitId = fragmentArguments.getString(EXTRA_ORGUNIT);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new QueryTrackedEntityInstancesDialogFragmentQuery(
                    orgUnitId, programId)
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<QueryTrackedEntityInstancesDialogFragmentForm> loader, QueryTrackedEntityInstancesDialogFragmentForm data) {

        Log.d(TAG, "load finished");
        if (loader.getId() == LOADER_ID && isAdded())
        {
            mListView.setVisibility(View.VISIBLE);

            mForm = data;

            if(mForm.getDataEntryRows() != null)
            {
                //setEditableDataEntryRows(false);
                if(getArguments().getBoolean(EXTRA_DETAILED)) {
                    mAdapter.swapData(mForm.getDataEntryRows());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<QueryTrackedEntityInstancesDialogFragmentForm> loader) {

    }

    public void toggleDetailedSearch(View v) {
        FloatingActionButton button = (FloatingActionButton) v;
        boolean current = getArguments().getBoolean(EXTRA_DETAILED);
        if(current) {
            button.setImageResource(R.drawable.ic_new);
            mAdapter.swapData(null);
        } else {
            button.setImageResource(R.drawable.ic_delete);
            mAdapter.swapData(mForm.getDataEntryRows());
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

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.load_dialog_button) {
            dismiss();
            runQuery();
        } else if(v.getId() == org.hisp.dhis.android.trackercapture.R.id.detailed_search_button) {
            toggleDetailedSearch(v);
        }
    }

    public void runQuery() {
        List<TrackedEntityAttributeValue> searchValues = new ArrayList<>();
        if(mForm!=null && mForm.getTrackedEntityAttributeValues()!=null) {
            for (TrackedEntityAttributeValue value : mForm.getTrackedEntityAttributeValues()) {
                searchValues.add(value);
            }
        }
        Dhis2.queryTrackedEntityInstances(getFragmentManager(), mForm.getOrganisationUnit(), mForm.getProgram(), mForm.getQueryString(), searchValues.toArray(new TrackedEntityAttributeValue[]{}));
    }
}
