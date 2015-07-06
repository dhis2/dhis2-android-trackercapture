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

package org.hisp.dhis.android.trackercapture.fragments.upcomingevents;

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

import org.hisp.dhis.android.sdk.controllers.Dhis2;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.utils.ui.dialogs.AutoCompleteDialogAdapter;
import org.hisp.dhis.android.sdk.utils.ui.dialogs.AutoCompleteDialogFragment;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.trackercapture.ui.adapters.SimpleAdapter;

import java.util.ArrayList;
import java.util.List;


public class OrgUnitDialogFragment extends  AutoCompleteDialogFragment
        implements LoaderManager.LoaderCallbacks<List<AutoCompleteDialogAdapter.OptionAdapterValue>> {
    private static final String TAG = OrgUnitDialogFragment.class.getName();
    public static final int ID = 450123;
    private static final int LOADER_ID = 1;

    public static OrgUnitDialogFragment newInstance(OnOptionSelectedListener listener) {
        OrgUnitDialogFragment fragment = new OrgUnitDialogFragment();
        fragment.setOnOptionSetListener(listener);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDialogLabel("Organisation Units");
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
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new OrgUnitQuery()
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<AutoCompleteDialogAdapter.OptionAdapterValue>> loader,
                               List<AutoCompleteDialogAdapter.OptionAdapterValue> data) {
        if (loader.getId() == LOADER_ID) {
            getAdapter().swapData(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<AutoCompleteDialogAdapter.OptionAdapterValue>> loader) {
        getAdapter().swapData(null);
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

    static class OrgUnitQuery implements Query<List<AutoCompleteDialogAdapter.OptionAdapterValue>> {

        @Override
        public List<AutoCompleteDialogAdapter.OptionAdapterValue> query(Context context) {
            List<OrganisationUnit> orgUnits = queryUnits();
            List<AutoCompleteDialogAdapter.OptionAdapterValue> filteredUnits = new ArrayList<>();
            for (OrganisationUnit orgUnit : orgUnits) {
                if (hasPrograms(orgUnit.getId())) {
                    filteredUnits.add(new AutoCompleteDialogAdapter.OptionAdapterValue(orgUnit.getId(), orgUnit.getLabel()));
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
                            unitId, Program.MULTIPLE_EVENTS_WITH_REGISTRATION, Program.SINGLE_EVENT_WITH_REGISTRATION
                    );
            return (programs != null && !programs.isEmpty());
        }
    }
}