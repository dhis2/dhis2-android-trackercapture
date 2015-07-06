package org.hisp.dhis.android.trackercapture.ui.rows.selectprogram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by erling on 5/8/15.
 */
public interface TrackedEntityInstanceRow
{
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container);
    public int getViewType();
    public long getId();
    public boolean isEnabled();
    public TrackedEntityInstanceItemRow getItemRow();
}
