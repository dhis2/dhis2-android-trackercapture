package org.hisp.dhis.android.trackercapture.fragments.enrollment;

import android.content.Context;

import org.hisp.dhis.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.AutoCompleteRow;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.CheckBoxRow;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.EditTextRow;
import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.dataentry.RadioButtonsRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by erling on 5/12/15.
 */
class EnrollmentFragmentQuery implements Query<EnrollmentFragmentForm>
{
    public static final String CLASS_TAG = EnrollmentFragmentQuery.class.getSimpleName();

    private String mOrgUnitId;
    private String mProgramId;
    private long mTrackedEntityInstanceId;
    private TrackedEntityInstance currentTrackedEntityInstance;
    private Enrollment currentEnrollment;

    EnrollmentFragmentQuery(String mOrgUnitId, String mProgramId, long mTrackedEntityInstanceId)
    {
        this.mOrgUnitId = mOrgUnitId;
        this.mProgramId = mProgramId;
        this.mTrackedEntityInstanceId = mTrackedEntityInstanceId;
    }


    @Override
    public EnrollmentFragmentForm query(Context context) {
        EnrollmentFragmentForm mForm = new EnrollmentFragmentForm();
        final Program mProgram = MetaDataController.getProgram(mProgramId);
        final OrganisationUnit mOrgUnit = MetaDataController.getOrganisationUnit(mOrgUnitId);//Select.byId(OrganisationUnit.class, mOrgUnitId);

        if (mProgram == null) {
            return mForm;
        }
        if (mOrgUnit == null) {
            return mForm;
        }


        if( mTrackedEntityInstanceId < 0 )
        {
            currentTrackedEntityInstance = new TrackedEntityInstance(mProgram, mOrgUnitId);
        }
        else
        {
            currentTrackedEntityInstance = DataValueController.getTrackedEntityInstance(mTrackedEntityInstanceId);
        }

        currentEnrollment = new Enrollment(mOrgUnitId, currentTrackedEntityInstance.trackedEntityInstance, mProgram);

        mForm.setProgram(mProgram);
        mForm.setOrganisationUnit(mOrgUnit);
        mForm.setDataElementNames(new HashMap<String, String>());
        mForm.setDataEntryRows(new ArrayList<DataEntryRow>());
        mForm.setTrackedEntityInstance(currentTrackedEntityInstance);

        List<TrackedEntityAttributeValue> trackedEntityAttributeValues = new ArrayList<>();
        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = new ArrayList<>();
        List<DataEntryRow> dataEntryRows = new ArrayList<>();

        programTrackedEntityAttributes = mProgram.getProgramTrackedEntityAttributes();

        for(ProgramTrackedEntityAttribute ptea: programTrackedEntityAttributes) {
            TrackedEntityAttributeValue value = DataValueController.getTrackedEntityAttributeValue(ptea.trackedEntityAttribute, currentTrackedEntityInstance.trackedEntityInstance);
            if(value!=null)
            {
                trackedEntityAttributeValues.add(value);
            }

        }
        currentEnrollment.setAttributes(trackedEntityAttributeValues);
        for(int i=0;i<programTrackedEntityAttributes.size();i++)
        {
            DataEntryRow row = createDataEntryView(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute(),
                    getTrackedEntityDataValue(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().id, trackedEntityAttributeValues));
            dataEntryRows.add(row);
        }
        mForm.setDataEntryRows(dataEntryRows);
        mForm.setEnrollment(currentEnrollment);


        return mForm;
    }

    public TrackedEntityAttributeValue getTrackedEntityDataValue(String trackedEntityAttribute, List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        for(TrackedEntityAttributeValue trackedEntityAttributeValue: trackedEntityAttributeValues) {
            if(trackedEntityAttributeValue.getTrackedEntityAttributeId().equals(trackedEntityAttribute))
                return trackedEntityAttributeValue;
        }

        //the datavalue didnt exist for some reason. Create a new one.
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setTrackedEntityAttributeId(trackedEntityAttribute);
        trackedEntityAttributeValue.setTrackedEntityInstanceId(currentTrackedEntityInstance.trackedEntityInstance);
        trackedEntityAttributeValue.setValue("");
        trackedEntityAttributeValues.add(trackedEntityAttributeValue);
        return trackedEntityAttributeValue;
    }

    public DataEntryRow createDataEntryView(TrackedEntityAttribute trackedEntityAttribute, TrackedEntityAttributeValue dataValue) {
        DataEntryRow row;
        String trackedEntityAttributeName = trackedEntityAttribute.getName();
        if (trackedEntityAttribute.getOptionSet() != null) {
            OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.getOptionSet());
            if (optionSet == null) {
                row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
            } else {
                row = new AutoCompleteRow(trackedEntityAttributeName, dataValue, optionSet);
            }
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_LONG_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_NUMBER)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.NUMBER);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_ZERO_OR_POSITIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_ZERO_OR_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_POSITIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_NEGATIVE_INT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_NEGATIVE);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_BOOL)) {
            row = new RadioButtonsRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.BOOLEAN);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_TRUE_ONLY)) {
            row = new CheckBoxRow(trackedEntityAttributeName, dataValue);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_DATE)) {
            row = new DatePickerRow(trackedEntityAttributeName, dataValue);
        } else if (trackedEntityAttribute.getValueType().equalsIgnoreCase(DataElement.VALUE_TYPE_STRING)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        }
        return row;
    }
}
