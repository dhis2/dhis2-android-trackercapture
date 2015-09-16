package org.hisp.dhis.android.trackercapture.fragments.enrollment;

import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IndicatorRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.List;
import java.util.Map;

/**
 * Created by erling on 5/12/15.
 */
class EnrollmentDataEntryFragmentForm
{
    private Enrollment enrollment;
    private Program program;
    private TrackedEntityInstance trackedEntityInstance;
    private OrganisationUnit organisationUnit;
    private Map<String, DataValue> dataValues;
    private Map<String, String> dataElementNames;
    private List<IndicatorRow> indicatorRows;
    private List<Row> dataEntryRows;

    public List<Row> getDataEntryRows() {
        return dataEntryRows;
    }

    public void setDataEntryRows(List<Row> dataEntryRows) {
        this.dataEntryRows = dataEntryRows;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public OrganisationUnit getOrganisationUnit() {
        return organisationUnit;
    }

    public void setOrganisationUnit(OrganisationUnit organisationUnit) {
        this.organisationUnit = organisationUnit;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public Map<String, String> getDataElementNames() {
        return dataElementNames;
    }

    public void setDataElementNames(Map<String, String> dataElementNames) {
        this.dataElementNames = dataElementNames;
    }



    public TrackedEntityInstance getTrackedEntityInstance() {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        this.trackedEntityInstance = trackedEntityInstance;
    }
}
