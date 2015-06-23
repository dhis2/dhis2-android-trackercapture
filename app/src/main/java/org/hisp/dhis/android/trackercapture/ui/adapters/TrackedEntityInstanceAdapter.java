package org.hisp.dhis.android.trackercapture.ui.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.hisp.dhis.android.sdk.utils.ui.adapters.AbsAdapter;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceRow;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

import java.util.ArrayList;
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

    public TrackedEntityInstanceAdapter(LayoutInflater inflater, List<TrackedEntityInstanceRow> data) {
        super(inflater);
        this.allRows = data;
        this.filteredRows = data;
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

            if(constraint != null && constraint.toString().length() > 0)
            {
                ArrayList<TrackedEntityInstanceRow> filteredItems = new ArrayList<TrackedEntityInstanceRow>();

                for(int i = 0, l = allRows.size(); i < l; i++) {
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
                    }
                    else
                    {
                        if(row instanceof TrackedEntityInstanceColumnNamesRow)
                            filteredItems.add(row);
                    }
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            }
            else
            {
                synchronized(this)
                {
                    result.values = allRows;
                    result.count = allRows.size();
                }
            }
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
