package org.hisp.dhis.android.trackercapture.export;

import org.hisp.dhis.android.sdk.export.ExportResponse;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;

import java.util.List;

/**
 * Created by thomaslindsjorn on 08/09/16.
 */
public class TrackerExportResponse extends ExportResponse {
    List<TrackedEntityInstance> trackedEntityInstances;
    List<Enrollment> enrollments;
    List<Event> events;

    private TrackerExportResponse(List<TrackedEntityInstance> trackedEntityInstances, List<Enrollment> enrollments, List<Event> events) {
        this.trackedEntityInstances = trackedEntityInstances;
        this.enrollments = enrollments;
        this.events = events;
    }

    public List<TrackedEntityInstance> getTrackedEntityInstances() {
        return trackedEntityInstances;
    }

    public void setTrackedEntityInstances(List<TrackedEntityInstance> trackedEntityInstances) {
        this.trackedEntityInstances = trackedEntityInstances;
    }

    public List<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public static TrackerExportResponse build(List<TrackedEntityInstance> trackedEntityInstances, List<Enrollment> enrollments, List<Event> events) {
        return new TrackerExportResponse(trackedEntityInstances, enrollments, events);
    }
}
