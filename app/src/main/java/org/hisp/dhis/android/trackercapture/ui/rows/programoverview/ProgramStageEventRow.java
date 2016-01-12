package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.utils.Utils;
import org.hisp.dhis.android.sdk.utils.support.DateUtils;
import org.joda.time.LocalDate;

public class ProgramStageEventRow implements ProgramStageRow {

    private static final String TAG = ProgramStageEventRow.class.getSimpleName();

    private final Event event;
    private boolean hasFailed = false;
    private boolean isSynchronized = false;
    private EventViewHolder holder;
    private String message;
    private int status;
    private ProgramStageLabelRow labelRow;

    public ProgramStageEventRow(Event event) {
        this.event = event;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        TextView orgUnit;
        TextView eventDateTextView;
        ImageButton statusButton = null;

        if (convertView != null && convertView.getTag() instanceof EventViewHolder) {
            view = convertView;
            holder = (EventViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis.android.sdk.R.layout.eventlayout, container, false);
            orgUnit = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.organisationunit);
            eventDateTextView = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.date);
            statusButton = (ImageButton) root.findViewById(org.hisp.dhis.android.sdk.R.id.statusButton);

            holder = new EventViewHolder(orgUnit, eventDateTextView, statusButton, new OnProgramStageEventInternalClickListener());

            root.findViewById(org.hisp.dhis.android.sdk.R.id.eventbackground).setOnClickListener(holder.listener);

            root.setTag(holder);
            view = root;
        }

        if(holder.statusButton != null) {
            if (hasFailed()) {
                holder.statusButton.setEnabled(true);
                holder.statusButton.setVisibility(View.VISIBLE);
                holder.statusButton.setBackgroundResource(org.hisp.dhis.android.sdk.R.drawable.ic_event_error);
                holder.statusButton.setTag(org.hisp.dhis.android.sdk.R.drawable.ic_event_error);
                holder.listener.setStatusButton(statusButton);
                holder.listener.setStatus(OnRowClick.ITEM_STATUS.ERROR);
                holder.statusButton.setOnClickListener(holder.listener);
            } else if (!isSynchronized()) {
                holder.statusButton.setEnabled(true);
                holder.statusButton.setVisibility(View.VISIBLE);
                holder.statusButton.setBackgroundResource(org.hisp.dhis.android.sdk.R.drawable.ic_offline);
                holder.statusButton.setTag(org.hisp.dhis.android.sdk.R.drawable.ic_offline);
                holder.listener.setStatusButton(statusButton);
                holder.listener.setStatus(OnRowClick.ITEM_STATUS.OFFLINE);
                holder.statusButton.setOnClickListener(holder.listener);
            } else if (isSynchronized()) {
                holder.statusButton.setEnabled(true);
                holder.statusButton.setVisibility(View.VISIBLE);
                holder.statusButton.setBackgroundResource(org.hisp.dhis.android.sdk.R.drawable.ic_from_server);
                holder.statusButton.setTag(org.hisp.dhis.android.sdk.R.drawable.ic_from_server);
                holder.listener.setStatusButton(statusButton);
                holder.listener.setStatus(OnRowClick.ITEM_STATUS.SENT);
                holder.statusButton.setOnClickListener(holder.listener);
            }
        }

        holder.listener.setEvent(getEvent());
        holder.listener.setMessage(getMessage());
        if(event.getOrganisationUnitId()!=null) {
            holder.orgUnit.setText(MetaDataController.getOrganisationUnit(event.getOrganisationUnitId()).getLabel());
        } else {
            holder.orgUnit.setText("");
        }
        String date="";
        if(event.getEventDate()!=null) {
            date = event.getEventDate();
        } else {
            date = event.getDueDate();
        }
        if(date!=null) {
            date = Utils.removeTimeFromDateString(date);
        } else {
            date = "";
        }
        holder.date.setText(date);
        LocalDate eventDate = null;
        if(event.getEventDate() != null) {
            eventDate = new LocalDate(DateUtils.parseDate(event.getEventDate()));
        }
        LocalDate dueDate = new LocalDate(DateUtils.parseDate(event.getDueDate()));
        LocalDate now = new LocalDate(DateUtils.parseDate(DateUtils.getMediumDateString()));
        int color = getStatusColor(eventDate, dueDate, now);

        view.findViewById(org.hisp.dhis.android.sdk.R.id.eventbackground).
                setBackgroundColor(inflater.getContext().getResources().getColor(color));

        return view;
    }

    private int getStatusColor(LocalDate eventDate, LocalDate dueDate, LocalDate now) {
        int color;
        if(event.getStatus().equals(Event.STATUS_COMPLETED)) {
            color = org.hisp.dhis.android.sdk.R.color.stage_completed;
        } else if (event.getStatus().equals(Event.STATUS_SKIPPED)) {
            color = org.hisp.dhis.android.sdk.R.color.stage_skipped;
        } else {
            if(eventDate != null) {
                color = org.hisp.dhis.android.sdk.R.color.stage_executed;
            } else if(dueDate.isBefore(now)) {
                color = org.hisp.dhis.android.sdk.R.color.stage_overdue;
            } else if (true){
                color = org.hisp.dhis.android.sdk.R.color.stage_ontime;
            }
        }
        return color;
    }

    private static class EventViewHolder {
        public final TextView orgUnit;
        public final TextView date;
        public final ImageButton statusButton;
        public final OnProgramStageEventInternalClickListener listener;

        private EventViewHolder(TextView orgUnit,
                                TextView date, ImageButton statusButton, OnProgramStageEventInternalClickListener listener) {
            this.orgUnit = orgUnit;
            this.date = date;
            this.statusButton = statusButton;
            this.listener = listener;
        }
    }

    public Event getEvent() {
        return event;
    }

    public ProgramStageLabelRow getLabelRow() {
        return labelRow;
    }


    public void setLabelRow(ProgramStageLabelRow labelRow) {
        this.labelRow = labelRow;
    }

    @Override
    public boolean hasFailed() {
        return hasFailed;
    }

    @Override
    public void setHasFailed(boolean hasFailed) {
        this.hasFailed = hasFailed;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void setSynchronized(boolean isSynchronized) {
        this.isSynchronized = isSynchronized;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    public void setStatus(int status){
        this.status = status;
    }

    public int getStatus()
    {
        return status;
    }

    public String getMessage() {
        return message;
    }

    private static class OnProgramStageEventInternalClickListener implements View.OnClickListener {
        private Event event;
        private ImageButton statusButton;
        private String message;
        private OnRowClick.ITEM_STATUS status;

        public void setEvent(Event event) {
            this.event = event;
        }

        public void setStatusButton(ImageButton statusButton) {
            this.statusButton = statusButton;
        }

        public void setStatus(OnRowClick.ITEM_STATUS status){
            this.status = status;
        }
        public void setMessage(String message)
        {
            this.message = message;
        }

        @Override
        public void onClick(View view) {
            if(view.getId() == org.hisp.dhis.android.sdk.R.id.eventbackground) {
                Dhis2Application.getEventBus().post(new OnProgramStageEventClick(event, statusButton,false, "", status));
            } else if(view.getId() == org.hisp.dhis.android.sdk.R.id.statusButton) {
                Dhis2Application.getEventBus().post(new OnProgramStageEventClick(event, statusButton, true, message, status));
            }
        }
    }
}
