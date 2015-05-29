package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Simen Skogly Russnes on 13.05.15.
 */
public interface ProgramStageRow {
    public View getView(LayoutInflater inflater, View convertView, ViewGroup container);
    public boolean hasFailed();
    public void setHasFailed(boolean hasFailed);
    public void setSynchronized(boolean isSynchronized);
    public boolean isSynchronized();
}
