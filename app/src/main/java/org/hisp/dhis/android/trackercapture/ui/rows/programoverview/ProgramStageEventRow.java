package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.OnProgramStageEventClick;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.utils.Utils;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */

public class ProgramStageEventRow implements ProgramStageRow {

    private final Event event;
    private boolean hasFailed = false;
    private EventViewHolder holder;

    private String errorMessage;

    public ProgramStageEventRow(Event event) {
        this.event = event;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        TextView orgUnit;
        TextView eventDate;
        ImageButton hasFailedButton = null;

        if (convertView != null && convertView.getTag() instanceof EventViewHolder) {
            view = convertView;
            holder = (EventViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis.android.sdk.R.layout.eventlayout, container, false);
            orgUnit = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.organisationunit);
            eventDate = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.date);
            hasFailedButton = (ImageButton) root.findViewById(org.hisp.dhis.android.sdk.R.id.statusButton);

//            hasFailedButton.setVisibility(View.INVISIBLE);
//            hasFailedButton.setEnabled(false);
            holder = new EventViewHolder(orgUnit, eventDate, hasFailedButton, new OnProgramStageEventInternalClickListener());

            root.findViewById(org.hisp.dhis.android.sdk.R.id.eventbackground).setOnClickListener(holder.listener);

            root.setTag(holder);
            view = root;
        }

        if(hasFailed())
        {
            if(holder.hasFailedButton != null)
            {
                holder.hasFailedButton.setEnabled(true);
                holder.hasFailedButton.setVisibility(View.VISIBLE);
                holder.listener.setHasFailedButton(hasFailedButton);
                view.findViewById(org.hisp.dhis.android.sdk.R.id.statusButton)
                        .setOnClickListener(holder.listener);
            }
        }
        else
        {
            if(holder.hasFailedButton != null)
            {
                holder.hasFailedButton.setEnabled(false);
                holder.hasFailedButton.setVisibility(View.INVISIBLE);
                holder.listener.setHasFailedButton(null);
                view.findViewById(org.hisp.dhis.android.sdk.R.id.statusButton).setOnClickListener(null);
            }
        }

        holder.listener.setEvent(getEvent());
        holder.listener.setErrorMessage(getErrorMessage());
        holder.orgUnit.setText(MetaDataController.getOrganisationUnit(event.organisationUnitId).getLabel());
        String date="";
        if(event.getDueDate()!=null) {
            date = event.getDueDate();
        } else {
            date = event.getEventDate();
        }
        if(date!=null) {
            date = Utils.removeTimeFromDateString(date);
        } else {
            date = "";
        }
        holder.date.setText(date);
        LocalDate dueDate = new LocalDate(DateUtils.parseDate(event.getDueDate()));
        LocalDate now = new LocalDate(DateUtils.parseDate(DateUtils.getMediumDateString()));
        int color = org.hisp.dhis.android.sdk.R.color.stage_skipped;
        if(event.status.equals(Event.STATUS_COMPLETED)) {
            color = org.hisp.dhis.android.sdk.R.color.stage_completed;
        } else if (event.status.equals(Event.STATUS_SKIPPED)) {
            color = org.hisp.dhis.android.sdk.R.color.stage_skipped;
        } else if (event.status.equals(Event.STATUS_ACTIVE)) {
            if (now.isBefore(dueDate) || now.isEqual(dueDate)) {
                color = org.hisp.dhis.android.sdk.R.color.stage_executed;
            } else {
                color = org.hisp.dhis.android.sdk.R.color.stage_overdue;
            }
        } else if (event.status.equals(Event.STATUS_FUTURE_VISIT)) {
            if (now.isBefore(dueDate) || now.isEqual(dueDate)) {
                color = org.hisp.dhis.android.sdk.R.color.stage_ontime;
            } else {
                color = org.hisp.dhis.android.sdk.R.color.stage_overdue;
            }
        }
        view.findViewById(org.hisp.dhis.android.sdk.R.id.eventbackground).
                setBackgroundColor(inflater.getContext().getResources().getColor(color));

        return view;
    }



    private static class EventViewHolder {
        public final TextView orgUnit;
        public final TextView date;
        public final ImageButton hasFailedButton;
        public final OnProgramStageEventInternalClickListener listener;

        private EventViewHolder(TextView orgUnit,
                                TextView date, ImageButton hasFailedButton, OnProgramStageEventInternalClickListener listener) {
            this.orgUnit = orgUnit;
            this.date = date;
            this.hasFailedButton = hasFailedButton;
            this.listener = listener;
        }
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public boolean hasFailed() {
        return hasFailed;
    }

    @Override
    public void setHasFailed(boolean hasFailed) {
        this.hasFailed = hasFailed;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static class OnProgramStageEventInternalClickListener implements View.OnClickListener
    {
        private Event event;
        private ImageButton hasFailedButton;
        private String errorMessage;

        public void setEvent(Event event) {
            this.event = event;
        }

        public void setHasFailedButton(ImageButton hasFailedButton) {
            this.hasFailedButton = hasFailedButton;
        }

        public void setErrorMessage(String errorMessage)
        {
            this.errorMessage = errorMessage;
        }

        @Override
        public void onClick(View view)
        {
            if(view.getId() == org.hisp.dhis.android.sdk.R.id.eventbackground)
            {
                Dhis2Application.getEventBus().post(new OnProgramStageEventClick(event, hasFailedButton,false, ""));
            }
            else if(view.getId() == org.hisp.dhis.android.sdk.R.id.statusButton)
            {
                Dhis2Application.getEventBus().post(new OnProgramStageEventClick(event, hasFailedButton,true, errorMessage));

            }
        }
    }
}
