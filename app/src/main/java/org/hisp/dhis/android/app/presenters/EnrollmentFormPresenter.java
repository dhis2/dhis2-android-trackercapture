package org.hisp.dhis.android.app.presenters;

import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.Presenter;
import org.joda.time.DateTime;

public interface EnrollmentFormPresenter extends Presenter {
    void createDataEntryForm(String enrollmentUid);

    void saveDateOfEnrollment(String enrollmentUid, DateTime eventDate);

    boolean validateForm(String enrollmentUid);

    void subscribeToLocations();

    void stopLocationUpdates();
}
