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

package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import android.content.DialogInterface;
import android.os.Bundle;


import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRuleAction;
import org.hisp.dhis.android.sdk.ui.adapters.SectionAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by erling on 5/18/15.
 */
public class TrackedEntityInstanceProfileFragment extends DataEntryFragment<TrackedEntityInstanceProfileFragmentForm>
{
    public static final String TAG = TrackedEntityInstanceProfileFragment.class.getName();
    public static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";
    public static final String PROGRAM_ID = "extra:ProgramId";

    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    private static final int LOADER_ID = 95640;

    private boolean edit;
    private boolean editableDataEntryRows;

    private TrackedEntityInstanceProfileFragmentForm mForm;
    private SaveThread saveThread;

    public TrackedEntityInstanceProfileFragment() {
    }

    public static TrackedEntityInstanceProfileFragment newInstance(long mTrackedEntityInstanceId, String mProgramId) {
        TrackedEntityInstanceProfileFragment fragment = new TrackedEntityInstanceProfileFragment();
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putLong(TRACKEDENTITYINSTANCE_ID, mTrackedEntityInstanceId);
        fragmentArgs.putString(PROGRAM_ID, mProgramId);

        fragment.setArguments(fragmentArgs);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(saveThread == null || saveThread.isKilled()) {
            saveThread = new SaveThread();
            saveThread.start();
        }
        saveThread.init(this);
        setHasOptionsMenu(true);
        editableDataEntryRows = false;
    }

    @Override
    public void onDestroy() {
        saveThread.kill();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(org.hisp.dhis.android.sdk.R.menu.menu_data_entry, menu);
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
        else if (menuItem.getItemId() == org.hisp.dhis.android.sdk.R.id.action_new_event) {
            if (editableDataEntryRows) {
                setEditableDataEntryRows(false);
            } else {
                setEditableDataEntryRows(true);
            }
            editableDataEntryRows = !editableDataEntryRows;
        }

        return super.onOptionsItemSelected(menuItem);
    }


    public void doBack() {
        if(edit) {
            UiUtils.showConfirmDialog(getActivity(),
                    getString(org.hisp.dhis.android.sdk.R.string.discard), getString(org.hisp.dhis.android.sdk.R.string.discard_confirm_changes),
                    getString(org.hisp.dhis.android.sdk.R.string.save_and_close),
                    getString(org.hisp.dhis.android.sdk.R.string.discard),
                    getString(org.hisp.dhis.android.sdk.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            save();
                            onDetach();
                            getFragmentManager().popBackStack();
                            DhisController.hasUnSynchronizedDatavalues = true;

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDetach();
                            getFragmentManager().popBackStack();

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        } else {
            onDetach();
            getFragmentManager().popBackStack();
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

        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
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
        if (loader.getId() == LOADER_ID && isAdded()) {
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            mForm = data;

            if(mForm.getDataEntryRows() != null) {
                setEditableDataEntryRows(false);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<TrackedEntityInstanceProfileFragmentForm> loader) {
        if (listViewAdapter != null) {
            listViewAdapter.swapData(null);
        }
    }

    public void setEditableDataEntryRows(boolean editable) {
        List<Row> rows = new ArrayList<>(mForm.getDataEntryRows());
        listViewAdapter.swapData(null);
        if(editable) {
            for(Row row : rows) {
                row.setEditable(true);
            }
        } else {
            for(Row row : rows) {
                row.setEditable(false);
            }
        }
        listView.setAdapter(null);
        listViewAdapter.swapData(rows);
        listView.setAdapter(listViewAdapter);
    }



    public void flagDataChanged(boolean changed)
    {
        edit = changed;
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        flagDataChanged(true);
        if (mForm == null ) {
            return;
        }
        saveThread.schedule();
    }

    @Override
    public SectionAdapter getSpinnerAdapter() {
        return null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    protected ArrayList<String> getValidationErrors() {
        return null;
    }

    @Override
    protected boolean isValid() {
        return true;
    }

    @Override
    protected void save() {
        if(!edit) {// if rows are not edited
            return;
        }

        if(mForm!=null && isAdded() && mForm.getTrackedEntityInstance() != null ) {
            for(TrackedEntityAttributeValue val : mForm.getTrackedEntityAttributeValues()) {
                val.save();
            }
            mForm.getTrackedEntityInstance().setFromServer(false);
            mForm.getTrackedEntityInstance().save();
        }

        flagDataChanged(false);
    }

    @Override
    protected void proceed() {

    }

    @Subscribe
    public void onDetailedInfoClick(OnDetailedInfoButtonClick eventClick) {
        super.onShowDetailedInfo(eventClick);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
