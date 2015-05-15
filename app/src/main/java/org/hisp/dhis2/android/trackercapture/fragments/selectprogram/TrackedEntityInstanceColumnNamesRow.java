package org.hisp.dhis2.android.trackercapture.fragments.selectprogram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.trackercapture.R;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.EventRowType;

/**
 * Created by erling on 5/8/15.
 */
public class TrackedEntityInstanceColumnNamesRow implements TrackedEntityInstanceRow
{
    private String mFirstItem;
    private String mSecondItem;
    private String mThirdItem;

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(R.layout.listview_column_names_item, container, false);
            holder = new ViewHolder(
                    (TextView) view.findViewById(R.id.first_column_name),
                    (TextView) view.findViewById(R.id.second_column_name)
            );
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

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

    public void setSecondItem(String secondItem) {
        this.mSecondItem = secondItem;
    }

    public void setFirstItem(String firstItem) {
        this.mFirstItem = firstItem;
    }

    private static class ViewHolder {
        public final TextView firstItem;
        public final TextView secondItem;

        private ViewHolder(TextView firstItem,
                           TextView secondItem) {
            this.firstItem = firstItem;
            this.secondItem = secondItem;
        }
    }

}
