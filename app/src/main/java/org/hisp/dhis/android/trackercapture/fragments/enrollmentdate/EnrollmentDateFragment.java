package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by erling on 7/16/15.
 */
public class EnrollmentDateFragment extends DataEntryFragment<EnrollmentDateFragmentForm>
{
    private static final String ENROLLMENT_ID = "extra:EnrollmentId";
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

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(org.hisp.dhis.android.sdk.R.menu.menu_data_entry, menu);
        Log.d(TAG, "onCreateOptionsMenu");

        final MenuItem editFormButton = menu.findItem(org.hisp.dhis.android.sdk.R.id.action_new_event);

        editFormButton.setEnabled(true);
        editFormButton.setIcon(R.drawable.ic_edit);
        editFormButton.getIcon().setAlpha(0xFF);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            getFragmentManager().popBackStack();
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

    public void flagDataChanged(boolean changed)
    {
        edit = changed;
    }






    public void setEditableDataEntryRows(boolean editable)
    {
        List<Row> rows = new ArrayList<>(mForm.getDataEntryRows());
        listViewAdapter.swapData(null);
        if(editable)
        {
            for(Row row : rows)
            {
                row.setEditable(true);
            }
        }
        else
        {
            for(Row row : rows)
            {
                row.setEditable(false);
            }
        }
        listView.setAdapter(null);
        listViewAdapter.swapData(rows);
        listView.setAdapter(listViewAdapter);
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        Log.d(TAG, "onRowValueChanged");
        flagDataChanged(true);
        if (mForm == null ) {
            return;
        }
        save();

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
        if(!edit) return; // if rows are not edited, return

        flagDataChanged(false);

        if(mForm!=null && isAdded())
        {
            mForm.getEnrollment().setFromServer(false);
            mForm.getEnrollment().save();
        }
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