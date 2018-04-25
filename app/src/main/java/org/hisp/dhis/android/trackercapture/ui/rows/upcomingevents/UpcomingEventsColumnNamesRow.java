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

package org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.trackercapture.R;

/**
 * Created by araz on 03.04.2015.
 */
public class UpcomingEventsColumnNamesRow implements EventRow {
    private String mTitle;
    private String mFirstItem;
    private String mSecondItem;

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(R.layout.listview_upcomingevents_column_names_item, container, false);
            holder = new ViewHolder(
                    (TextView) view.findViewById(R.id.title_column_name),
                    (TextView) view.findViewById(R.id.first_column_name),
                    (TextView) view.findViewById(R.id.second_column_name)
            );
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        holder.title.setText(mTitle);
        holder.firstItem.setText(mFirstItem);
        holder.secondItem.setText(mSecondItem);

        return view;
    }

    @Override
    public int getViewType() {
        return EventRowType.COLUMN_NAMES_ROW.ordinal();
    }

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setSecondItem(String secondItem) {
        this.mSecondItem = secondItem;
    }

    public void setFirstItem(String firstItem) {
        this.mFirstItem = firstItem;
    }

    private static class ViewHolder {
        public final TextView title;
        public final TextView firstItem;
        public final TextView secondItem;


        private ViewHolder(TextView title, TextView firstItem,
                           TextView secondItem) {
            this.title = title;
            this.firstItem = firstItem;
            this.secondItem = secondItem;
        }
    }
}
