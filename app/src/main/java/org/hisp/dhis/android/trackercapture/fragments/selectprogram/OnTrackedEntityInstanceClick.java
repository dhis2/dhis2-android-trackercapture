package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemStatus;

/**
 * Created by erling on 5/11/15.
 */
public class OnTrackedEntityInstanceClick
{

        private final TrackedEntityInstance trackedEntityInstance;
        private final TrackedEntityInstanceItemStatus status;
        private final boolean onDescriptionClick;

        public OnTrackedEntityInstanceClick(TrackedEntityInstance trackedEntityInstance, TrackedEntityInstanceItemStatus status,
                            boolean description) {
            this.trackedEntityInstance = trackedEntityInstance;
            this.status = status;
            this.onDescriptionClick = description;
        }

        public TrackedEntityInstance getTrackedEntityInstance() {
            return trackedEntityInstance;
        }

        public boolean isOnDescriptionClick() {
            return onDescriptionClick;
        }

        public TrackedEntityInstanceItemStatus getStatus() {
            return status;
        }
}

