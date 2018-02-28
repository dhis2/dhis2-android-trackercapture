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

package org.hisp.dhis.android.trackercapture.fragments.programoverview.registerrelationshipdialogfragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import com.raizlabs.android.dbflow.structure.Model;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.RelationshipType;
import org.hisp.dhis.android.sdk.ui.adapters.SimpleAdapter;
import org.hisp.dhis.android.sdk.ui.dialogs.AutoCompleteDialogAdapter;
import org.hisp.dhis.android.sdk.ui.dialogs.AutoCompleteDialogFragment;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;

public class RelationshipTypesDialogFragment extends AutoCompleteDialogFragment
        implements LoaderManager.LoaderCallbacks<List<AutoCompleteDialogAdapter.OptionAdapterValue>> {
    public static final int ID = 921345130;
    private static final int LOADER_ID = 1;

    public static RelationshipTypesDialogFragment newInstance(OnOptionSelectedListener listener) {
        RelationshipTypesDialogFragment fragment = new RelationshipTypesDialogFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.setOnOptionSetListener(listener);
        return fragment;
    }



    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDialogLabel(getString(R.string.relationships));
        setDialogId(ID);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);
    }

    @Override
    public Loader<List<AutoCompleteDialogAdapter.OptionAdapterValue>> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(Program.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new RelationshipTypesQuery()
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<AutoCompleteDialogAdapter.OptionAdapterValue>> loader,
                               List<AutoCompleteDialogAdapter.OptionAdapterValue> data) {
        if (LOADER_ID == loader.getId()) {
            getAdapter().swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AutoCompleteDialogAdapter.OptionAdapterValue>> loader) {
        getAdapter().swapData(null);
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

    static class RelationshipTypesQuery implements Query<List<AutoCompleteDialogAdapter.OptionAdapterValue>> {

        public RelationshipTypesQuery() {
        }

        @Override
        public List<AutoCompleteDialogAdapter.OptionAdapterValue> query(Context context) {

            List<RelationshipType> relationshipTypes = MetaDataController.getRelationshipTypes();

            List<AutoCompleteDialogAdapter.OptionAdapterValue> values = new ArrayList<>();
            if (relationshipTypes != null && !relationshipTypes.isEmpty()) {
                for (RelationshipType relationshipType : relationshipTypes) {
                    values.add(new AutoCompleteDialogAdapter.OptionAdapterValue(relationshipType.getUid(), relationshipType.getName()));
                }
            }

            return values;
        }
    }
}
