package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.joda.time.DateTime;

/**
 * Interface to be implemented to be used by {@link EnrollmentDateSetterHelper} for triggering
 * enrollment creation
 */
public interface IEnroller {
    void showEnrollmentFragment(TrackedEntityInstance trackedEntityInstance, DateTime enrollmentDate, DateTime incidentDate);
}
