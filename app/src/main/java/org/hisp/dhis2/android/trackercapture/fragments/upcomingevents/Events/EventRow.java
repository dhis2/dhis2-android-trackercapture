package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public interface EventRow {
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container);
    public int getViewType();
    public long getId();
    public boolean isEnabled();
}
