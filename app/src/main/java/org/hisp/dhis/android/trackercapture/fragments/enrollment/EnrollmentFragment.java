/*
 * Copyright (c) 2015, dhis2
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.trackercapture.fragments.enrollment;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.DhisService;
import org.hisp.dhis.android.sdk.ui.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.ValidationErrorDialog;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.hisp.dhis.android.sdk.ui.adapters.DataValueAdapter;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class EnrollmentFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<EnrollmentFragmentForm>,
        OnBackPressedListener, AdapterView.OnItemSelectedListener
{
    public static final String TAG = EnrollmentFragment.class.getSimpleName();

    private static final String EMPTY_FIELD = "";

    private static final int LOADER_ID = 1;

    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    private static final String ORG_UNIT_ID = "extra:orgUnitId";
    private static final String PROGRAM_ID = "extra:ProgramId";
    private static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";



    private ListView mListView;
    private ProgressBar mProgressBar;
    private DataValueAdapter mListViewAdapter;

    private INavigationHandler mNavigationHandler;
    private EnrollmentFragmentForm mForm;

    private View mEnrollmentDatePicker;
    private View mIncidentDatePicker;

    private boolean hasDataChanged = false;

    public static EnrollmentFragment newInstance(String unitId, String programId) {
        EnrollmentFragment fragment = new EnrollmentFragment();
        Bundle args = new Bundle();
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);

        fragment.setArguments(args);
        return fragment;
    }

    public static EnrollmentFragment newInstance(String unitId, String programId, long trackedEntityInstanceId) {
        EnrollmentFragment fragment = new EnrollmentFragment();
        Bundle args = new Bundle();
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);
        args.putLong(TRACKEDENTITYINSTANCE_ID, trackedEntityInstanceId);

        fragment.setArguments(args);
        return fragment;
    }


    private static Map<String, ProgramTrackedEntityAttribute> toMap(List<ProgramTrackedEntityAttribute> attributes) {
        Map<String, ProgramTrackedEntityAttribute> attributeMap = new HashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            for (ProgramTrackedEntityAttribute attribute : attributes) {
                attributeMap.put(attribute.getTrackedEntityAttributeId(), attribute);
            }
        }
        return attributeMap;
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

        GpsController.disableGps();
        mNavigationHandler = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_data_entry, menu);
        Log.d(TAG, "onCreateOptionsMenu");
        MenuItem menuItem = menu.findItem(R.id.action_new_event);
        if(!hasDataChanged) {
            menuItem.setEnabled(false);
            menuItem.getIcon().setAlpha(0x30);
        } else {
            menuItem.setEnabled(true);
            menuItem.getIcon().setAlpha(0xFF);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_entry, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        mListViewAdapter = new DataValueAdapter(getChildFragmentManager(),
                getLayoutInflater(savedInstanceState));
        mListView = (ListView) view.findViewById(R.id.datavalues_listview);
        mListView.setVisibility(View.VISIBLE);

        mEnrollmentDatePicker = LayoutInflater.from(getActivity())
                .inflate(R.layout.fragment_data_entry_date_picker, mListView, false);
        mIncidentDatePicker = LayoutInflater.from(getActivity())
                .inflate(R.layout.fragment_data_entry_date_picker, mListView, false);

        mListView.addHeaderView(mEnrollmentDatePicker);
        mListView.addHeaderView(mIncidentDatePicker);


        mListView.setAdapter(mListViewAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            doBack();
            return true;
        } else if (menuItem.getItemId() == R.id.action_new_event) {
            if(validate()) {
                save();
            }
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);

        mProgressBar.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    long timerStart = -1;

    @Override
    public Loader<EnrollmentFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            timerStart = System.currentTimeMillis();
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String orgUnitId = fragmentArguments.getString(ORG_UNIT_ID);
            String programId = fragmentArguments.getString(PROGRAM_ID);
            long trackedEntityInstance = fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new EnrollmentFragmentQuery(
                    orgUnitId,programId, trackedEntityInstance )
            );
        }
        return null;
    }


    @Override
    public void onLoadFinished(Loader<EnrollmentFragmentForm> loader, EnrollmentFragmentForm data) {
        if (loader.getId() == LOADER_ID && isAdded()) {
            mProgressBar.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);

            mForm = data;

            if (data.getProgram() != null) {
                attachEnrollmentDatePicker();
                attachIncidentDatePicker();
            }

            if(data.getDataEntryRows() != null && !data.getDataEntryRows().isEmpty())
            {
                mListViewAdapter.swapData(data.getDataEntryRows());
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<EnrollmentFragmentForm> loader) {
        if (loader.getId() == LOADER_ID) {
            if (mListViewAdapter != null) {
                mListViewAdapter.swapData(null);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }



    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void doBack() {
        if (haveValuesChanged()) {
            UiUtils.showConfirmDialog(getActivity(),
                    getString(R.string.discard), getString(R.string.discard_confirm_changes),
                    getString(R.string.discard),
                    getString(R.string.save_and_close),
                    getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getFragmentManager().popBackStack();
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (validate()) {
                                save();
                                getFragmentManager().popBackStack();
                            }
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private boolean haveValuesChanged() {
        return hasDataChanged;
    }

    public void flagDataChanged(boolean changed) {
        if(hasDataChanged!=changed) {
            hasDataChanged = changed;
            getActivity().invalidateOptionsMenu();
        }
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        Log.d(TAG, "onRowValueChanged");
        flagDataChanged(true);
        if (mForm == null ) {
            return;
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

    private void attachEnrollmentDatePicker() {
        if (mForm != null && isAdded()) {

            final TextView label = (TextView) mEnrollmentDatePicker
                    .findViewById(R.id.text_label);
            final EditText datePickerEditText = (EditText) mEnrollmentDatePicker
                    .findViewById(R.id.date_picker_edit_text);
            final ImageButton clearDateButton = (ImageButton) mEnrollmentDatePicker
                    .findViewById(R.id.clear_edit_text);

            final DatePickerDialog.OnDateSetListener dateSetListener
                    = new DatePickerDialog.OnDateSetListener() {
                @Override public void onDateSet(DatePicker view, int year,
                                                int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DateUtils.DATE_PATTERN);
                    datePickerEditText.setText(newValue);
                    mForm.getEnrollment().setDateOfEnrollment(newValue);
                    onRowValueChanged(null);
                }
            };
            clearDateButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    datePickerEditText.setText(EMPTY_FIELD);
                    mForm.getEnrollment().setDateOfEnrollment(EMPTY_FIELD);
                }
            });
            datePickerEditText.setOnClickListener(new View.OnClickListener() {
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

            String reportDateDescription = mForm.getProgram().getDateOfEnrollmentDescription()== null ?
                    getString(R.string.report_date) : mForm.getProgram().getDateOfEnrollmentDescription();
            label.setText(reportDateDescription);
            if (mForm.getEnrollment() != null && mForm.getEnrollment().getDateOfEnrollment() != null) {
                DateTime date = DateTime.parse(mForm.getEnrollment().getDateOfEnrollment());
                String newValue = date.toString(DateUtils.DATE_PATTERN);
                datePickerEditText.setText(newValue);
            }

        }
    }

    private void attachIncidentDatePicker() {
        if (mForm != null && isAdded()) {

            final TextView label = (TextView) mIncidentDatePicker
                    .findViewById(R.id.text_label);
            final EditText datePickerEditText = (EditText) mIncidentDatePicker
                    .findViewById(R.id.date_picker_edit_text);
            final ImageButton clearDateButton = (ImageButton) mIncidentDatePicker
                    .findViewById(R.id.clear_edit_text);

            final DatePickerDialog.OnDateSetListener dateSetListener
                    = new DatePickerDialog.OnDateSetListener() {
                @Override public void onDateSet(DatePicker view, int year,
                                                int monthOfYear, int dayOfMonth) {
                    LocalDate date = new LocalDate(year, monthOfYear + 1, dayOfMonth);
                    String newValue = date.toString(DateUtils.DATE_PATTERN);
                    datePickerEditText.setText(newValue);
                    mForm.getEnrollment().setDateOfEnrollment(newValue);
                    onRowValueChanged(null);
                }
            };
            clearDateButton.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    datePickerEditText.setText(EMPTY_FIELD);
                    mForm.getEnrollment().setDateOfEnrollment(EMPTY_FIELD);
                }
            });
            datePickerEditText.setOnClickListener(new View.OnClickListener() {
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

            String reportDateDescription = mForm.getProgram().getDateOfIncidentDescription()== null ?
                    getString(R.string.report_date) : mForm.getProgram().getDateOfIncidentDescription();
            label.setText(reportDateDescription);
            if (mForm.getEnrollment() != null && mForm.getEnrollment().getDateOfIncident() != null) {
                DateTime date = DateTime.parse(mForm.getEnrollment().getDateOfIncident());
                String newValue = date.toString(DateUtils.DATE_PATTERN);
                datePickerEditText.setText(newValue);
            }
        }
    }

    public boolean validate() {
        ArrayList<String> errors = isEnrollmentValid();
        if (!errors.isEmpty()) {
            ValidationErrorDialog dialog = ValidationErrorDialog
                    .newInstance(errors);
            dialog.show(getChildFragmentManager());
            return false;
        } else {
            return true;
        }
    }

    /**
     * returns true if the enrollment was successfully saved
     * @return
     */
    public void save() {
        flagDataChanged(false);
        if(validate())
        {
            new Thread() {
                public void run() {
                    if (mForm != null && isAdded()) {
                        final Context context = getActivity().getBaseContext();

                        if (mForm.getTrackedEntityInstance().getLocalId() < 0) {
                            //mForm.getTrackedEntityInstance().fromServer = true;
                            //mForm.getTrackedEntityInstance().save(true);

                            mForm.getTrackedEntityInstance().setFromServer(false);
                            mForm.getTrackedEntityInstance().save();
                        }

                        mForm.getEnrollment().setLocalTrackedEntityInstanceId(mForm.getTrackedEntityInstance().getLocalId());

                        mForm.getEnrollment().setFromServer(false);
                        mForm.getEnrollment().save();

                        TimerTask timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                DhisService.sendData();
                            }
                        };
                        Timer timer = new Timer();
                        timer.schedule(timerTask, 5000);
                    }
                }
            }.start();
        }
    }

    private ArrayList<String> isEnrollmentValid() {
        ArrayList<String> errors = new ArrayList<>();

        if (mForm.getEnrollment() == null || mForm.getProgram() == null || mForm.getOrganisationUnit() == null) {
            return errors;
        }

        if (isEmpty(mForm.getEnrollment().getDateOfEnrollment())) {
            String dateOfEnrollmentDescription = mForm.getProgram().getDateOfEnrollmentDescription() == null ?
                    getString(R.string.report_date) : mForm.getProgram().getDateOfEnrollmentDescription();
            errors.add(dateOfEnrollmentDescription);
        }

        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(mForm.getProgram().getUid())
        );

        for (TrackedEntityAttributeValue value : mForm.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(value.getTrackedEntityAttributeId());

            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                errors.add(programTrackedEntityAttribute.getTrackedEntityAttribute().getName());
            }
        }
        return errors;
    }
}
