package org.hisp.dhis.android.trackercapture.ui.rows.selectprogram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.utils.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

/**
 * Created by erling on 5/8/15.
 */
public class TrackedEntityInstanceColumnNamesRow implements EventRow
{
    private String mFirstItem;
    private String mSecondItem;
    private String mThirdItem;
    private String mTitle;

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(R.layout.listview_column_names_item, container, false);
            holder = new ViewHolder(
                    (TextView) view.findViewById(R.id.tracked_entity_title),
                    (TextView) view.findViewById(R.id.first_column_name),
                    (TextView) view.findViewById(R.id.second_column_name),
                    (TextView) view.findViewById(R.id.third_column_name),
                    (TextView) view.findViewById(R.id.status_column)
            );
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }
        holder.trackedEntityTitle.setText(mTitle);
        holder.firstItem.setText(mFirstItem);
        holder.secondItem.setText(mSecondItem);
        holder.thirdItem.setText(mThirdItem);

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

    public void setSecondItem(String secondItem) {
        this.mSecondItem = secondItem;
    }

    public void setFirstItem(String firstItem) {
        this.mFirstItem = firstItem;
    }

    public void setThirdItem(String mThirdItem) {
        this.mThirdItem = mThirdItem;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }


    private static class ViewHolder {
        public final TextView trackedEntityTitle;
        public final TextView firstItem;
        public final TextView secondItem;
        public final TextView thirdItem;
        public final TextView statusItem;


        private ViewHolder(TextView trackedEntityTitle,
                           TextView firstItem,
                           TextView secondItem,
                           TextView thirdItem,
                           TextView statusItem) {
            this.trackedEntityTitle = trackedEntityTitle;
            this.firstItem = firstItem;
            this.secondItem = secondItem;
            this.thirdItem = thirdItem;
            this.statusItem = statusItem;
        }
    }
}
