package org.hisp.dhis.android.trackercapture.ui.rows.selectprogram;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.OnTrackedEntityColumnClick;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

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
                    (TextView) view.findViewById(R.id.second_column_name),
                    (TextView) view.findViewById(R.id.third_column_name),
                    (TextView) view.findViewById(R.id.status_column),
                    new OnInternalColumnRowClickListener()
            );
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        holder.firstItem.setText(mFirstItem);
        holder.firstItem.setOnClickListener(holder.listener);
        holder.secondItem.setText(mSecondItem);
        holder.secondItem.setOnClickListener(holder.listener);
        holder.thirdItem.setText(mThirdItem);
        holder.thirdItem.setOnClickListener(holder.listener);
        holder.statusItem.setOnClickListener(holder.listener);

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

    @Override
    public TrackedEntityInstanceItemRow getItemRow() {
        return null;
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


    private static class ViewHolder {
        public final TextView firstItem;
        public final TextView secondItem;
        public final TextView thirdItem;
        public final TextView statusItem;
        public final OnInternalColumnRowClickListener listener;


        private ViewHolder(TextView firstItem,
                           TextView secondItem,
                           TextView thirdItem,
                           TextView statusItem,
                           OnInternalColumnRowClickListener listener) {
            this.firstItem = firstItem;
            this.secondItem = secondItem;
            this.thirdItem = thirdItem;
            this.statusItem = statusItem;
            this.listener = listener;
        }
    }

    private static class OnInternalColumnRowClickListener implements View.OnClickListener
    {
        @Override
        public void onClick(View view)
        {
            if(view.getId() == R.id.first_column_name)
                    Dhis2Application.getEventBus().post(new OnTrackedEntityColumnClick(OnTrackedEntityColumnClick.FIRST_COLUMN));

            else if(view.getId() == R.id.second_column_name)
                    Dhis2Application.getEventBus().post(new OnTrackedEntityColumnClick(OnTrackedEntityColumnClick.SECOND_COLUMN));

            else if (view.getId() == R.id.third_column_name)
                    Dhis2Application.getEventBus().post(new OnTrackedEntityColumnClick(OnTrackedEntityColumnClick.THIRD_COLUMN));

            else if(view.getId() == R.id.status_column)
                    Dhis2Application.getEventBus().post(new OnTrackedEntityColumnClick(OnTrackedEntityColumnClick.STATUS_COLUMN));
        }

    }

}
