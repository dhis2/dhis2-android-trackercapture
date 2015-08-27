package org.hisp.dhis.android.trackercapture.ui.adapters;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.AbsAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by erling on 5/11/15.
 */
public class TrackedEntityInstanceAdapter extends AbsAdapter<EventRow> implements Filterable {

    public static final String TAG = TrackedEntityInstanceAdapter.class.getSimpleName();

    private List<EventRow> allRows;
    private List<EventRow> filteredRows;
    private Filter filter;
    public static final int FILTER_SEARCH = 1;
    public static final int FILTER_STATUS = 2;
    public static final int FILTER_DATE = 3;
    public static final int FILTER_ALL_ATTRIBUTES = 4;

    public TrackedEntityInstanceAdapter(LayoutInflater inflater) {
        super(inflater);
    }

    public void setData(List<EventRow> allRows) {
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
            List<EventRow> filteredItems = new ArrayList<>();
            if(constraint.toString().startsWith(Integer.toString(FILTER_SEARCH)))
            {
                constraint = constraint.subSequence(1,constraint.length()); // remove the filter flag from search string

                if (constraint != null && constraint.toString().length() > 0) {

                    for (int i = 0, l = allRows.size(); i < l; i++) {
                        EventRow row = allRows.get(i);
                        if ( row != null && row instanceof TrackedEntityInstanceItemRow ) {

                            if ( ( (TrackedEntityInstanceItemRow) row ).getmFirstItem() != null)
                                if ( ( (TrackedEntityInstanceItemRow) row ).getmFirstItem().toLowerCase().contains(constraint))
                                    filteredItems.add(row);
                                else if ( ( (TrackedEntityInstanceItemRow) row ).getmSecondItem() != null)
                                    if ( ( (TrackedEntityInstanceItemRow) row ).getmSecondItem().toLowerCase().contains(constraint))
                                        filteredItems.add(row);
                                    else if ( ( (TrackedEntityInstanceItemRow) row ).getmThirdItem() != null)
                                        if ( ( (TrackedEntityInstanceItemRow) row ).getmThirdItem().toLowerCase().contains(constraint))
                                            filteredItems.add(row);
                        } else {
                            if (row instanceof TrackedEntityInstanceColumnNamesRow)
                                filteredItems.add(row);
                        }
                    }
                } else {
                    synchronized ( this ) {
                        filteredItems.addAll(allRows);
                    }
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            }
            else if (constraint.toString().startsWith(Integer.toString(FILTER_STATUS)))
            {
                List<EventRow> offlineRows = new ArrayList<>();
                List<EventRow> errorRows = new ArrayList<>();
                List<EventRow> sentRows = new ArrayList<>();

                for (int i = 0, l = allRows.size(); i < l; i++) {
                    EventRow row = allRows.get(i);
                    OnRowClick.ITEM_STATUS status = null;
                    if(row instanceof TrackedEntityInstanceColumnNamesRow)
                        filteredItems.add(row);
                    else if(row instanceof TrackedEntityInstanceItemRow)
                        status =  ( (TrackedEntityInstanceItemRow) row ).getStatus();

                    if(status != null)
                    {

                        if (status.toString().equalsIgnoreCase(OnRowClick.ITEM_STATUS.OFFLINE.toString()))
                            offlineRows.add(row);
                        if(status.toString().equalsIgnoreCase(OnRowClick.ITEM_STATUS.ERROR.toString()))
                            errorRows.add(row);
                        if(status.toString().equalsIgnoreCase(OnRowClick.ITEM_STATUS.SENT.toString()))
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
            else if(constraint.toString().startsWith(Integer.toString(FILTER_ALL_ATTRIBUTES)))
            {
                constraint = constraint.subSequence(1,constraint.length()); // remove the filter flag from search string

                if (constraint != null && constraint.toString().length() > 0) {

                    String prefixString = constraint.toString().toLowerCase();

                    ArrayList<EventRow> values;
                    synchronized (this) {
                        values = new ArrayList<>(allRows);
                    }

                    final int count = values.size();

                    for (int i = 0; i < count; i++) {
                        if( values.get(i) instanceof TrackedEntityInstanceItemRow ) {
                            final TrackedEntityInstance trackedEntityInstanceValue = ((TrackedEntityInstanceItemRow) values.get(i)).getTrackedEntityInstance();
                            for (TrackedEntityAttributeValue attrValue : trackedEntityInstanceValue.getAttributes()) {
                                final String value = attrValue.getValue();
                                final String valueText = value.toLowerCase();

                                // First match against the whole, non-splitted value
                                if (valueText.startsWith(prefixString)) {
                                    filteredItems.add(values.get(i));
                                } else {
                                    final String[] words = valueText.split(" ");
                                    final int wordCount = words.length;

                                    // Start at index 0, in case valueText starts with space(s)
                                    for (int k = 0; k < wordCount; k++) {
                                        if (words[k].startsWith(prefixString)) {
                                            filteredItems.add(values.get(i));
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    synchronized(this) {
                        filteredItems.addAll(allRows);
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
                swapData(new ArrayList<EventRow>());
                return;
            }
            else
            {
                filteredRows = (ArrayList<EventRow>) results.values;
                swapData(filteredRows);
            }
        }
    }
}
