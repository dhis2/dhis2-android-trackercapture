package org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs;

import android.content.Context;
import android.util.Log;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.AutoCompleteRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.CheckBoxRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EditTextRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.RadioButtonsRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.utils.api.ValueType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simen S. Russnes on 7/9/15.
 */
public class QueryTrackedEntityInstancesDialogFragmentQuery implements Query<QueryTrackedEntityInstancesDialogFragmentForm>
{
    public static final String TAG = QueryTrackedEntityInstancesDialogFragmentQuery.class.getSimpleName();
    private String orgUnit;
    private String programId;

    public QueryTrackedEntityInstancesDialogFragmentQuery(String orgUnit, String programId)
    {
        this.programId = programId;
        this.orgUnit = orgUnit;
    }

    @Override
    public QueryTrackedEntityInstancesDialogFragmentForm query(Context context)
    {
        QueryTrackedEntityInstancesDialogFragmentForm form = new QueryTrackedEntityInstancesDialogFragmentForm();
        form.setOrganisationUnit(orgUnit);
        form.setProgram(programId);

        Log.d(TAG, orgUnit + programId);

        Program program = MetaDataController.getProgram(programId);
        if(program == null || orgUnit == null) {
            return form;
        }
        List<ProgramTrackedEntityAttribute> programAttrs = program.getProgramTrackedEntityAttributes();
        List<TrackedEntityAttributeValue> values = new ArrayList<>();
        List<TrackedEntityAttribute> listAttributes = new ArrayList<>();
        for(ProgramTrackedEntityAttribute ptea: programAttrs) {
                listAttributes.add(ptea.getTrackedEntityAttribute());

        }
        Log.d(TAG, "rows1: " + listAttributes.size());
        if(listAttributes == null)
            return form;

        List<Row> dataEntryRows = new ArrayList<>();
        for(int i=0;i<listAttributes.size();i++)
        {
            TrackedEntityAttributeValue value = new TrackedEntityAttributeValue();
            value.setTrackedEntityAttributeId(listAttributes.get(i).getUid());
            values.add(value);

            Row row = createDataEntryView(listAttributes.get(i), value);
            dataEntryRows.add(row);
        }
        Log.d(TAG, "rows: " + dataEntryRows.size());
        form.setTrackedEntityAttributeValues(values);
        form.setDataEntryRows(dataEntryRows);
        return form;
    }

    public Row createDataEntryView(TrackedEntityAttribute trackedEntityAttribute, TrackedEntityAttributeValue dataValue) {
        Row row;
        String trackedEntityAttributeName = trackedEntityAttribute.getName();
        if (trackedEntityAttribute.getOptionSet() != null) {
            OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.getOptionSet());
            if (optionSet == null) {
                row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
            } else {
                row = new AutoCompleteRow(trackedEntityAttributeName, dataValue, optionSet);
            }
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.TEXT);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.LONG_TEXT)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.NUMBER)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.NUMBER);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_ZERO_OR_POSITIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_ZERO_OR_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_POSITIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_POSITIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.INTEGER_NEGATIVE)) {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.INTEGER_NEGATIVE);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.BOOLEAN)) {
            row = new RadioButtonsRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.BOOLEAN);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.TRUE_ONLY)) {
            row = new CheckBoxRow(trackedEntityAttributeName, dataValue);
        } else if (trackedEntityAttribute.getValueType().equals(ValueType.DATE)) {
            row = new DatePickerRow(trackedEntityAttributeName, dataValue);
        } else {
            row = new EditTextRow(trackedEntityAttributeName, dataValue, DataEntryRowTypes.LONG_TEXT);
        }
        return row;
    }
}
