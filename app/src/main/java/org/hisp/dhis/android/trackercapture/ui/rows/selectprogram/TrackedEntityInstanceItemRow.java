package org.hisp.dhis.android.trackercapture.ui.rows.selectprogram;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.OnTrackedEntityInstanceClick;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

import static org.hisp.dhis.android.sdk.utils.Preconditions.isNull;

/**
 * Created by erling on 5/11/15.
 */
public class TrackedEntityInstanceItemRow implements TrackedEntityInstanceRow
{
    private TrackedEntityInstance mTrackedEntityInstance;
    private String mFirstItem;
    private String mSecondItem;
    private String mThirdItem;
    private TrackedEntityInstanceItemStatus mStatus;

    private Drawable mOfflineDrawable;
    private Drawable mErrorDrawable;
    private Drawable mSentDrawable;

    private String mSent;
    private String mError;
    private String mOffline;

    public TrackedEntityInstanceItemRow(Context context)
    {
        isNull(context, "Context must not be null");

        mOfflineDrawable = context.getResources().getDrawable(R.drawable.ic_offline);
        mErrorDrawable = context.getResources().getDrawable(R.drawable.ic_event_error);
        mSentDrawable = context.getResources().getDrawable(R.drawable.ic_from_server);

        mSent = context.getResources().getString(R.string.event_sent);
        mError = context.getResources().getString(R.string.event_error);
        mOffline = context.getResources().getString(R.string.event_offline);
    }

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container) {
        View view;
        ViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(org.hisp.dhis.android.sdk.R.layout.listview_event_item, container, false);
            holder = new ViewHolder(
                    (TextView) view.findViewById(R.id.first_event_item),
                    (TextView) view.findViewById(R.id.second_event_item),
                    (TextView) view.findViewById(R.id.third_event_item),
                    (ImageView) view.findViewById(R.id.status_image_view),
                    (TextView) view.findViewById(R.id.status_text_view),
                    new OnTrackedEntityInstanceInternalClickListener()
            );
            view.setTag(holder);
            view.setOnClickListener(holder.listener);
            view.findViewById(R.id.status_container)
                    .setOnClickListener(holder.listener);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        holder.listener.setTrackedEntityInstance(mTrackedEntityInstance);
        holder.listener.setStatus(mStatus);
        holder.firstItem.setText(mFirstItem);
        holder.secondItem.setText(mSecondItem);
        holder.thirdItem.setText(mThirdItem);

        switch (mStatus) {
            case OFFLINE: {
                holder.statusImageView.setImageDrawable(mOfflineDrawable);
                holder.statusTextView.setText(mOffline);
                break;
            }
            case ERROR: {
                holder.statusImageView.setImageDrawable(mErrorDrawable);
                holder.statusTextView.setText(mError);
                break;
            }
            case SENT: {
                holder.statusImageView.setImageDrawable(mSentDrawable);
                holder.statusTextView.setText(mSent);
                break;
            }
        }

        return view;
    }

    @Override
    public int getViewType() {
        return EventRowType.EVENT_ITEM_ROW.ordinal();
    }

    @Override
    public long getId() {
        if (mTrackedEntityInstance != null) {
            return mTrackedEntityInstance.getLocalId();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public TrackedEntityInstanceItemRow getItemRow() {
        return this;
    }

    public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        mTrackedEntityInstance = trackedEntityInstance;
    }

    public void setSecondItem(String secondItem) {
        this.mSecondItem = secondItem;
    }

    public void setThirdItem(String thirdItem) {
        this.mThirdItem = thirdItem;
    }

    public void setFirstItem(String firstItem) {
        this.mFirstItem = firstItem;
    }

    public String getmSecondItem() {
        return mSecondItem;
    }

    public String getmThirdItem() {
        return mThirdItem;
    }

    public String getmFirstItem() {
        return mFirstItem;
    }

    public void setStatus(TrackedEntityInstanceItemStatus status) {
        mStatus = status;
    }

    public TrackedEntityInstanceItemStatus getStatus() {
        return mStatus;
    }

    private static class ViewHolder {
        public final TextView firstItem;
        public final TextView secondItem;
        public final TextView thirdItem;
        public final ImageView statusImageView;
        public final TextView statusTextView;
        public final OnTrackedEntityInstanceInternalClickListener listener;

        private ViewHolder(TextView firstItem,
                           TextView secondItem,
                           TextView thirdItem,
                           ImageView statusImageView,
                           TextView statusTextView,
                           OnTrackedEntityInstanceInternalClickListener listener) {
            this.firstItem = firstItem;
            this.secondItem = secondItem;
            this.thirdItem = thirdItem;
            this.statusImageView = statusImageView;
            this.statusTextView = statusTextView;
            this.listener = listener;
        }
    }

    private static class OnTrackedEntityInstanceInternalClickListener implements View.OnClickListener {
        private TrackedEntityInstance trackedEntityInstance;
        private TrackedEntityInstanceItemStatus status;

        public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
            this.trackedEntityInstance = trackedEntityInstance;
        }

        public void setStatus(TrackedEntityInstanceItemStatus status) {
            this.status = status;
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.event_container) {
                Dhis2Application.getEventBus()
                        .post(new OnTrackedEntityInstanceClick(trackedEntityInstance, status, true));
            } else if (view.getId() == R.id.status_container) {
                Dhis2Application.getEventBus()
                        .post(new OnTrackedEntityInstanceClick(trackedEntityInstance, status, false));
            }
        }
    }
}
