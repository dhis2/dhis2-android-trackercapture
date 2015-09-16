package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by erling on 7/16/15.
 */
public class EnrollmentDateFragment extends DataEntryFragment<EnrollmentDateFragmentForm> implements OnBackPressedListener
{
    private static final String ENROLLMENT_ID = "extra:EnrollmentId";
    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";
    public static final String TAG = EnrollmentDateFragment.class.getSimpleName();
    private boolean edit;
    private boolean editableRows;
    private EnrollmentDateFragmentForm mForm;
    private static final String EMPTY_FIELD = "";
    private static final String DATE_FORMAT = "YYYY-MM-dd";
    private View enrollmentDatePicker;
    private View incidentDatePicker;
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

        enrollmentDatePicker = LayoutInflater.from(getActivity())
                .inflate(org.hisp.dhis.android.sdk.R.layout.fragment_data_entry_date_picker, listView, false);
        incidentDatePicker = LayoutInflater.from(getActivity())
                .inflate(org.hisp.dhis.android.sdk.R.layout.fragment_data_entry_date_picker, listView, false);
        listView.addHeaderView(enrollmentDatePicker);
        listView.addHeaderView(incidentDatePicker);
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
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            doBack();
            return true;
        }
        else if (menuItem.getItemId() == org.hisp.dhis.android.sdk.R.id.action_new_event)
        {

            if (editableRows) {
                setEditableDatePickerRows(false);
            } else {
                setEditableDatePickerRows(true);
            }
            editableRows= !editableRows;
        }

        return super.onOptionsItemSelected(menuItem);
    }


    @Override
    public void doBack()
    {
        if(edit)
        {
            UiUtils.showConfirmDialog(getActivity(),
                    getString(org.hisp.dhis.android.sdk.R.string.discard), getString(org.hisp.dhis.android.sdk.R.string.discard_confirm_changes),
                    getString(org.hisp.dhis.android.sdk.R.string.save_and_close),
                    getString(org.hisp.dhis.android.sdk.R.string.cancel),
                    getString(org.hisp.dhis.android.sdk.R.string.discard),
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
                            dialog.dismiss();

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDetach();
                            getFragmentManager().popBackStack();
                        }
                    });
        }
        else
        {
            onDetach();
            getFragmentManager().popBackStack();
        }
    }

    public void flagDataChanged(boolean changed)
    {
        edit = changed;
    }




    private void attachEnrollmentDatePicker() {
        if (mForm != null && isAdded()) {
            final EditText datePickerEditText = (EditText) enrollmentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
            View.OnClickListener onClearDateListener = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    datePickerEditText.setText(EMPTY_FIELD);
                    mForm.getEnrollment().setDateOfEnrollment(EMPTY_FIELD);
                }
            };
            DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DateUtils.DATE_PATTERN);
                    datePickerEditText.setText(newValue);
                    mForm.getEnrollment().setDateOfEnrollment(newValue);
                    onRowValueChanged(null);
                }
            };
            String reportDateDescription = mForm.getEnrollment().getProgram().getDateOfEnrollmentDescription()== null ?
                    getString(org.hisp.dhis.android.sdk.R.string.report_date) : mForm.getEnrollment().getProgram().getDateOfEnrollmentDescription();
            String value = null;
            if (mForm.getEnrollment() != null && mForm.getEnrollment().getDateOfEnrollment() != null) {
                DateTime date = DateTime.parse(mForm.getEnrollment().getDateOfEnrollment());
                String newValue = date.toString(DateUtils.DATE_PATTERN);
                datePickerEditText.setText(newValue);
            }
            setDatePicker(enrollmentDatePicker, reportDateDescription, value, onClearDateListener, onDateSetListener);
        }
    }

    private void attachIncidentDatePicker() {
        if (mForm != null && isAdded()) {
            final EditText datePickerEditText = (EditText) incidentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
            View.OnClickListener onClearDateListener = new View.OnClickListener() {
                @Override public void onClick(View v) {
                    datePickerEditText.setText(EMPTY_FIELD);
                    mForm.getEnrollment().setDateOfIncident(EMPTY_FIELD);
                }
            };
            DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DateUtils.DATE_PATTERN);
                    datePickerEditText.setText(newValue);
                    mForm.getEnrollment().setDateOfIncident(newValue);
                    onRowValueChanged(null);
                }
            };
            String reportDateDescription = mForm.getEnrollment().getProgram().getDateOfIncidentDescription()== null ?
                    getString(org.hisp.dhis.android.sdk.R.string.report_date) : mForm.getEnrollment().getProgram().getDateOfIncidentDescription();
            String value = null;
            if (mForm.getEnrollment() != null && mForm.getEnrollment().getDateOfIncident() != null) {
                DateTime date = DateTime.parse(mForm.getEnrollment().getDateOfIncident());
                value = date.toString(DateUtils.DATE_PATTERN);
            }
            setDatePicker(incidentDatePicker, reportDateDescription, value, onClearDateListener, onDateSetListener);
        }
    }

    private void setDatePicker(View datePicker, String labelValue, String dateValue, View.OnClickListener clearDateListener, final DatePickerDialog.OnDateSetListener onDateSetListener) {
        final TextView label = (TextView) datePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.text_label);
        final EditText datePickerEditText = (EditText) datePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
        final ImageButton clearDateButton = (ImageButton) datePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.clear_edit_text);
        clearDateButton.setOnClickListener(clearDateListener);
        datePickerEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDate currentDate = new LocalDate();
                DatePickerDialog picker = new DatePickerDialog(getActivity(),onDateSetListener
                        , currentDate.getYear(),
                        currentDate.getMonthOfYear() - 1,
                        currentDate.getDayOfMonth());
                picker.getDatePicker().setMaxDate(DateTime.now().getMillis());
                picker.show();
            }
        });
        label.setText(labelValue);
        if(dateValue!=null) {
            datePickerEditText.setText(dateValue);
        }
    }

    private void setEditableDatePickerRows(boolean editable)
    {
        //should rather fetch through loader


        final TextView enrollmentLabel = (TextView) enrollmentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.text_label);
        final EditText enrollmentDatePickerEditText = (EditText) enrollmentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
        final ImageButton enrollmentClearDateButton = (ImageButton) enrollmentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.clear_edit_text);

        final TextView incidentLabel = (TextView) incidentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.text_label);
        final EditText incidentDatePickerEditText = (EditText) incidentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
        final ImageButton incidentClearDateButton = (ImageButton) incidentDatePicker
                .findViewById(org.hisp.dhis.android.sdk.R.id.clear_edit_text);


        enrollmentDatePickerEditText.setEnabled(editable);
        enrollmentDatePickerEditText.setClickable(editable);
        enrollmentLabel.setEnabled(editable);
        enrollmentLabel.setClickable(editable);
        enrollmentClearDateButton.setClickable(editable);
        enrollmentClearDateButton.setEnabled(editable);
        incidentDatePickerEditText.setEnabled(editable);
        incidentDatePickerEditText.setClickable(editable);
        incidentLabel.setEnabled(editable);
        incidentLabel.setClickable(editable);
        incidentClearDateButton.setClickable(editable);
        incidentClearDateButton.setEnabled(editable);
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        Log.d(TAG, "onRowValueChanged");
        flagDataChanged(true);
        if (mForm == null ) {
            return;
        }

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
        attachEnrollmentDatePicker();
        attachIncidentDatePicker();
        setEditableDatePickerRows(false);
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