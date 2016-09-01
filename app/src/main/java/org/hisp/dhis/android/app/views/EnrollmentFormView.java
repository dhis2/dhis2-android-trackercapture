package org.hisp.dhis.android.app.views;

import android.location.Location;

import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormSection;
import org.hisp.dhis.client.sdk.ui.models.Picker;

import java.util.List;

public interface EnrollmentFormView extends View{
    /**
     * Should be called in cases when ProgramStage
     * does not contain any explicit sections
     */
    void showFormDefaultSection(String formSectionId);

    void showReportDatePicker(String hint, String value);

    void showCoordinatesPicker(String latitude, String longitude);

    void showEnrollmentStatus(Enrollment.EnrollmentStatus enrollmentStatus);

    void setLocation(Location location);

    void setLocationButtonState(boolean enabled);
}
