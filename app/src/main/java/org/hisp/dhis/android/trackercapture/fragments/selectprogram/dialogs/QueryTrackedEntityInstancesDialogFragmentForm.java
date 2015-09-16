package org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs;

import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.List;
import java.util.Map;

/**
 * Created by Simen S. Russnes on 9/7/15.
 */
class QueryTrackedEntityInstancesDialogFragmentForm
{
    private String organisationUnit;
    private String program;
    private String queryString;
    private Map<String, DataValue> attributeValues;
    private List<TrackedEntityAttribute> trackedEntityAttributes;
    private List<TrackedEntityAttributeValue> trackedEntityAttributeValues;
    private List<Row> dataEntryRows;

    public String getOrganisationUnit() {
        return organisationUnit;
    }

    public void setOrganisationUnit(String organisationUnit) {
        this.organisationUnit = organisationUnit;
    }

    public Map<String, DataValue> getAttributeValues() {
        return attributeValues;
    }

    public void setAttributeValues(Map<String, DataValue> attributeValues) {
        this.attributeValues = attributeValues;
    }

    public List<TrackedEntityAttribute> getTrackedEntityAttributes() {
        return trackedEntityAttributes;
    }

    public void setTrackedEntityAttributes(List<TrackedEntityAttribute> trackedEntityAttributes) {
        this.trackedEntityAttributes = trackedEntityAttributes;
    }

    public List<Row> getDataEntryRows() {
        return dataEntryRows;
    }

    public void setDataEntryRows(List<Row> dataEntryRows) {
        this.dataEntryRows = dataEntryRows;
    }

    public List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues() {
        return trackedEntityAttributeValues;
    }

    public void setTrackedEntityAttributeValues(List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        this.trackedEntityAttributeValues = trackedEntityAttributeValues;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
}
