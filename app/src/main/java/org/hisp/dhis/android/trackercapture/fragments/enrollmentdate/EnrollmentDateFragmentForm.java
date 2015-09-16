package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.List;

/**
 * Created by erling on 9/15/15.
 */
public class EnrollmentDateFragmentForm
{
    private Enrollment enrollment;
    private List<Row> dataEntryRows;

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public List<Row> getDataEntryRows() {
        return dataEntryRows;
    }

    public void setDataEntryRows(List<Row> dataEntryRows) {
        this.dataEntryRows = dataEntryRows;
    }
}
