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

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.InvalidateEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.DataElement;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStageDataElement;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.sdk.utils.Utils;
import org.hisp.dhis2.android.sdk.utils.ui.rows.AutoCompleteRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.BooleanRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.CheckBoxRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.DatePickerRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.IntegerRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.LongTextRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.NegativeIntegerRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.NumberRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.PosIntegerRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.PosOrZeroIntegerRow;
import org.hisp.dhis2.android.sdk.utils.ui.rows.Row;
import org.hisp.dhis2.android.sdk.utils.ui.rows.TextRow;
import org.hisp.dhis2.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class DataEntryFragment extends Fragment {

    private static final String CLASS_TAG = "DataEntryFragment";

    private OrganisationUnit selectedOrganisationUnit;
    private ProgramStage selectedProgramStage;

    private TrackedEntityInstance currentTrackedEntityInstance; /* can be null */
    private Enrollment currentEnrollment; /* can be null */

    private Button captureCoordinateButton;
    private EditText latitudeEditText;
    private EditText longitudeEditText;

    private Event event;
    private long editingEvent = -1;
    private List<DataValue> dataValues;
    private List<ProgramStageDataElement> programStageDataElements;
    private boolean editing;
    private boolean readOnly;
    private List<DataValue> originalDataValues;
    private ProgressBar progressBar;
    private LayoutInflater inflater;
    private Context context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        final View rootView = inflater.inflate(R.layout.fragment_register_event,
                container, false);
        this.inflater = inflater;
        this.context = getActivity();

        progressBar = (ProgressBar) rootView.findViewById(R.id.register_progress);
        setupUi(rootView);
        return rootView;
    }

    public void setupUi(View rootView) {
        captureCoordinateButton = (Button) rootView.findViewById(R.id.dataentry_getcoordinatesbutton);
        latitudeEditText = (EditText) rootView.findViewById(R.id.dataentry_latitudeedit);
        longitudeEditText = (EditText) rootView.findViewById(R.id.dataentry_longitudeedit);

        if(selectedOrganisationUnit == null || selectedProgramStage == null) return;

        final LinearLayout dataElementContainer = (LinearLayout) rootView.
                findViewById(R.id.dataentry_dataElementContainer);
        new Thread() {
            @Override
            public void run() {
                setupDataEntryForm(dataElementContainer);
            }
        }.start();

    }

    /**
     * returns true if the DataEntryFragment is currently editing an existing event. False if
     * it is creating a new Event.
     * @return
     */
    public boolean isEditing() {
        return editing;
    }

    /**
     * returns true if there have been made changes to an editing event.
     * @return
     */
    public boolean hasEdited() {
        if(originalDataValues==null || dataValues == null) return false;
        for(int i = 0; i<dataValues.size(); i++) {
            if(!originalDataValues.get(i).value.equals(dataValues.get(i).value)) return true;
        }
        return false;
    }

    public void setupDataEntryForm(final LinearLayout dataElementContainer) {
        programStageDataElements = selectedProgramStage.getProgramStageDataElements();

        if(editingEvent < 0) {
            editing = false;
            event = new Event(selectedOrganisationUnit.getId(), Event.STATUS_ACTIVE,
                    selectedProgramStage.program, selectedProgramStage.id,
                    currentTrackedEntityInstance.trackedEntityInstance, currentEnrollment.enrollment);
            dataValues = event.dataValues;
        } else {
            editing = true;
            loadEvent();
        }


        if(!selectedProgramStage.captureCoordinates) {

        } else {
            if(getActivity()==null) return;
            getActivity().runOnUiThread(new Thread() {
                @Override
                public void run() {
                    Dhis2.activateGps(getActivity());
                    if(event.latitude!=null)
                        latitudeEditText.setText(event.latitude+"");
                    if(event.longitude!=null)
                        longitudeEditText.setText(event.longitude+"");
                    captureCoordinateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getCoordinates();
                        }
                    });
                    enableCaptureCoordinates();
                }
            });
        }

        final List<Row> rows = new ArrayList<>();
        for(int i = 0; i<programStageDataElements.size(); i++) {
            Row row = createDataEntryView(programStageDataElements.get(i),
                    getDataValue(programStageDataElements.get(i).dataElement, dataValues));
            if(currentEnrollment!=null && currentEnrollment.status.equals(Enrollment.COMPLETED))
                row.setEditable(false);
            rows.add(row);
        }

        originalDataValues = new ArrayList<>();
        for(DataValue dv: dataValues)
            originalDataValues.add(dv.clone());
        if(getActivity() == null) return;
        getActivity().runOnUiThread(new Thread() {
            final Context context = getActivity();
            @Override
            public void run() {
                if(context == null) return;
                progressBar.setVisibility(View.GONE);
                for(int i = 0; i<rows.size(); i++) {
                    Row row = rows.get(i);
                    View view = row.getView(null);



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
                    if(i==programStageDataElements.size()-1) {
                        TextView textView = row.getEntryView();
                        if(textView!=null) textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    }
                }
            }
        });
    }

    /**
     * Returns the DataValue associated with the given programStageDataElement from a list of DataValues
     * @param dataValues
     * @return
     */
    public DataValue getDataValue(String dataElement, List<DataValue> dataValues) {
        if(dataValues==null) dataValues = new ArrayList<>();
        for(DataValue dataValue: dataValues) {
            if(dataValue.dataElement.equals(dataElement))
                return dataValue;
        }

        //the datavalue didnt exist for some reason. Create a new one.
        DataValue dataValue = new DataValue(event.event, "",
                dataElement, false,
                Dhis2.getInstance().getUsername(getActivity()));
        dataValues.add(dataValue);
        return dataValue;
    }

    public void loadEvent() {
        event = DataValueController.getEvent(editingEvent);
        dataValues = event.getDataValues();
    }

    /**
     * Gets coordinates from the device GPS if possible and stores in the current Event.
     */
    public void getCoordinates() {
        Location location = Dhis2.getLocation(getActivity());
        event.latitude = location.getLatitude();
        event.longitude = location.getLongitude();
        latitudeEditText.setText(""+event.latitude);
        longitudeEditText.setText(""+event.longitude);
    }

    public void enableCaptureCoordinates() {
        longitudeEditText.setVisibility(View.VISIBLE);
        latitudeEditText.setVisibility(View.VISIBLE);
        captureCoordinateButton.setVisibility(View.VISIBLE);
    }

    public Row createDataEntryView(ProgramStageDataElement programStageDataElement, DataValue dataValue) {
        DataElement dataElement = MetaDataController.getDataElement(programStageDataElement.dataElement);
        Row row;
        if (dataElement.getOptionSet() != null) {
            OptionSet optionSet = MetaDataController.getOptionSet(dataElement.optionSet);
            if(optionSet == null)
                row = new TextRow(inflater, dataElement.name, dataValue);
            else
                row = new AutoCompleteRow(inflater, dataElement.name, optionSet, dataValue, context);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_TEXT)) {
            row = new TextRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_LONG_TEXT)) {
            row = new LongTextRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_NUMBER)) {
            row = new NumberRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_INT)) {
            row = new IntegerRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_ZERO_OR_POSITIVE_INT)) {
            row = new PosOrZeroIntegerRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_POSITIVE_INT)) {
            row = new PosIntegerRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_NEGATIVE_INT)) {
            row = new NegativeIntegerRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_BOOL)) {
            row = new BooleanRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_TRUE_ONLY)) {
            row = new CheckBoxRow(inflater, dataElement.name, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_DATE)) {
            row = new DatePickerRow(inflater, dataElement.name, context, dataValue);
        } else if (dataElement.getType().equalsIgnoreCase(DataElement.VALUE_TYPE_STRING)) {
            row = new LongTextRow(inflater, dataElement.name, dataValue);
        } else {
            Log.d(CLASS_TAG, "type is: " + dataElement.getType());
            row = new LongTextRow(inflater, dataElement.name, dataValue);
        }
        return row;
    }

    /**
     * saves the current data values as a registered event.
     */
    public void submit() {
        Log.d(CLASS_TAG, "submit");
        boolean valid = true;
        //go through each data element and check that they are valid
        //i.e. all compulsory are not empty
        for(int i = 0; i<dataValues.size(); i++) {
            ProgramStageDataElement programStageDataElement = programStageDataElements.get(i);
            if( programStageDataElement.isCompulsory() ) {
                DataValue dataValue = dataValues.get(i);
                if(dataValue.value == null || dataValue.value.length() <= 0) {
                    valid = false;
                }
            }
        }

        if(!valid) {
            Dhis2.getInstance().showErrorDialog(getActivity(), "Validation error",
                    "Some compulsory fields are empty, please fill them in");
        } else {
            saveEvent();
            showPreviousFragment();
            InvalidateEvent event = new InvalidateEvent(InvalidateEvent.EventType.event);
            Dhis2Application.bus.post(event);
        }
    }

    public void saveEvent() {
        Log.d(CLASS_TAG, "save event");
        event.fromServer = false;
        event.status = Event.STATUS_ACTIVE;
        event.lastUpdated = Utils.getCurrentTime();
        if(event.eventDate == null) event.eventDate = Utils.getCurrentTime();
        event.save(true);
        Dhis2.sendLocalData(getActivity().getApplicationContext());
    }

    public void showPreviousFragment() {
        Log.d(CLASS_TAG, "showing previous fragment..");
        MessageEvent event = new MessageEvent(BaseEvent.EventType.showPreviousFragment);
        Dhis2Application.bus.post(event);
    }

    public OrganisationUnit getSelectedOrganisationUnit() {
        return selectedOrganisationUnit;
    }

    public void setSelectedOrganisationUnit(OrganisationUnit selectedOrganisationUnit) {
        this.selectedOrganisationUnit = selectedOrganisationUnit;
    }

    public void setSelectedProgramStage(ProgramStage selectedProgramStage) {
        this.selectedProgramStage = selectedProgramStage;
    }

    public void setCurrentTrackedEntityInstance(TrackedEntityInstance currentTrackedEntityInstance) {
        this.currentTrackedEntityInstance = currentTrackedEntityInstance;
    }

    public void setCurrentEnrollment(Enrollment enrollment) {
        this.currentEnrollment = enrollment;
    }

    public void setEditingEvent(long localEventId) {
        this.editingEvent = localEventId;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Dhis2.disableGps();
    }
}
