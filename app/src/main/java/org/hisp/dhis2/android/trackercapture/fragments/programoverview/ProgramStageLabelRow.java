package org.hisp.dhis2.android.trackercapture.fragments.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */
public class ProgramStageLabelRow implements ProgramStageRow {

    private final ProgramStage programStage;
    private ProgramStageViewHolder holder;
    private View.OnClickListener listener;
    public ProgramStageLabelRow(ProgramStage programStage) {
        this.programStage = programStage;
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;

        if (convertView != null && convertView.getTag() instanceof ProgramStageViewHolder) {
            view = convertView;
            holder = (ProgramStageViewHolder) view.getTag();
        } else {
            View root = inflater.inflate(org.hisp.dhis2.android.sdk.R.layout.programstagelayout, container, false);
            TextView programStageName = (TextView) root.findViewById(org.hisp.dhis2.android.sdk.R.id.programstagename);
            Button newEventButton = (Button) root.findViewById(org.hisp.dhis2.android.sdk.R.id.neweventbutton);
            newEventButton.setVisibility(View.INVISIBLE);
            newEventButton.setEnabled(false);
            newEventButton.setTag(programStage);
            if(listener!=null) {
                newEventButton.setOnClickListener(listener);
                newEventButton.setVisibility(View.VISIBLE);
                newEventButton.setEnabled(true);
            }
            holder = new ProgramStageViewHolder(programStageName, newEventButton);

            root.setTag(holder);
            view = root;
        }

        holder.programStageName.setText(programStage.getName());

        return view;
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
}
