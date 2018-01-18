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

package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.ErrorType;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.SectionAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Deprecated
/**
 * @deprecated Use EnrollmentFragment instead to avoid having fragments doing the same thing
 */
public class EnrollmentDateFragment extends DataEntryFragment<EnrollmentDateFragmentForm>
{
    public static final String ENROLLMENT_ID = "extra:EnrollmentId";
    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";
    public static final String TAG = EnrollmentDateFragment.class.getSimpleName();
    private boolean edit;
    private boolean editableRows;
    private EnrollmentDateFragmentForm mForm;
    private SaveThread saveThread;


    public static EnrollmentDateFragment newInstance(long enrollmentId) {
        EnrollmentDateFragment fragment = new EnrollmentDateFragment();
        Bundle args = new Bundle();
        args.putLong(ENROLLMENT_ID, enrollmentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(saveThread == null || saveThread.isKilled()) {
            saveThread = new SaveThread();
            saveThread.start();
        }
        saveThread.init(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        saveThread.kill();
        super.onDestroy();
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(R.string.enrollment);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
//            getFragmentManager().popBackStack();
            getActivity().finish();
        }
        else if (menuItem.getItemId() == org.hisp.dhis.android.sdk.R.id.action_new_event)
        {

            if (editableRows) {
                setEditableDataEntryRows(false);
            } else {
                setEditableDataEntryRows(true);
            }
            editableRows= !editableRows;
        }
        return true;
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

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        flagDataChanged(true);
        if (mForm == null ) {
            return;
        }
        edit = true;
        save();

    }

    @Override
    public SectionAdapter getSpinnerAdapter() {
        return null;
    }

    @Override
    protected HashMap<ErrorType, ArrayList<String>> getValidationErrors() {
        return null;
    }

    @Override
    protected boolean isValid() {
        return true;
    }

    @Override
    protected void save() {
        if(!edit) return; // if rows are not edited, return

        flagDataChanged(false);

        if(mForm!=null && isAdded())
        {
            mForm.getEnrollment().setFromServer(false);
            TrackedEntityInstance trackedEntityInstance = TrackerController.getTrackedEntityInstance(mForm.getEnrollment().getTrackedEntityInstance());
            trackedEntityInstance.setFromServer(false);
            mForm.getEnrollment().save();

        }
        edit = false;
    }

    @Override
    protected void proceed() {

    }


    //@Override
    protected boolean goBack() {
        if(isValid()) {
            goBackToPreviousActivity();
        }
        return false;
    }

    private void goBackToPreviousActivity() {
        getActivity().finish();
    }


    @Override
    public void onLoadFinished(Loader<EnrollmentDateFragmentForm> loader, EnrollmentDateFragmentForm data)
    {
        mForm = data;
        progressBar.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        listViewAdapter.swapData(data.getDataEntryRows());
        setEditableDataEntryRows(false);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {

        if (LOADER_ID == id && isAdded()) {

            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            long enrollmentId = fragmentArguments.getLong(ENROLLMENT_ID, 0);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new EnrollmentDateFragmentQuery(
                    enrollmentId)
            );
        }
        return null;
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }
}