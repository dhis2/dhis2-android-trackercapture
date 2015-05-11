/*
 *  Copyright (c) 2015, University of Oslo
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

package org.hisp.dhis2.android.trackercapture.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.raizlabs.android.dbflow.runtime.FlowContentObserver;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.InvalidateEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.BaseValue;
import org.hisp.dhis2.android.sdk.persistence.models.DataElement;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.Option;
import org.hisp.dhis2.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStageDataElement;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.sdk.utils.Utils;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.AutoCompleteRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.CheckBoxRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.EditTextRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.RadioButtonsRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.Row;
import org.hisp.dhis2.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.text.TextUtils.isEmpty;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class EnrollmentFragment extends Fragment {

    private static final String CLASS_TAG = "DataEntryFragment";

    private OrganisationUnit selectedOrganisationUnit;
    private Program selectedProgram;

    private TrackedEntityInstance currentTrackedEntityInstance;
    private Enrollment currentEnrollment;
    private List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes;
    private List<TrackedEntityAttributeValue> trackedEntityAttributeValues;

    private TrackedEntityAttributeValue dateOfEnrollmentValue;
    private TrackedEntityAttributeValue dateOfIncidentValue;

    private ProgressBar progressBar;
    private LayoutInflater inflater;
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View rootView = inflater.inflate(R.layout.fragment_enrollment,
                container, false);
        this.inflater = inflater;
        this.context = getActivity();

        progressBar = (ProgressBar) rootView.findViewById(R.id.register_progress);
        setupUi(rootView);
        return rootView;
    }

    public void setupUi(View rootView) {

        if(selectedOrganisationUnit == null || selectedProgram == null) return;

        LinearLayout dateContainer = (LinearLayout) rootView.findViewById(R.id.enrollment_dateContainer);
        setupEnrollmentDatesForm(dateContainer);
        final LinearLayout dataElementContainer = (LinearLayout) rootView.
                findViewById(R.id.enrollment_dataElementContainer);
        new Thread() {
            @Override
            public void run() {
                setupDataEntryForm(dataElementContainer);
            }
        }.start();

    }

    public void setupEnrollmentDatesForm(LinearLayout container) {
        String dateOfEnrollmentDescription = selectedProgram.dateOfEnrollmentDescription;
        String dateOfIncidentDescription = selectedProgram.dateOfIncidentDescription;
        dateOfEnrollmentValue = new TrackedEntityAttributeValue();
        dateOfIncidentValue = new TrackedEntityAttributeValue();
        DataEntryRow dateOfEnrollmentRow = new DatePickerRow(dateOfEnrollmentDescription, dateOfEnrollmentValue);
        DataEntryRow dateOfIncidentRow = new DatePickerRow(dateOfIncidentDescription, dateOfIncidentValue);



        Resources r = getActivity().getResources();
        int px = Utils.getDpPx(6, r.getDisplayMetrics());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(px, px, px, 0);
        CardView cardViewDateOfEnrollment = new CardView(context);
        cardViewDateOfEnrollment.setLayoutParams(params);
        //cardViewDateOfEnrollment.addView(dateOfEnrollmentRow.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, container));

        CardView cardViewDateOfIncident = new CardView(context);
        cardViewDateOfIncident.setLayoutParams(params);
        //cardViewDateOfIncident.addView(dateOfIncidentRow.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, container));

        container.addView(cardViewDateOfEnrollment);
        container.addView(cardViewDateOfIncident);
    }

    public void setupDataEntryForm(final LinearLayout dataElementContainer) {
        if(currentTrackedEntityInstance == null) {
            currentTrackedEntityInstance =
                    new TrackedEntityInstance(selectedProgram, selectedOrganisationUnit.getId());
        }
        currentEnrollment = new Enrollment(selectedOrganisationUnit.id, currentTrackedEntityInstance.trackedEntityInstance, selectedProgram);
        programTrackedEntityAttributes = selectedProgram.getProgramTrackedEntityAttributes();
        trackedEntityAttributeValues = new ArrayList<>();
        for(ProgramTrackedEntityAttribute ptea: programTrackedEntityAttributes) {
            TrackedEntityAttributeValue value = DataValueController.getTrackedEntityAttributeValue(ptea.trackedEntityAttribute, currentTrackedEntityInstance.trackedEntityInstance);
            if(value!=null) trackedEntityAttributeValues.add(value);
        }

        final List<DataEntryRow> rows = new ArrayList<>();

        for(int i = 0; i<programTrackedEntityAttributes.size(); i++) {
            DataEntryRow row = createDataEntryView(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute(),
                    getTrackedEntityDataValue(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().id, trackedEntityAttributeValues));
            if(currentEnrollment!=null && currentEnrollment.status.equals(Enrollment.COMPLETED)) {
                //row.setEditable(false);
                //todo: fix
            }
            rows.add(row);
        }

        if(getActivity() == null) return;
        getActivity().runOnUiThread(new Thread() {
            final Context context = getActivity();
            @Override
            public void run() {
                if(context == null) return;
                progressBar.setVisibility(View.GONE);
                for(int i = 0; i<rows.size(); i++) {
                    DataEntryRow row = rows.get(i);
                    View view = row.getView(getFragmentManager(), getActivity().getLayoutInflater(), null, dataElementContainer);

                    CardView cardView = new CardView(context);

                    Resources r = getActivity().getResources();
                    int px = Utils.getDpPx(6, r.getDisplayMetrics());

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(px, px, px, 0);
                    cardView.setLayoutParams(params);
                    cardView.addView(view);
                    dataElementContainer.addView(cardView);

                    //set done button for last element to hide keyboard
                    if(i==programTrackedEntityAttributes.size()-1) {
                        //TextView textView = row.getEntryView();
                        //if(textView!=null) textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    }
                }
            }
        });
    }





    /**
     * Returns the DataValue associated with the given programStageDataElement from a list of DataValues
     * @param trackedEntityAttribute
     * @param trackedEntityAttributeValues
     * @return
     */
    public TrackedEntityAttributeValue getTrackedEntityDataValue(String trackedEntityAttribute, List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        for(TrackedEntityAttributeValue trackedEntityAttributeValue: trackedEntityAttributeValues) {
            if(trackedEntityAttributeValue.trackedEntityAttributeId.equals(trackedEntityAttribute))
                return trackedEntityAttributeValue;
        }

        //the datavalue didnt exist for some reason. Create a new one.
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.trackedEntityAttributeId = trackedEntityAttribute;
        trackedEntityAttributeValue.trackedEntityInstanceId = currentTrackedEntityInstance.trackedEntityInstance;
        trackedEntityAttributeValue.value = "";
        trackedEntityAttributeValues.add(trackedEntityAttributeValue);
        return trackedEntityAttributeValue;
    }

    public DataEntryRow createDataEntryView(TrackedEntityAttribute trackedEntityAttribute, TrackedEntityAttributeValue dataValue) {
        DataEntryRow row;
        String trackedEntityAttributeName = trackedEntityAttribute.getName();

        if (trackedEntityAttribute.getOptionSet() != null) {
            OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.optionSet);
            if (optionSet == null) {
                row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
            } else {
                row = new AutoCompleteRow(trackedEntityAttributeName, dataValue, optionSet);
            }
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_LONG_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_NUMBER)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.NUMBER);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_ZERO_OR_POSITIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_ZERO_OR_POSITIVE);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_POSITIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_POSITIVE);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_NEGATIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_NEGATIVE);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_BOOL)) {
            row = new RadioButtonsRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.BOOLEAN);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_TRUE_ONLY)) {
            row = new CheckBoxRow(trackedEntityAttributeName, dataValue);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_DATE)) {
            row = new DatePickerRow(trackedEntityAttributeName, dataValue);
        } else if (trackedEntityAttribute.valueType.equalsIgnoreCase(DataElement.VALUE_TYPE_STRING)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        }
        return row;
    }

    /**
     * saves the current enrollment with corresponding data
     */
    public void submit() {
        boolean valid = true;
        //go through each data element and check that they are valid
        //i.e. all compulsory are not empty
        currentEnrollment.dateOfEnrollment = dateOfEnrollmentValue.value;
        currentEnrollment.dateOfIncident = dateOfIncidentValue.value;
        if(currentEnrollment.dateOfEnrollment == null
                || currentEnrollment.dateOfEnrollment.isEmpty()
                || currentEnrollment.dateOfIncident == null
                || currentEnrollment.dateOfIncident.isEmpty()) {
            valid = false;
        }

        for(int i = 0; i<trackedEntityAttributeValues.size(); i++) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute =
                    MetaDataController.getProgramTrackedEntityAttribute
                            (trackedEntityAttributeValues.get(i).trackedEntityAttributeId);
            if( programTrackedEntityAttribute.isMandatory() ) {
                TrackedEntityAttributeValue trackedEntityAttributeValue =
                        trackedEntityAttributeValues.get(i);
                if(trackedEntityAttributeValue.value == null || trackedEntityAttributeValue.value.
                        length() <= 0) {
                    valid = false;
                }
            }
        }

        if(!valid) {
            Dhis2.getInstance().showErrorDialog(getActivity(), "Validation error",
                    "Some compulsory fields are empty, please fill them in");
        } else {
            saveData();
            showPreviousFragment();
            InvalidateEvent event = new InvalidateEvent(InvalidateEvent.EventType.enrollment);
            Dhis2Application.bus.post(event);
        }
    }

    public void saveData() {
        saveTrackedEntityInstance();
        saveEnrollment();
    }

    public void saveTrackedEntityInstance() {
        if(!currentTrackedEntityInstance.fromServer)
        currentTrackedEntityInstance.save(true);
    }

    public void saveEnrollment() {
        currentEnrollment.fromServer = false;
        currentEnrollment.localTrackedEntityInstanceId = currentTrackedEntityInstance.localId;
        Log.d(CLASS_TAG, "currentenrollmentlocalteiID: " + currentEnrollment.localTrackedEntityInstanceId);
        currentEnrollment.fromServer = true;
        currentEnrollment.save(true);
        for(TrackedEntityAttributeValue trackedEntityAttributeValue: trackedEntityAttributeValues) {
            Log.e(CLASS_TAG, "saving enrollment " + trackedEntityAttributeValue.
                    trackedEntityInstanceId + ": " + trackedEntityAttributeValue.
                    trackedEntityAttributeId + ": " + trackedEntityAttributeValue.value);
            trackedEntityAttributeValue.localTrackedEntityInstanceId = currentTrackedEntityInstance.localId;
            trackedEntityAttributeValue.save(true);
        }
        currentEnrollment.fromServer = false;
        currentEnrollment.save(true); //workaround to make sure event isnt triggered to be sent to server before its datavalues are stored.
        Dhis2.sendLocalData(getActivity().getApplicationContext());
    }

    public void showPreviousFragment() {
        MessageEvent event = new MessageEvent(BaseEvent.EventType.showPreviousFragment);
        Dhis2Application.bus.post(event);
    }

    public OrganisationUnit getSelectedOrganisationUnit() {
        return selectedOrganisationUnit;
    }

    public void setSelectedOrganisationUnit(OrganisationUnit selectedOrganisationUnit) {
        this.selectedOrganisationUnit = selectedOrganisationUnit;
    }

    public void setSelectedProgram(Program selectedProgram) {
        this.selectedProgram = selectedProgram;
    }

    public void setCurrentTrackedEntityInstance(TrackedEntityInstance currentTrackedEntityInstance) {
        this.currentTrackedEntityInstance = currentTrackedEntityInstance;
    }
}
