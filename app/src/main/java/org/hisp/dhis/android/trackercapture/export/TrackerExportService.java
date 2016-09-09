package org.hisp.dhis.android.trackercapture.export;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.export.ExportService;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;

import java.util.List;

/**
 * Created by thomaslindsjorn on 08/09/16.
 */
public class TrackerExportService extends ExportService<TrackerExportResponse> {

    @Override
    public TrackerExportResponse getResponseObject() {
        FlowManager.init(this);
        List<TrackedEntityInstance> trackedEntityInstances = new Select().from(TrackedEntityInstance.class).queryList();
        List<Enrollment> enrollments = new Select().from(Enrollment.class).queryList();
        List<Event> events = new Select().from(Event.class).queryList();
        FlowManager.destroy();
        return TrackerExportResponse.build(trackedEntityInstances, enrollments, events);
    }
}
