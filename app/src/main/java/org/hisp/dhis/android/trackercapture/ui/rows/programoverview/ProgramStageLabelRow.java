package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.utils.ui.views.FloatingActionButton;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */
public class ProgramStageLabelRow implements ProgramStageRow {

    private final ProgramStage programStage;
    private ProgramStageViewHolder holder;
    private View.OnClickListener listener;
    private boolean hasFailed = false;
    private boolean isSynchronized = false;

    public ProgramStageLabelRow(ProgramStage programStage) {
        this.programStage = programStage;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        TextView programStageName;
        FloatingActionButton newEventButton = null;

        if (convertView != null && convertView.getTag() instanceof ProgramStageViewHolder) {
            view = convertView;
            holder = (ProgramStageViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis.android.sdk.R.layout.programstagelayout, container, false);
            programStageName = (TextView) root.findViewById(org.hisp.dhis.android.sdk.R.id.programstagename);
            newEventButton = (FloatingActionButton) root.findViewById(org.hisp.dhis.android.sdk.R.id.neweventbutton);
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
        this.isSynchronized = isSynchronized;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    private static class ProgramStageViewHolder {
        public final TextView programStageName;
        public final FloatingActionButton newEventButton;

        private ProgramStageViewHolder(TextView programStageName,
                                       FloatingActionButton newEventButton) {
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

}
