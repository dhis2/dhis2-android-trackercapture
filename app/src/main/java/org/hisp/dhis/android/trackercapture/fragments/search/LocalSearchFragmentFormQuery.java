package org.hisp.dhis.android.trackercapture.fragments.search;

import android.content.Context;
import android.util.Log;

import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowFactory;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.utils.api.ValueType;

import java.util.ArrayList;
import java.util.List;

public class LocalSearchFragmentFormQuery implements Query<LocalSearchFragmentForm> {
    private String TAG = this.getClass().getSimpleName();
    private String orgUnitId;
    private String programId;

    public LocalSearchFragmentFormQuery(String orgUnitId, String programId) {
        this.orgUnitId = orgUnitId;
        this.programId = programId;
    }

    @Override
    public LocalSearchFragmentForm query(Context context) {
        LocalSearchFragmentForm form = new LocalSearchFragmentForm();
        form.setOrganisationUnitId(orgUnitId);
        form.setProgram(programId);

        Log.d(TAG, orgUnitId + programId);

        Program program = MetaDataController.getProgram(programId);
        if (program == null || orgUnitId == null) {
            return form;
        }
        List<ProgramTrackedEntityAttribute> programAttrs =
                program.getProgramTrackedEntityAttributes();
        List<TrackedEntityAttributeValue> values = new ArrayList<>();
        List<Row> dataEntryRows = new ArrayList<>();
        for (ProgramTrackedEntityAttribute ptea : programAttrs) {
            TrackedEntityAttribute trackedEntityAttribute = ptea.getTrackedEntityAttribute();
            TrackedEntityAttributeValue value = new TrackedEntityAttributeValue();
            value.setTrackedEntityAttributeId(trackedEntityAttribute.getUid());
            values.add(value);

            if (ptea.getMandatory()) {
                ptea.setMandatory(
                        !ptea.getMandatory()); // HACK to skip mandatory fields in search form
            }
            if (ValueType.COORDINATE.equals(ptea.getTrackedEntityAttribute().getValueType())) {
                GpsController.activateGps(context);
            }
            Row row = DataEntryRowFactory.createDataEntryView(ptea.getMandatory(),
                    ptea.getAllowFutureDate(), trackedEntityAttribute.getOptionSet(),
                    trackedEntityAttribute.getName(), value, trackedEntityAttribute.getValueType(),
                    true, false, program.getDataEntryMethod());
            dataEntryRows.add(row);
        }
        form.setTrackedEntityAttributeValues(values);
        form.setDataEntryRows(dataEntryRows);
        return form;


    }
}
