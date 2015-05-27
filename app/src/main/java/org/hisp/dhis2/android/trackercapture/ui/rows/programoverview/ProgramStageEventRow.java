package org.hisp.dhis2.android.trackercapture.ui.rows.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.utils.Utils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */
public class ProgramStageEventRow implements ProgramStageRow {

    private final Event event;
    public ProgramStageEventRow(Event event) {
        this.event = event;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        EventViewHolder holder;

        if (convertView != null && convertView.getTag() instanceof EventViewHolder) {
            view = convertView;
            holder = (EventViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis2.android.sdk.R.layout.eventlayout, container, false);
            TextView orgUnit = (TextView) root.findViewById(org.hisp.dhis2.android.sdk.R.id.organisationunit);
            TextView eventDate = (TextView) root.findViewById(org.hisp.dhis2.android.sdk.R.id.date);

            holder = new EventViewHolder(orgUnit, eventDate);

            root.setTag(holder);
            view = root;
        }

        holder.orgUnit.setText(MetaDataController.getOrganisationUnit(event.organisationUnitId).getLabel());
        holder.date.setText(Utils.removeTimeFromDateString(event.getEventDate()));

        int color = org.hisp.dhis2.android.sdk.R.color.stage_skipped;
        if(event.status.equals(Event.STATUS_COMPLETED)) {
            color = org.hisp.dhis2.android.sdk.R.color.stage_completed;
        } else if (event.status.equals(Event.STATUS_SKIPPED)) {
            color = org.hisp.dhis2.android.sdk.R.color.stage_skipped;
        } else if (event.status.equals(Event.STATUS_ACTIVE)) {
            if (new LocalDateTime(event.getDueDate()).isAfter(new LocalDateTime(Utils.getCurrentDate()))) {
                color = org.hisp.dhis2.android.sdk.R.color.stage_executed;
            } else {
                color = org.hisp.dhis2.android.sdk.R.color.stage_overdue;
            }
        } else if (event.status.equals(Event.STATUS_FUTURE_VISIT)) {
            if (new LocalDateTime(event.getDueDate()).isAfter(new LocalDateTime(Utils.getCurrentDate()))) {
                color = org.hisp.dhis2.android.sdk.R.color.stage_ontime;
            } else {
                color = org.hisp.dhis2.android.sdk.R.color.stage_overdue;
            }
        }
        view.findViewById(org.hisp.dhis2.android.sdk.R.id.eventbackground).
                setBackgroundColor(inflater.getContext().getResources().getColor(color));

        return view;
    }

    private static class EventViewHolder {
        public final TextView orgUnit;
        public final TextView date;

        private EventViewHolder(TextView orgUnit,
                           TextView date) {
            this.orgUnit = orgUnit;
            this.date = date;
        }
    }

    public Event getEvent() {
        return event;
    }
}
