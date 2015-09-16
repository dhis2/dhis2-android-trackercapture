package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.List;

/**
 * Created by erling on 5/18/15.
 */
public class TrackedEntityInstanceProfileFragmentForm
{
    private Enrollment mEnrollment;
    private Program mProgram;
    private TrackedEntityInstance mTrackedEntityInstance;
    private List<Row> mDataEntryRows;
    private List<TrackedEntityAttributeValue> trackedEntityAttributeValues;


    public List<TrackedEntityAttributeValue> getTrackedEntityAttributeValues() {
        return trackedEntityAttributeValues;
    }

    public void setTrackedEntityAttributeValues(List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        this.trackedEntityAttributeValues = trackedEntityAttributeValues;
    }

    public Enrollment getEnrollment() {
        return mEnrollment;
    }

    public void setEnrollment(Enrollment mEnrollment) {
        this.mEnrollment = mEnrollment;
    }

    public Program getProgram() {
        return mProgram;
    }

    public void setProgram(Program mProgram) {
        this.mProgram = mProgram;
    }

    public TrackedEntityInstance getTrackedEntityInstance() {
        return mTrackedEntityInstance;
    }

    public void setTrackedEntityInstance(TrackedEntityInstance mTrackedEntityInstance) {
        this.mTrackedEntityInstance = mTrackedEntityInstance;
    }

    public List<Row> getDataEntryRows() {
        return mDataEntryRows;
    }

    public void setDataEntryRows(List<Row> mDataEntryRows) {
        this.mDataEntryRows = mDataEntryRows;
    }
}
