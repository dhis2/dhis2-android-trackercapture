package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import android.content.Context;
import android.util.Log;

import org.hisp.dhis.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
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
import java.util.List;

/**
 * Created by erling on 5/19/15.
 */
public class TrackedEntityInstanceProfileFragmentQuery implements Query<TrackedEntityInstanceProfileFragmentForm>
{
    private long mTrackedEntityInstanceId;
    private String mProgramId;
    private TrackedEntityInstance currentTrackedEntityInstance;
    private boolean editable;

    public TrackedEntityInstanceProfileFragmentQuery(long mTrackedEntityInstanceId, String mProgramId)
    {
        this.mTrackedEntityInstanceId = mTrackedEntityInstanceId;
        this.mProgramId = mProgramId;
    }

    @Override
    public TrackedEntityInstanceProfileFragmentForm query(Context context)
    {
        TrackedEntityInstanceProfileFragmentForm mForm = new TrackedEntityInstanceProfileFragmentForm();
        final Program mProgram = MetaDataController.getProgram(mProgramId);
        final TrackedEntityInstance mTrackedEntityInstance = DataValueController.getTrackedEntityInstance(mTrackedEntityInstanceId);

        if(mProgram == null || mTrackedEntityInstance == null)
            return mForm;

        currentTrackedEntityInstance = mTrackedEntityInstance;

        mForm.setProgram(mProgram);
        mForm.setTrackedEntityInstance(mTrackedEntityInstance);

        List<TrackedEntityAttributeValue> values = DataValueController.getProgramTrackedEntityAttributeValues(mProgram,mTrackedEntityInstance);
        List<ProgramTrackedEntityAttribute> attributes = MetaDataController.getProgramTrackedEntityAttributes(mProgramId);

        if(values == null && attributes == null)
            return mForm;

        List<DataEntryRow> dataEntryRows = new ArrayList<>();
        for(int i=0;i<attributes.size();i++)
        {

                DataEntryRow row = createDataEntryView(attributes.get(i).getTrackedEntityAttribute(),
                        getTrackedEntityDataValue(attributes.get(i).getTrackedEntityAttribute().id,
                                values));

                dataEntryRows.add(row);


            Log.d(attributes.get(i).getTrackedEntityAttribute().getName(), values.get(i).getValue());
        }
        mForm.setTrackedEntityAttributeValues(values);
        mForm.setDataEntryRows(dataEntryRows);
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
        trackedEntityAttributeValue.setLocalTrackedEntityInstanceId(currentTrackedEntityInstance.localId);
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
