package org.hisp.dhis.android.trackercapture.ui.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.hisp.dhis.android.sdk.utils.ui.adapters.AbsAdapter;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemStatus;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceRow;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by erling on 5/11/15.
 */
public class TrackedEntityInstanceAdapter extends AbsAdapter<TrackedEntityInstanceRow> implements Filterable {

    private List<TrackedEntityInstanceRow> allRows;
    private List<TrackedEntityInstanceRow> filteredRows;
    private Activity context;
    private TrackedEntityInstanceRowFilter filter;
    private LayoutInflater layoutInflater;
    public static final int FILTER_SEARCH = 1;
    public static final int FILTER_STATUS = 2;
    public static final int FILTER_DATE = 3;

    public TrackedEntityInstanceAdapter(LayoutInflater inflater) {
        super(inflater);
    }

    public void setData(List<TrackedEntityInstanceRow> allRows) {
        this.allRows = allRows;
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

    @Override
    public Filter getFilter() {
        if(filter == null)
            filter = new TrackedEntityInstanceRowFilter();
        return filter;
    }

    private class TrackedEntityInstanceRowFilter extends Filter
    {


        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            List<TrackedEntityInstanceRow> filteredItems = new ArrayList<TrackedEntityInstanceRow>();

            if(constraint.toString().startsWith(Integer.toString(FILTER_SEARCH)))
            {
                constraint = constraint.subSequence(1,constraint.length()); // remove the filter flag from search string

                if (constraint != null && constraint.toString().length() > 0) {

                    for (int i = 0, l = allRows.size(); i < l; i++) {
                        TrackedEntityInstanceRow row = allRows.get(i);
                        if (row.getItemRow() != null) {

                            if (row.getItemRow().getmFirstItem() != null)
                                if (row.getItemRow().getmFirstItem().toLowerCase().contains(constraint))
                                    filteredItems.add(row);
                                else if (row.getItemRow().getmSecondItem() != null)
                                    if (row.getItemRow().getmSecondItem().toLowerCase().contains(constraint))
                                        filteredItems.add(row);
                                    else if (row.getItemRow().getmThirdItem() != null)
                                        if (row.getItemRow().getmThirdItem().toLowerCase().contains(constraint))
                                            filteredItems.add(row);
                        } else {
                            if (row instanceof TrackedEntityInstanceColumnNamesRow)
                                filteredItems.add(row);
                        }
                    }
                    result.count = filteredItems.size();
                    result.values = filteredItems;
                }
            }
            else if (constraint.toString().startsWith(Integer.toString(FILTER_STATUS)))
            {
                List<TrackedEntityInstanceRow> offlineRows = new ArrayList<>();
                List<TrackedEntityInstanceRow> errorRows = new ArrayList<>();
                List<TrackedEntityInstanceRow> sentRows = new ArrayList<>();

                for (int i = 0, l = allRows.size(); i < l; i++) {
                    TrackedEntityInstanceRow row = allRows.get(i);
                    TrackedEntityInstanceItemStatus status = null;
                    if(row instanceof TrackedEntityInstanceColumnNamesRow)
                        filteredItems.add(row);
                    else if(row instanceof TrackedEntityInstanceItemRow)
                        status = row.getItemRow().getStatus();

                    if(status != null)
                    {

                        if (status.toString().equalsIgnoreCase(TrackedEntityInstanceItemStatus.OFFLINE.toString()))
                            offlineRows.add(row);
                        if(status.toString().equalsIgnoreCase(TrackedEntityInstanceItemStatus.ERROR.toString()))
                            errorRows.add(row);
                        if(status.toString().equalsIgnoreCase(TrackedEntityInstanceItemStatus.SENT.toString()))
                            sentRows.add(row);

                    }
                }
                filteredItems.addAll(offlineRows);
                filteredItems.addAll(errorRows);
                filteredItems.addAll(sentRows);


            }
            else if (constraint.toString().startsWith(Integer.toString(FILTER_DATE)))
            {

            }
            else
            {
                synchronized(this)
                {
                    result.values = allRows;
                    result.count = allRows.size();
                }
            }

            result.count = filteredItems.size();
            result.values = filteredItems;

            return result;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            if(constraint.equals(""))
            {
                swapData(allRows);
                return;
            }

            if(results.count == 0)
            {
                Log.d(getClass().getSimpleName(), "results count == 0");
                swapData(new ArrayList<TrackedEntityInstanceRow>());
                return;
            }
            else
            {
                filteredRows = (ArrayList<TrackedEntityInstanceRow>) results.values;
                swapData(filteredRows);
            }
        }
    }
}
