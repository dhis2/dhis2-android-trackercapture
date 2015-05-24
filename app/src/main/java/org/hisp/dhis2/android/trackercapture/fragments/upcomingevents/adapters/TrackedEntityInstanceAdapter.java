package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hisp.dhis2.android.trackercapture.fragments.selectprogram.TrackedEntityInstanceRow;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.EventRowType;

/**
 * Created by erling on 5/11/15.
 */
public class TrackedEntityInstanceAdapter extends AbsAdapter<TrackedEntityInstanceRow>{

    public TrackedEntityInstanceAdapter(LayoutInflater inflater) {
        super(inflater);
    }

    @Override
    public long getItemId(int position) {
        if (getData() != null) {
            return getData().get(position).getId();
        } else {
            return -1;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getData() != null) {
            return getData().get(position).getView(getInflater(), convertView, parent);
        } else {
            return null;
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return getData() != null && getData().get(position).isEnabled();
    }

    @Override
    public int getViewTypeCount() {
        return EventRowType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        if (getData() != null) {
            return getData().get(position).getViewType();
        } else {
            return 0;
        }
    }
}
