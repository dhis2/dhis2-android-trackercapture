package org.hisp.dhis.android.trackercapture.fragments.search;

import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceColumnNamesRow;

import java.util.List;

public class LocalSearchResultFragmentForm {

    private List<EventRow> eventRowList;
    private String programId;
    private String orgUnitId;
    private TrackedEntityInstanceColumnNamesRow columnNames;

    public LocalSearchResultFragmentForm(){}

    public List<EventRow> getEventRowList() {
        return eventRowList;
    }

    public void setEventRowList(List<EventRow> eventRowList) {
        this.eventRowList = eventRowList;
    }

    public String getOrgUnitId() {
        return orgUnitId;
    }

    public void setOrgUnitId(String orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public TrackedEntityInstanceColumnNamesRow getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(TrackedEntityInstanceColumnNamesRow columnNames) {
        this.columnNames = columnNames;
    }
}
