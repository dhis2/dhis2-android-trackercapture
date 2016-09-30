package org.hisp.dhis.android.trackercapture.fragments.search;

import android.content.Context;
import android.util.Log;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.autocompleterow.AutoCompleteRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.CheckBoxRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EditTextRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.RadioButtonsRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.utils.api.ValueType;

import java.util.ArrayList;
import java.util.List;


public class OnlineSearchFragmentQuery implements Query<OnlineSearchFragmentForm> {
    public static final String TAG = OnlineSearchFragmentQuery.class.getSimpleName();
    private String orgUnit;
    private String programId;

    public OnlineSearchFragmentQuery(String orgUnit, String programId)
    {
        this.programId = programId;
        this.orgUnit = orgUnit;
    }

    @Override
    public OnlineSearchFragmentForm query(Context context)
    {
        OnlineSearchFragmentForm form = new OnlineSearchFragmentForm();
        form.setOrganisationUnit(orgUnit);
        form.setProgram(programId);

        Log.d(TAG, orgUnit + programId);

        Program program = MetaDataController.getProgram(programId);
        if(program == null || orgUnit == null) {
            return form;
        }
        List<ProgramTrackedEntityAttribute> programAttrs = program.getProgramTrackedEntityAttributes();
        List<TrackedEntityAttributeValue> values = new ArrayList<>();
        List<Row> dataEntryRows = new ArrayList<>();
        for(ProgramTrackedEntityAttribute ptea: programAttrs) {
            TrackedEntityAttribute trackedEntityAttribute = ptea.getTrackedEntityAttribute();
            TrackedEntityAttributeValue value = new TrackedEntityAttributeValue();
            value.setTrackedEntityAttributeId(trackedEntityAttribute.getUid());
            values.add(value);

            if(ptea.getMandatory()) {
                ptea.setMandatory(!ptea.getMandatory()); // HACK to skip mandatory fields in search form
            }

            Row row = createDataEntryView(ptea, trackedEntityAttribute, value);
            dataEntryRows.add(row);
        }
        form.setTrackedEntityAttributeValues(values);
        form.setDataEntryRows(dataEntryRows);
        return form;
    }

    public Row createDataEntryView(ProgramTrackedEntityAttribute programTrackedEntityAttribute, TrackedEntityAttribute trackedEntityAttribute, TrackedEntityAttributeValue dataValue) {
        Row row;
        String trackedEntityAttributeName = trackedEntityAttribute.getName();
        if (trackedEntityAttribute.getOptionSet() != null) {
            OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.getOptionSet());
            if (optionSet == null) {
                row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.TEXT);
            } else {
                row = new AutoCompleteRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, optionSet);
            }
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.TEXT);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.LONG_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.NUMBER)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.NUMBER);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.INTEGER);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_ZERO_OR_POSITIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.INTEGER_ZERO_OR_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_POSITIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.INTEGER_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_NEGATIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.INTEGER_NEGATIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.BOOLEAN)) {
            row = new RadioButtonsRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.BOOLEAN);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.TRUE_ONLY)) {
            row = new CheckBoxRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.PHONE_NUMBER)) {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.PHONE_NUMBER);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.DATE)) {
            row = new DatePickerRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, programTrackedEntityAttribute.getAllowFutureDate());
        } else {
            row = new EditTextRow(trackedEntityAttributeName, programTrackedEntityAttribute.getMandatory(), null, dataValue, DataEntryRowTypes.LONG_TEXT);
        }
        return row;
    }

}
