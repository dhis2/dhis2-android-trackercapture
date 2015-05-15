package org.hisp.dhis2.android.trackercapture.fragments.programoverview;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.persistence.models.Event;

import static android.text.TextUtils.isEmpty;

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
        holder.date.setText(event.getEventDate());

        int color = org.hisp.dhis2.android.sdk.R.color.stage_skipped;
        switch (event.status) {
            case Event.STATUS_ACTIVE:
                color = org.hisp.dhis2.android.sdk.R.color.stage_executed;
                break;
            case Event.STATUS_COMPLETED:
                color = org.hisp.dhis2.android.sdk.R.color.stage_completed;
                break;
            case Event.STATUS_FUTURE_VISIT:
                color = org.hisp.dhis2.android.sdk.R.color.stage_ontime;
                break;
            case Event.STATUS_LATE_VISIT:
                color = org.hisp.dhis2.android.sdk.R.color.stage_overdue;
                break;
            case Event.STATUS_SKIPPED:
                color = org.hisp.dhis2.android.sdk.R.color.stage_skipped;
                break;
            case Event.STATUS_VISITED:
                color = org.hisp.dhis2.android.sdk.R.color.stage_executed;
                break;
        }
        view.setBackgroundColor(inflater.getContext().getResources().getColor(color));

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
