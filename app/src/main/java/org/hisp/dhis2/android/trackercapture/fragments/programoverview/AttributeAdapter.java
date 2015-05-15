/*
 * Copyright (c) 2015, dhis2
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis2.android.trackercapture.fragments.programoverview;

import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import org.hisp.dhis2.android.sdk.persistence.models.BaseValue;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.AbsAdapter;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis2.android.sdk.utils.ui.adapters.rows.dataentry.DataEntryRowTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter to show list of attribute name and attribute value horizontally
 */
public final class AttributeAdapter extends AbsAdapter<AttributeRow> {

    private static final String CLASS_TAG = AttributeAdapter.class.getSimpleName();

    private Map<String, Integer> dataElementsToRowIndexMap;
    private final FragmentManager mFragmentManager;

    public AttributeAdapter(FragmentManager fragmentManager,
                            LayoutInflater inflater) {
        super(inflater);
        mFragmentManager = fragmentManager;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getData() != null) {
            AttributeRow attributeRow = getData().get(position);
            View view = attributeRow.getView(mFragmentManager, getInflater(), convertView, parent);
            view.setVisibility(View.VISIBLE); //in case recycling invisible view
            view.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                    AbsListView.LayoutParams.WRAP_CONTENT));
            view.setId(position);
            return view;
        } else {
            return null;
        }
    }

    @Override
    public int getViewTypeCount() {
        return DataEntryRowTypes.values().length;
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
    public void swapData(List<AttributeRow> data) {
        boolean notifyAdapter = mData != data;
        mData = data;
        if (dataElementsToRowIndexMap == null)
            dataElementsToRowIndexMap = new HashMap<>();
        else {
            dataElementsToRowIndexMap.clear();
        }
        if (mData != null) {
            for (int i = 0; i < mData.size(); i++) {
                AttributeRow attributeRow = mData.get(i);
                BaseValue baseValue = attributeRow.getBaseValue();
                if (baseValue instanceof DataValue) {
                    dataElementsToRowIndexMap.put(((DataValue) baseValue).dataElement, i);
                }
            }
        }

        if (notifyAdapter) {
            notifyDataSetChanged();
        }
    }
}
