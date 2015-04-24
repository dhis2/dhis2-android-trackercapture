/*
 * Copyright (c) 2015, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.trackercapture.R;
import org.hisp.dhis2.android.trackercapture.fragments.loaders.DbLoader;
import org.hisp.dhis2.android.trackercapture.fragments.loaders.Query;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.adapters.SimpleAdapter;

import java.util.ArrayList;
import java.util.List;


public class OrgUnitDialogFragment extends DialogFragment
        implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<List<OrganisationUnit>> {
    private static final String TAG = OrgUnitDialogFragment.class.getName();
    private static final int LOADER_ID = 1;

    private ListView mListView;
    private ProgressBar mProgressBar;
    private SimpleAdapter<OrganisationUnit> mAdapter;
    private OnOrgUnitSetListener mListener;

    public static OrgUnitDialogFragment newInstance(OnOrgUnitSetListener listener) {
        OrgUnitDialogFragment fragment = new OrgUnitDialogFragment();
        fragment.setListener(listener);
        return fragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE,
                R.style.Theme_AppCompat_Light_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragment_listview, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressBar.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public Loader<List<OrganisationUnit>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(OrganisationUnit.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new OrgUnitQuery()
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<OrganisationUnit>> loader,
                               List<OrganisationUnit> data) {
        if (loader.getId() == LOADER_ID) {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<OrganisationUnit>> loader) {
        mAdapter.swapData(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.INVISIBLE);

        mListView = (ListView) view.findViewById(R.id.simple_listview);
        mListView.setOnItemClickListener(this);

        mAdapter = new SimpleAdapter<>(LayoutInflater.from(getActivity()));
        mAdapter.setStringExtractor(new StringExtractor());
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            OrganisationUnit unit = mAdapter.getItemSafely(position);
            if (unit != null) {
                mListener.onUnitSelected(
                        unit.getId(), unit.getLabel()
                );
            }
        }
        dismiss();
    }

    public void setListener(OnOrgUnitSetListener listener) {
        mListener = listener;
    }

    public void show(FragmentManager manager) {
        show(manager, TAG);
    }


    public interface OnOrgUnitSetListener {
        public void onUnitSelected(String orgUnitId, String orgUnitLabel);
    }

    static class StringExtractor implements SimpleAdapter.ExtractStringCallback<OrganisationUnit> {

        @Override
        public String getString(OrganisationUnit object) {
            return object.getLabel();
        }
    }

    static class OrgUnitQuery implements Query<List<OrganisationUnit>> {

        @Override
        public List<OrganisationUnit> query(Context context) {
            List<OrganisationUnit> orgUnits = queryUnits();
            List<OrganisationUnit> filteredUnits = new ArrayList<>();
            for (OrganisationUnit orgUnit : orgUnits) {
                if (hasPrograms(orgUnit.getId())) {
                    filteredUnits.add(orgUnit);
                }
            }

            return filteredUnits;
        }

        private List<OrganisationUnit> queryUnits() {
            return Dhis2.getInstance()
                    .getMetaDataController()
                    .getAssignedOrganisationUnits();
        }

        private boolean hasPrograms(String unitId) {
            List<Program> programs = Dhis2.getInstance()
                    .getMetaDataController()
                    .getProgramsForOrganisationUnit(
                            unitId, Program.SINGLE_EVENT_WITHOUT_REGISTRATION
                    );
            return (programs != null && !programs.isEmpty());
        }
    }
}