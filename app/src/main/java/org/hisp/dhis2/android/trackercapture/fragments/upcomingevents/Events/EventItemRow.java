package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.trackercapture.R;

import static org.hisp.dhis2.android.sdk.utils.Preconditions.isNull;


public final class EventItemRow implements EventRow {
    private Event mEvent;
    private String mFirstItem;
    private String mSecondItem;
    private String mThirdItem;
    private EventItemStatus mStatus;

    private Drawable mOfflineDrawable;
    private Drawable mErrorDrawable;
    private Drawable mSentDrawable;

    private String mSent;
    private String mError;
    private String mOffline;

    public EventItemRow(Context context) {
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
            view = inflater.inflate(R.layout.listview_event_item, container, false);
            holder = new ViewHolder(
                    (TextView) view.findViewById(R.id.first_event_item),
                    (TextView) view.findViewById(R.id.second_event_item),
                    (TextView) view.findViewById(R.id.third_event_item),
                    (ImageView) view.findViewById(R.id.status_image_view),
                    (TextView) view.findViewById(R.id.status_text_view),
                    new OnEventInternalClickListener()
            );
            view.setTag(holder);
            view.setOnClickListener(holder.listener);
            view.findViewById(R.id.status_container)
                    .setOnClickListener(holder.listener);
        } else {
            view = convertView;
            holder = (ViewHolder) view.getTag();
        }

        holder.listener.setEvent(mEvent);
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
        if (mEvent != null) {
            return mEvent.getLocalId();
        } else {
            return 0;
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setEvent(Event event) {
        mEvent = event;
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

    public void setStatus(EventItemStatus status) {
        mStatus = status;
    }

    public EventItemStatus getStatus() {
        return mStatus;
    }

    private static class ViewHolder {
        public final TextView firstItem;
        public final TextView secondItem;
        public final TextView thirdItem;
        public final ImageView statusImageView;
        public final TextView statusTextView;
        public final OnEventInternalClickListener listener;

        private ViewHolder(TextView firstItem,
                           TextView secondItem,
                           TextView thirdItem,
                           ImageView statusImageView,
                           TextView statusTextView,
                           OnEventInternalClickListener listener) {
            this.firstItem = firstItem;
            this.secondItem = secondItem;
            this.thirdItem = thirdItem;
            this.statusImageView = statusImageView;
            this.statusTextView = statusTextView;
            this.listener = listener;
        }
    }

    private static class OnEventInternalClickListener implements View.OnClickListener {
        private Event event;
        private EventItemStatus status;

        public void setEvent(Event event) {
            this.event = event;
        }

        public void setStatus(EventItemStatus status) {
            this.status = status;
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.event_container) {
                Dhis2Application.getEventBus()
                        .post(new OnEventClick(event, status, true));
            } else if (view.getId() == R.id.status_container) {
                Dhis2Application.getEventBus()
                        .post(new OnEventClick(event, status, false));
            }
        }
    }
}
