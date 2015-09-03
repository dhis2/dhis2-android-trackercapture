package org.hisp.dhis.android.trackercapture.ui.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.ui.adapters.AbsAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.EventRowType;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by erling on 5/11/15.
 */
public class TrackedEntityInstanceAdapter extends AbsAdapter<EventRow> implements Filterable {

    public static final String TAG = TrackedEntityInstanceAdapter.class.getSimpleName();

    private List<EventRow> allRows;
    private List<EventRow> filteredRows;
    private Filter filter;
    private int filteredColumn;
    private boolean listIsReversed;
    public static final int FILTER_SEARCH = 1;
    public static final int FILTER_STATUS = 2;
    public static final int FILTER_FIRST_COLUMN = 3;
    public static final int FILTER_SECOND_COLUMN = 4;
    public static final int FILTER_THIRD_COLUMN = 5;
    public static final int FILTER_DATE = 6;
    public static final int FILTER_ALL_ATTRIBUTES = 7;

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

    public int getFilteredColumn() {
        return filteredColumn;
    }

    public void setFilteredColumn(int filteredColumn) {
        this.filteredColumn = filteredColumn;
    }

    public boolean isListIsReversed(int column) {
        return listIsReversed;
    }

    public void setListIsReversed(boolean listIsReversed, int column) {
        this.listIsReversed = listIsReversed;
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
            Log.d("Filter", constraint.toString());
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
            else if(constraint.toString().startsWith(Integer.toString(FILTER_FIRST_COLUMN)))
            {
                filteredItems = filterColumnValues(1); //filter column #1
                setFilteredColumn(1);
            }
            else if(constraint.toString().startsWith(Integer.toString(FILTER_SECOND_COLUMN)))
            {
                filteredItems = filterColumnValues(2); //filter column #2
                setFilteredColumn(2);
            }
            else if(constraint.toString().startsWith(Integer.toString(FILTER_THIRD_COLUMN)))
            {
                filteredItems = filterColumnValues(3); //filter column #3
                setFilteredColumn(3);
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

        public List<EventRow> filterColumnValues(int columnNumber)
        {
            List<EventRow> filteredItems = new ArrayList<>();
            EventRow headerRow = null;
            String value = null;
            for (int i = 0, l = allRows.size(); i < l; i++) {
                EventRow row = allRows.get(i);
                if (row instanceof TrackedEntityInstanceColumnNamesRow)
                    headerRow = row;
                else if (row instanceof TrackedEntityInstanceItemRow)
                    filteredItems.add(row);
            }
            Collections.sort(filteredItems, new RowComparator(columnNumber));
            filteredItems.add(0,headerRow); // setting headerRow to first row
            return filteredItems;
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

    private class RowComparator<T extends EventRow> implements Comparator<T>
    {

        private final int column;
        DateTime lhsDate = null;
        DateTime rhsDate = null;

        public RowComparator( int column)
        {
            this.column = column;
        }

        @Override
        public int compare(T lhs, T rhs)
        {
            if(column == getFilteredColumn()) // if the filteredColumn is the current one, reverse it
            {
                setListIsReversed(true, column);
                return sortDescending(lhs,rhs) ; // we cannot return sortAscending * (-1) because it is prone to overflow
            }
            else if(isListIsReversed(column)) // if list is reversed, sort Ascending
            {
                setListIsReversed(false, column);
                return sortAscending(lhs, rhs);
            }
            else
                return sortAscending(lhs,rhs);
        }

        private int sortAscending(T lhs, T rhs)
        {

            if(column == 1 )
            {
                try
                {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmFirstItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmFirstItem());

                }
                catch(Exception e)
                {
                }
                if(lhsDate != null && rhsDate != null)
                {
                    return lhsDate.compareTo(rhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) lhs).getmFirstItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) rhs).getmFirstItem().toLowerCase());
                return compare;
            }
            else if(column == 2)
            {
                try
                {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmSecondItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmSecondItem());

                }
                catch(Exception e)
                {

                }
                if(lhsDate != null && rhsDate != null)
                {
                    return lhsDate.compareTo(rhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) lhs).getmSecondItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) rhs).getmSecondItem().toLowerCase());
                return compare;
            }
            else if(column == 3)
            {
                try
                {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmThirdItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmThirdItem());

                }
                catch(Exception e)
                {

                }
                if(lhsDate != null && rhsDate != null)
                {
                    return lhsDate.compareTo(rhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) lhs).getmThirdItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) rhs).getmThirdItem().toLowerCase());

                return compare;
            }


            return 0;
        }
        private int sortDescending(T lhs, T rhs)
        {

            if(column == 1 )
            {
                try
                {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmFirstItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmFirstItem());

                }
                catch(Exception e)
                {
                }
                if(lhsDate != null && rhsDate != null)
                {
                    return rhsDate.compareTo(lhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) rhs).getmFirstItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) lhs).getmFirstItem().toLowerCase());
                return compare;
            }
            else if(column == 2)
            {
                try
                {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmSecondItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmSecondItem());

                }
                catch(Exception e)
                {

                }
                if(lhsDate != null && rhsDate != null)
                {
                    return rhsDate.compareTo(lhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) rhs).getmSecondItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) lhs).getmSecondItem().toLowerCase());
                return compare;
            }
            else if(column == 3) {
                try {
                    lhsDate = new DateTime(((TrackedEntityInstanceItemRow) lhs).getmThirdItem());
                    rhsDate = new DateTime(((TrackedEntityInstanceItemRow) rhs).getmThirdItem());

                } catch (Exception e) {

                }
                if (lhsDate != null && rhsDate != null) {
                    return rhsDate.compareTo(lhsDate);
                }
                int compare = ((TrackedEntityInstanceItemRow) rhs).getmThirdItem().toLowerCase().compareTo(((TrackedEntityInstanceItemRow) lhs).getmThirdItem().toLowerCase());

                return compare;
            }
            return 0;
        }
    }
}
