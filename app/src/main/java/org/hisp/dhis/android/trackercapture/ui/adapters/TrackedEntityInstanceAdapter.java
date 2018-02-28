/*
 *  Copyright (c) 2016, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

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
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceDynamicColumnRows;
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
        if (filter == null) {
            filter = new TrackedEntityInstanceRowFilter();
        }
        return filter;
    }

    private class TrackedEntityInstanceRowFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            Log.d("Filter", constraint.toString());
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            List<EventRow> filteredItems = new ArrayList<>();
            if (constraint.toString().startsWith(Integer.toString(FILTER_SEARCH))) {
                constraint = constraint.subSequence(1,
                        constraint.length()); // remove the filter flag from search string

                if (constraint != null && constraint.toString().length() > 0) {

                    for (int i = 0, l = allRows.size(); i < l; i++) {
                        EventRow row = allRows.get(i);
                        if (row != null && row instanceof TrackedEntityInstanceItemRow) {
                            TrackedEntityInstanceItemRow trackedEntityInstanceItemRow =
                                    (TrackedEntityInstanceItemRow) row;
                            for (String column : trackedEntityInstanceItemRow.getColumns()) {
                                if (column.toLowerCase().contains(constraint)) {
                                    filteredItems.add(row);
                                }
                            }
                        } else {
                            if (row instanceof TrackedEntityInstanceDynamicColumnRows) {
                                filteredItems.add(row);
                            }
                        }
                    }
                } else {
                    synchronized (this) {
                        filteredItems.addAll(allRows);
                    }
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else if (constraint.toString().startsWith(Integer.toString(FILTER_STATUS))) {
                List<EventRow> offlineRows = new ArrayList<>();
                List<EventRow> errorRows = new ArrayList<>();
                List<EventRow> sentRows = new ArrayList<>();

                for (int i = 0, l = allRows.size(); i < l; i++) {
                    EventRow row = allRows.get(i);
                    OnRowClick.ITEM_STATUS status = null;
                    if (row instanceof TrackedEntityInstanceDynamicColumnRows) {
                        filteredItems.add(row);
                    } else if (row instanceof TrackedEntityInstanceItemRow) {
                        status = ((TrackedEntityInstanceItemRow) row).getStatus();
                    }

                    if (status != null) {

                        if (status.toString().equalsIgnoreCase(
                                OnRowClick.ITEM_STATUS.OFFLINE.toString())) {
                            offlineRows.add(row);
                        }
                        if (status.toString().equalsIgnoreCase(
                                OnRowClick.ITEM_STATUS.ERROR.toString())) {
                            errorRows.add(row);
                        }
                        if (status.toString().equalsIgnoreCase(
                                OnRowClick.ITEM_STATUS.SENT.toString())) {
                            sentRows.add(row);
                        }

                    }
                }
                filteredItems.addAll(offlineRows);
                filteredItems.addAll(errorRows);
                filteredItems.addAll(sentRows);
            } else if (constraint.toString().startsWith(Integer.toString(FILTER_FIRST_COLUMN))) {
                filteredItems = filterColumnValues(1); //filter column #1
                setFilteredColumn(1);
            } else if (constraint.toString().startsWith(Integer.toString(FILTER_SECOND_COLUMN))) {
                filteredItems = filterColumnValues(2); //filter column #2
                setFilteredColumn(2);
            } else if (constraint.toString().startsWith(Integer.toString(FILTER_THIRD_COLUMN))) {
                filteredItems = filterColumnValues(3); //filter column #3
                setFilteredColumn(3);
            } else {
                synchronized (this) {
                    result.values = allRows;
                    result.count = allRows.size();
                }
            }

            result.count = filteredItems.size();
            result.values = filteredItems;

            return result;
        }

        public List<EventRow> filterColumnValues(int columnNumber) {
            List<EventRow> filteredItems = new ArrayList<>();
            EventRow headerRow = null;
            String value = null;
            for (int i = 0, l = allRows.size(); i < l; i++) {
                EventRow row = allRows.get(i);
                if (row instanceof TrackedEntityInstanceDynamicColumnRows) {
                    headerRow = row;
                } else if (row instanceof TrackedEntityInstanceItemRow) {
                    filteredItems.add(row);
                }
            }
            Collections.sort(filteredItems, new RowComparator(columnNumber));
            filteredItems.add(0, headerRow); // setting headerRow to first row
            return filteredItems;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (constraint.equals("")) {
                swapData(allRows);
                return;
            }

            if (results.count == 0) {
                Log.d(getClass().getSimpleName(), "results count == 0");
                swapData(new ArrayList<EventRow>());
                return;
            } else {
                filteredRows = (ArrayList<EventRow>) results.values;

                for (EventRow eventRow : filteredRows) {
                    if (eventRow instanceof TrackedEntityInstanceDynamicColumnRows) {
                        TrackedEntityInstanceDynamicColumnRows row =
                                (TrackedEntityInstanceDynamicColumnRows) eventRow;
                        row.setTitle(
                                row.getTrackedEntity() + " (" + (filteredRows.size() - 1) + ")");
                        break;
                    }
                }
                swapData(filteredRows);
            }
        }
    }

    private class RowComparator<T extends EventRow> implements Comparator<T> {

        private final int column;
        DateTime lhsDate = null;
        DateTime rhsDate = null;

        public RowComparator(int column) {
            this.column = column;
        }

        @Override
        public int compare(T lhs, T rhs) {
            if (column
                    == getFilteredColumn()) // if the filteredColumn is the current one, reverse it
            {
                setListIsReversed(true, column);
                return sortDescending(lhs,
                        rhs); // we cannot return sortAscending * (-1) because it is prone to
                // overflow
            } else if (isListIsReversed(column)) // if list is reversed, sort Ascending
            {
                setListIsReversed(false, column);
                return sortAscending(lhs, rhs);
            } else {
                return sortAscending(lhs, rhs);
            }
        }

        private int sortAscending(T lhs, T rhs) {
            TrackedEntityInstanceItemRow left = (TrackedEntityInstanceItemRow) lhs;
            TrackedEntityInstanceItemRow right = (TrackedEntityInstanceItemRow) rhs;
            try {
                lhsDate = new DateTime(
                        ((TrackedEntityInstanceItemRow) lhs).getColumns().get(column));
                rhsDate = new DateTime(
                        ((TrackedEntityInstanceItemRow) rhs).getColumns().get(column));

            } catch (Exception e) {
            }
            if (lhsDate != null && rhsDate != null) {
                return lhsDate.compareTo(rhsDate);
            }
            int compare = 0;
            String leftFirstItem = left.getColumns().get(column);
            String rightFirstItem = right.getColumns().get(column);
            if (leftFirstItem == null) {
                leftFirstItem = "";
            }

            if (rightFirstItem == null) {
                rightFirstItem = "";
            }
            if (rightFirstItem.equalsIgnoreCase(leftFirstItem)) {
                return 0;
            } else {
                compare = rightFirstItem.toLowerCase().compareTo(leftFirstItem.toLowerCase());
            }
            return compare;
        }

        private int sortDescending(T lhs, T rhs) {
            TrackedEntityInstanceItemRow left = (TrackedEntityInstanceItemRow) lhs;
            TrackedEntityInstanceItemRow right = (TrackedEntityInstanceItemRow) rhs;
            try {
                lhsDate = new DateTime(
                        ((TrackedEntityInstanceItemRow) lhs).getColumns().get(column));
                rhsDate = new DateTime(
                        ((TrackedEntityInstanceItemRow) rhs).getColumns().get(column));

            } catch (Exception e) {
            }
            if (lhsDate != null && rhsDate != null) {
                return rhsDate.compareTo(lhsDate);
            }
            int compare = 0;
            String leftFirstItem = left.getColumns().get(column);
            String rightFirstItem = right.getColumns().get(column);
            if (leftFirstItem == null) {
                leftFirstItem = "";
            }

            if (rightFirstItem == null) {
                rightFirstItem = "";
            }

            if (rightFirstItem.equalsIgnoreCase(leftFirstItem)) {
                return 0;
            } else {
                compare = rightFirstItem.toLowerCase().compareTo(leftFirstItem.toLowerCase());
            }
            return compare;
        }
    }
}
