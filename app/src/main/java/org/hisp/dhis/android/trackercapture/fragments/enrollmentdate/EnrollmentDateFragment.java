package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisService;
import org.hisp.dhis.android.sdk.ui.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by erling on 7/16/15.
 */
public class EnrollmentDateFragment extends Fragment implements OnBackPressedListener
{
    private static final String ENROLLMENT_ID = "extra:EnrollmentId";
    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";
    public static final String TAG = EnrollmentDateFragment.class.getSimpleName();
    private INavigationHandler mNavigationHandler;
    private ListView mListView;
    private boolean edit;
    private boolean editableRows;
    private boolean saving;
    private int LOADER_ID = 5439871;
    private ProgressBar mProgressBar;
    private DataValueAdapter mListViewAdapter;
    private List<DataEntryRow> mForm;
    private Enrollment enrollment;
    private static final String EMPTY_FIELD = "";
    private static final String DATE_FORMAT = "YYYY-MM-dd";
    private View mEnrollmentDatePicker;
    private View mIncidentDatePicker;
    private TextView enrollmentLabel, incidentLabel;
    private EditText enrollmentDatePickerEditText, incidentDatePickerEditText;
    private ImageButton incidentClearDateButton, enrollmentClearDateButton;

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
        setHasOptionsMenu(true);
        editableRows = false;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_entry, container, false); //re-use of layout file
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mForm = new ArrayList<>();
        mProgressBar = (ProgressBar) view.findViewById(org.hisp.dhis.android.sdk.R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mListViewAdapter = new DataValueAdapter(getChildFragmentManager(),
                getLayoutInflater(savedInstanceState));
        mListView = (ListView) view.findViewById(org.hisp.dhis.android.sdk.R.id.datavalues_listview);
        mListView.setVisibility(View.VISIBLE);

        mEnrollmentDatePicker = LayoutInflater.from(getActivity())
                .inflate(org.hisp.dhis.android.sdk.R.layout.fragment_data_entry_date_picker, mListView, false);
        mIncidentDatePicker = LayoutInflater.from(getActivity())
                .inflate(org.hisp.dhis.android.sdk.R.layout.fragment_data_entry_date_picker, mListView, false);

        mListView.addHeaderView(mEnrollmentDatePicker);
        mListView.addHeaderView(mIncidentDatePicker);
        enrollment = TrackerController.getEnrollment(getArguments().getLong(ENROLLMENT_ID));


        attachIncidentDatePicker();
        attachEnrollmentDatePicker();
        mListView.setAdapter(mListViewAdapter);
        setEditableDatePickerRows(false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
            editableRows = !editableRows;
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
                            DhisController.hasUnSynchronizedDatavalues = true;
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
        super.onDetach();
    }

    private ActionBar getActionBar() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            throw new IllegalArgumentException("Fragment should be attached to ActionBarActivity");
        }
    }

    public void flagDataChanged(boolean changed)
    {
        edit = changed;
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
                    enrollment.setFromServer(false);
                    enrollment.save();

                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            DhisService.sendData();
                        }
                    };
                    Timer timer = new Timer();
                    timer.schedule(timerTask, 5000);
                }
                saving = false;
            }

        }.start();

    }

    private void attachEnrollmentDatePicker() {

        if (enrollment != null && isAdded()) {

            enrollmentLabel = (TextView) mEnrollmentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.text_label);
            enrollmentDatePickerEditText = (EditText) mEnrollmentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
            enrollmentClearDateButton = (ImageButton) mEnrollmentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.clear_edit_text);

            final DatePickerDialog.OnDateSetListener dateSetListener
                    = new DatePickerDialog.OnDateSetListener() {
                @Override public void onDateSet(DatePicker view, int year,
                                                int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DATE_FORMAT);
                    enrollmentDatePickerEditText.setText(newValue);
                    enrollment.setDateOfEnrollment(newValue);
                    onRowValueChanged(null);
                }
            };
            enrollmentClearDateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enrollmentDatePickerEditText.setText(EMPTY_FIELD);
                    enrollment.setDateOfEnrollment(EMPTY_FIELD);
                }
            });
            enrollmentDatePickerEditText.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    LocalDate currentDate = new LocalDate();
                    DatePickerDialog picker = new DatePickerDialog(getActivity(),
                            dateSetListener, currentDate.getYear(),
                            currentDate.getMonthOfYear() - 1,
                            currentDate.getDayOfMonth());
                    picker.getDatePicker().setMaxDate(DateTime.now().getMillis());
                    picker.show();
                }
            });

            String reportDateDescription = enrollment.getProgram().getDateOfEnrollmentDescription()== null ?
                    getString(org.hisp.dhis.android.sdk.R.string.report_date) : enrollment.getProgram().getDateOfEnrollmentDescription();
            enrollmentLabel.setText(reportDateDescription);
            if (enrollment != null && enrollment.getDateOfEnrollment() != null) {
                DateTime date = DateTime.parse(enrollment.getDateOfEnrollment());
                String newValue = date.toString(DATE_FORMAT);
                enrollmentDatePickerEditText.setText(newValue);
            }

        }
    }

    private void attachIncidentDatePicker() {
        if (enrollment != null && isAdded()) {

            incidentLabel = (TextView) mIncidentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.text_label);
            incidentDatePickerEditText = (EditText) mIncidentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.date_picker_edit_text);
            incidentClearDateButton = (ImageButton) mIncidentDatePicker
                    .findViewById(org.hisp.dhis.android.sdk.R.id.clear_edit_text);

            final DatePickerDialog.OnDateSetListener dateSetListener
                    = new DatePickerDialog.OnDateSetListener() {
                @Override public void onDateSet(DatePicker view, int year,
                                                int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DATE_FORMAT);
                    incidentDatePickerEditText.setText(newValue);
                    enrollment.setDateOfEnrollment(newValue);
                    onRowValueChanged(null);
                }
            };
            incidentClearDateButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    incidentDatePickerEditText.setText(EMPTY_FIELD);
                    enrollment.setDateOfEnrollment(EMPTY_FIELD);
                }
            });
            incidentDatePickerEditText.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    LocalDate currentDate = new LocalDate();
                    DatePickerDialog picker = new DatePickerDialog(getActivity(),
                            dateSetListener, currentDate.getYear(),
                            currentDate.getMonthOfYear() - 1,
                            currentDate.getDayOfMonth());
                    picker.getDatePicker().setMaxDate(DateTime.now().getMillis());
                    picker.show();
                }
            });

            String reportDateDescription = enrollment.getProgram().getDateOfIncidentDescription()== null ?
                    getString(org.hisp.dhis.android.sdk.R.string.report_date) : enrollment.getProgram().getDateOfIncidentDescription();
            incidentLabel.setText(reportDateDescription);
            if (enrollment.getDateOfIncident() != null) {
                DateTime date = DateTime.parse(enrollment.getDateOfIncident());
                String newValue = date.toString(DATE_FORMAT);
                incidentDatePickerEditText.setText(newValue);
            }
        }
    }

    private void setEditableDatePickerRows(boolean editable)
    {
        //should rather fetch through loader
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
    public void onPause() {
        super.onPause();
        Dhis2Application.getEventBus().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.getEventBus().register(this);
    }
}