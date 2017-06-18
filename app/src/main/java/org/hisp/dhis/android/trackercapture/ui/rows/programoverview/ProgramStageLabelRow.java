/*
 *  Copyright (c) 2016, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

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
            holder.newEventButton.setTag(programStage);
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
