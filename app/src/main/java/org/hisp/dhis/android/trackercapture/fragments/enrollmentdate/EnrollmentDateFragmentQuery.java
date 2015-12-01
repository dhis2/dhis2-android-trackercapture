package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.content.Context;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EnrollmentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IncidentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by erling on 9/15/15.
 */
public class EnrollmentDateFragmentQuery implements Query<EnrollmentDateFragmentForm>
{
    private final long enrollmentId;
    public EnrollmentDateFragmentQuery(long enrollmentId)
    {
        this.enrollmentId = enrollmentId;
    }

    @Override
    public EnrollmentDateFragmentForm query(Context context) {
    //should return two enrollmentdatepickerrows when developed (enrollment date & incident date)
        EnrollmentDateFragmentForm fragmentForm = new EnrollmentDateFragmentForm();

        Enrollment enrollment = TrackerController.getEnrollment(enrollmentId);
        if(enrollment == null)
            return fragmentForm;

        List<Row> dataEntryRows = new ArrayList<>();
        dataEntryRows.add(new EnrollmentDatePickerRow(enrollment.getProgram().getDateOfEnrollmentDescription(), enrollment, enrollment.getDateOfEnrollment()));

        if(enrollment.getProgram().getDisplayIncidentDate())
        {
            dataEntryRows.add(new IncidentDatePickerRow(enrollment.getProgram().getDateOfIncidentDescription(),enrollment, enrollment.getDateOfIncident()));
        }

        fragmentForm.setEnrollment(enrollment);
        fragmentForm.setDataEntryRows(dataEntryRows);

        return fragmentForm;
    }
}
