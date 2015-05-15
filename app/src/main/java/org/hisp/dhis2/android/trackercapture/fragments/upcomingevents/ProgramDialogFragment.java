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
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.Program$Table;
import org.hisp.dhis2.android.trackercapture.R;
import org.hisp.dhis2.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis2.android.sdk.persistence.loaders.Query;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.adapters.SimpleAdapter;

import java.util.ArrayList;
import java.util.List;

public class ProgramDialogFragment extends DialogFragment
        implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<List<Program>> {
    private static String TAG = ProgramDialogFragment.class.getName();
    private static final int LOADER_ID = 1;

    private ListView mListView;
    private ProgressBar mProgressBar;
    private SimpleAdapter<Program> mAdapter;
    private OnProgramSetListener mListener;

    public static ProgramDialogFragment newInstance(OnProgramSetListener listener,
                                                    String orgUnitId, String... programKinds) {
        ProgramDialogFragment fragment = new ProgramDialogFragment();
        Bundle args = new Bundle();
        args.putString(OrganisationUnit$Table.ID, orgUnitId);
        args.putStringArray(Program$Table.KIND, programKinds);
        fragment.setArguments(args);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressBar.setVisibility(View.VISIBLE);
        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public Loader<List<Program>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            String organisationUnitId = args.getString(OrganisationUnit$Table.ID);
            String[] kinds = args.getStringArray(Program$Table.KIND);
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(Program.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new ProgramQuery(organisationUnitId, kinds)
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Program>> loader, List<Program> data) {
        if (LOADER_ID == loader.getId()) {
            mProgressBar.setVisibility(View.GONE);
            mAdapter.swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Program>> loader) {
        mAdapter.swapData(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            Program program = mAdapter.getItemSafely(position);
            if (program != null) {
                mListener.onProgramSelected(
                        program.getId(), program.getName()
                );
            }
        }
        dismiss();
    }

    public void setListener(OnProgramSetListener listener) {
        mListener = listener;
    }

    public void show(FragmentManager manager) {
        show(manager, TAG);
    }

    public interface OnProgramSetListener {
        public void onProgramSelected(String programId, String programName);
    }

    static class StringExtractor implements SimpleAdapter.ExtractStringCallback<Program> {

        @Override
        public String getString(Program object) {
            return object.getName();
        }
    }

    static class ProgramQuery implements Query<List<Program>> {
        private final String mOrgUnitId;
        private final String[] mKinds;

        public ProgramQuery(String orgUnitId, String[] kinds) {
            mOrgUnitId = orgUnitId;
            mKinds = kinds;
        }

        @Override
        public List<Program> query(Context context) {
            return Dhis2.getInstance()
                    .getMetaDataController()
                    .getProgramsForOrganisationUnit(
                            mOrgUnitId, mKinds
                    );
        }
    }
}
