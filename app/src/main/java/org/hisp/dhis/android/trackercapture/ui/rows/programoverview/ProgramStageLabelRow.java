package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */
public class ProgramStageLabelRow implements ProgramStageRow {

    private final ProgramStage programStage;
    private ProgramStageViewHolder holder;
    private View.OnClickListener listener;
    private boolean hasFailed = false;
    private boolean hasCompletedEvents = false;
    private List<ProgramStageEventRow> eventRows = new ArrayList<>();

    public ProgramStageLabelRow(ProgramStage programStage) {
        this.programStage = programStage;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        TextView programStageName;
        Button newEventButton = null;

        if (convertView != null && convertView.getTag() instanceof ProgramStageViewHolder) {
            view = convertView;
            holder = (ProgramStageViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis.android.sdk.R.layout.programstagelayout, container, false);
            programStageName = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.programstagename);
            newEventButton = (Button) root.findViewById(org.hisp.dhis.android.sdk.R.id.neweventbutton);
            newEventButton.setVisibility(View.INVISIBLE);
            newEventButton.setEnabled(false);
            newEventButton.setTag(programStage);

            holder = new ProgramStageViewHolder(programStageName, newEventButton);

            root.setTag(holder);
            view = root;
        }

        if(holder.newEventButton != null && listener != null) {
            holder.newEventButton.setOnClickListener(listener);
            holder.newEventButton.setVisibility(View.VISIBLE);
            holder.newEventButton.setEnabled(true);
        }
        else
        {
            holder.newEventButton.setOnClickListener(null);
            holder.newEventButton.setVisibility(View.INVISIBLE);
            holder.newEventButton.setEnabled(false);
        }
        holder.programStageName.setText(programStage.getName());

        return view;
    }

    @Override
    public boolean hasFailed() {
        return hasFailed;
    }

    @Override
    public void setHasFailed(boolean hasFailed) {
        this.hasFailed = hasFailed;
    }

    @Override
    public void setSynchronized(boolean isSynchronized) {

    }

    @Override
    public boolean isSynchronized() {
        return false;
    }

    private static class ProgramStageViewHolder {
        public final TextView programStageName;
        public final Button newEventButton;

        private ProgramStageViewHolder(TextView programStageName,
                                       Button newEventButton) {
            this.programStageName = programStageName;
            this.newEventButton = newEventButton;
        }
    }

    public void setButtonListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    public ProgramStage getProgramStage() {
        return programStage;
    }


    public boolean getHasCompletedEvents() {
        return hasCompletedEvents;
    }

    public void setHasCompletedEvents(boolean hasCompletedEvents) {
        this.hasCompletedEvents = hasCompletedEvents;
    }

    public List<ProgramStageEventRow> getEventRows() {
        return eventRows;
    }

    public void setEventRows(List<ProgramStageEventRow> eventRows) {
        this.eventRows = eventRows;
    }
}
