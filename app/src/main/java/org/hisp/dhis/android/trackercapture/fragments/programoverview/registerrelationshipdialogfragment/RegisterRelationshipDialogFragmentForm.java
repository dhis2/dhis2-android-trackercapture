package org.hisp.dhis.android.trackercapture.fragments.programoverview.registerrelationshipdialogfragment;

import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;

import java.util.List;

/**
 * Created by Simen S. Russnes on 9/7/15.
 */
class RegisterRelationshipDialogFragmentForm
{
    TrackedEntityInstance trackedEntityInstance;
    List<EventRow> rows;
    private String queryString;

    public TrackedEntityInstance getTrackedEntityInstance() {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    public List<EventRow> getRows() {
        return rows;
    }

    public void setRows(List<EventRow> rows) {
        this.rows = rows;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
}
