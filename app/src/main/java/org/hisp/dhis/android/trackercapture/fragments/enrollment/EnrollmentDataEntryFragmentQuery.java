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

package org.hisp.dhis.android.trackercapture.fragments.enrollment;

import android.content.Context;
import android.util.Log;

import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Constant;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeGeneratedValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.models.UserAccount;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowFactory;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.autocompleterow.AutoCompleteRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.CheckBoxRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EditTextRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EnrollmentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IncidentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.RadioButtonsRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RefreshListViewEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.utils.api.ValueType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

class EnrollmentDataEntryFragmentQuery implements Query<EnrollmentDataEntryFragmentForm> {
    public static final String CLASS_TAG = EnrollmentDataEntryFragmentQuery.class.getSimpleName();
    private static String appliedValue;
    private final String mOrgUnitId;
    private final String AUTOIDPREFIX="m-PLAN-";
    private final String mProgramId;
    private final long mTrackedEntityInstanceId;
    private final String enrollmentDate;
    private String incidentDate;

//    private String  DOHID="yeI6AOQjrqg";
//    private String  SETTLEMENTID="iRJYMHN0Rel";

      private String PROJECT_DONOR_TZ="o94ggG6Mhx8";
      private String PROJECT_DONOR_PH="KLSVjftH2xS";
      private String BEN_ID="L2doMQ7OtUB";

    private TrackedEntityInstance currentTrackedEntityInstance;
    private Enrollment currentEnrollment;
    private List<OrganisationUnit> assignedOrganisationUnits;
    private UserAccount userAccounts;



    List<String> fieldsToDisable = new ArrayList<String>(
            Arrays.asList(BEN_ID));
    EnrollmentDataEntryFragmentQuery(String mOrgUnitId, String mProgramId,
                                     long mTrackedEntityInstanceId,
                                     String enrollmentDate, String incidentDate) {
        this.mOrgUnitId = mOrgUnitId;
        this.mProgramId = mProgramId;
        this.mTrackedEntityInstanceId = mTrackedEntityInstanceId;
        this.enrollmentDate = enrollmentDate;
        this.incidentDate = incidentDate;
        appliedValue =null;

    }

    @Override
    public EnrollmentDataEntryFragmentForm query(Context context) {
        EnrollmentDataEntryFragmentForm mForm = new EnrollmentDataEntryFragmentForm();
        final Program mProgram = MetaDataController.getProgram(mProgramId);
        final OrganisationUnit mOrgUnit = MetaDataController.getOrganisationUnit(mOrgUnitId);

        if (mProgram == null || mOrgUnit == null) {
            return mForm;
        }

        if (mTrackedEntityInstanceId < 0) {
            currentTrackedEntityInstance = new TrackedEntityInstance(mProgram, mOrgUnitId);
        } else {
            currentTrackedEntityInstance = TrackerController.getTrackedEntityInstance(mTrackedEntityInstanceId);
        }
        if ("".equals(incidentDate)) {
            incidentDate = null;
        }
        currentEnrollment = new Enrollment(mOrgUnitId, currentTrackedEntityInstance.getTrackedEntityInstance(), mProgram, enrollmentDate, incidentDate);

        mForm.setProgram(mProgram);
        mForm.setOrganisationUnit(mOrgUnit);
        mForm.setDataElementNames(new HashMap<String, String>());
        mForm.setDataEntryRows(new ArrayList<Row>());
        mForm.setTrackedEntityInstance(currentTrackedEntityInstance);
        mForm.setTrackedEntityAttributeValueMap(new HashMap<String, TrackedEntityAttributeValue>());

        List<TrackedEntityAttributeValue> trackedEntityAttributeValues = new ArrayList<>();
        final List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = mProgram.getProgramTrackedEntityAttributes();
        List<Row> dataEntryRows = new ArrayList<>();

//        dataEntryRows.add(new EnrollmentDatePickerRow(currentEnrollment.getProgram().getEnrollmentDateLabel(), currentEnrollment));

//        if (currentEnrollment.getProgram().getDisplayIncidentDate()) {
//            dataEntryRows.add(new IncidentDatePickerRow(currentEnrollment.getProgram().getIncidentDateLabel(), currentEnrollment));
//        }

        for (ProgramTrackedEntityAttribute ptea : programTrackedEntityAttributes) {
            TrackedEntityAttributeValue value = TrackerController.getTrackedEntityAttributeValue(ptea.getTrackedEntityAttributeId(), currentTrackedEntityInstance.getLocalId());
            if (value != null) {
                trackedEntityAttributeValues.add(value);
            } else {
                TrackedEntityAttribute trackedEntityAttribute = MetaDataController.getTrackedEntityAttribute(ptea.getTrackedEntityAttributeId());
                if (trackedEntityAttribute.isGenerated()) {
                    TrackedEntityAttributeGeneratedValue trackedEntityAttributeGeneratedValue =
                            MetaDataController.getTrackedEntityAttributeGeneratedValue(ptea.getTrackedEntityAttribute());

                    if (trackedEntityAttributeGeneratedValue != null) {
                        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
                        trackedEntityAttributeValue.setTrackedEntityAttributeId(ptea.getTrackedEntityAttribute().getUid());
                        trackedEntityAttributeValue.setTrackedEntityInstanceId(currentTrackedEntityInstance.getUid());
                        trackedEntityAttributeValue.setValue(trackedEntityAttributeGeneratedValue.getValue());
                        trackedEntityAttributeValues.add(trackedEntityAttributeValue);
                    } else {
                        mForm.setOutOfTrackedEntityAttributeGeneratedValues(true);
                    }
                }
            }
        }
        currentEnrollment.setAttributes(trackedEntityAttributeValues);
        int paddingForIndex = dataEntryRows.size();
        int project_dtz_RowIndex = 0;//added to manupulate or dynamicly change the row value based on user input for the other
//        int dobRowIndex = -1;//added to manupulate or dynamicly change the row value based on user input for the other
        int benRowIndex = 1;//added to manupulate or dynamicly change the row value based on user input for the other
        for (int i = 0; i < programTrackedEntityAttributes.size(); i++) {
            boolean editable = true;
            boolean shouldNeverBeEdited = false;
            if(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().isGenerated()) {
                editable = false;
                shouldNeverBeEdited = true;
            }
//
            if(fieldsToDisable.contains(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getUid())){
                editable=false;
                shouldNeverBeEdited=true;
            }
            //change ends here

            if(ValueType.COORDINATE.equals(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getValueType())) {
                GpsController.activateGps(context);
            }
            Row row = DataEntryRowFactory.createDataEntryView(programTrackedEntityAttributes.get(i).getMandatory(),
                    programTrackedEntityAttributes.get(i).getAllowFutureDate(), programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getOptionSet(),
                    programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getName(),
                    getTrackedEntityDataValue(programTrackedEntityAttributes.get(i).
                            getTrackedEntityAttribute().getUid(), trackedEntityAttributeValues),
                    programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getValueType(),
                    editable, shouldNeverBeEdited);

            if(programTrackedEntityAttributes.get(i).
                    getTrackedEntityAttribute().getUid().equals(PROJECT_DONOR_TZ)||programTrackedEntityAttributes.get(i).
                    getTrackedEntityAttribute().getUid().equals(PROJECT_DONOR_PH))
            {
                project_dtz_RowIndex = i;
            }
            else if(programTrackedEntityAttributes.get(i).
                    getTrackedEntityAttribute().getUid().equals(BEN_ID))
            {
                benRowIndex = i;
            }

            dataEntryRows.add(row);
        }
        for (TrackedEntityAttributeValue trackedEntityAttributeValue : trackedEntityAttributeValues) {
            mForm.getTrackedEntityAttributeValueMap().put(trackedEntityAttributeValue.getTrackedEntityAttributeId(), trackedEntityAttributeValue);
        }
        mForm.setDataEntryRows(dataEntryRows);
        mForm.setEnrollment(currentEnrollment);


        final AutoCompleteRow project_donor_tz_Row = (AutoCompleteRow) dataEntryRows.get(paddingForIndex+project_dtz_RowIndex);
        final EditTextRow ben_Row = (EditTextRow) dataEntryRows.get(paddingForIndex+benRowIndex);

        final String project_tz_UID =programTrackedEntityAttributes.get(project_dtz_RowIndex).getTrackedEntityAttribute().getUid();

        try{
            Dhis2Application.getEventBus().unregister(new DobAgeSync() {
                @com.squareup.otto.Subscribe
                @Override
                public void eventHandler(RowValueChangedEvent event) {

                }
            });

        }catch (Exception ex){
            ex.printStackTrace();
        }
        Dhis2Application.getEventBus().register(new DobAgeSync(){
            @Override
            @com.squareup.otto.Subscribe
            public void eventHandler(RowValueChangedEvent event){
                // Log.i(" Called ",event.getBaseValue().getValue()+"");
                if(event.getId()!=null && event.getId().equals(project_tz_UID)){
                    Row row = event.getRow();
                    if(appliedValue==null || !appliedValue.equals(project_donor_tz_Row.getValue().getValue()) ){

                            //Log.i(" Called ",row.getValue().getValue());
                            try {
                                List<TrackedEntityInstance> tei_list= MetaDataController.getTrackedEntityInstancesFromLocal();
                                int count=tei_list.size();
                                String seq_count = String.format ("%05d", count+1);
                                int year = Calendar.getInstance().get(Calendar.YEAR);
                                String year_=String.valueOf(year);
                                String nimhans_="-"+year_.toString()+"-"+seq_count;
                                project_donor_tz_Row.getValue();
                                ben_Row.getValue().setValue(AUTOIDPREFIX+project_donor_tz_Row.getmSelectedOptionName()+nimhans_);
                                EnrollmentDataEntryFragment.refreshListView();
                            } catch (Exception ex) {
                                Log.i("Exception ", "Converting to integer not possible");

                            }

                    }

                }

            }

        });
        return mForm;
    }


    public TrackedEntityAttributeValue getTrackedEntityDataValue(String trackedEntityAttribute, List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        for (TrackedEntityAttributeValue trackedEntityAttributeValue : trackedEntityAttributeValues) {
            if (trackedEntityAttributeValue.getTrackedEntityAttributeId().equals(trackedEntityAttribute))
                return trackedEntityAttributeValue;
        }

        //for Doh id:

        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();

        trackedEntityAttributeValue.setTrackedEntityAttributeId(trackedEntityAttribute);
        //the datavalue didnt exist for some reason. Create a new one.
        trackedEntityAttributeValue.setTrackedEntityAttributeId(trackedEntityAttribute);
        trackedEntityAttributeValue.setTrackedEntityInstanceId(currentTrackedEntityInstance.getTrackedEntityInstance());
        trackedEntityAttributeValue.setValue("");
        trackedEntityAttributeValues.add(trackedEntityAttributeValue);
        return trackedEntityAttributeValue;
    }

    abstract class DobAgeSync {

        public abstract void eventHandler(RowValueChangedEvent event);

        public boolean equals(Object obj){
            if(obj==null) return false;
            if(obj instanceof DobAgeSync)
                return true;
            else
                return false;
        }

        @Override
        public int hashCode() {
            return 143;
        }
    }

}
