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

package org.hisp.dhis.android.trackercapture.fragments.programoverview.selectprogramdialogfragment;



import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.ui.dialogs.AutoCompleteDialogFragment;
import org.hisp.dhis.android.sdk.ui.dialogs.OrgUnitDialogFragment;
import org.hisp.dhis.android.sdk.ui.dialogs.ProgramDialogFragment;
import org.hisp.dhis.android.sdk.ui.dialogs.UpcomingEventsDialogFilter;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentPreferences;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentState;
import org.hisp.dhis.android.sdk.ui.views.CardTextViewButton;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.fragments.programoverview
        .registerrelationshipdialogfragment.RelationshipTypesDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.search.OnlineSearchResultFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.Action;

import java.util.Arrays;

public class SelectProgramDialogFragment extends DialogFragment
        implements View.OnClickListener  ,AutoCompleteDialogFragment.OnOptionSelectedListener  {
    private static final String TAG = SelectProgramDialogFragment.class.getSimpleName();

    protected final String STATE = "state:SelectProgramFragment";

    protected CardTextViewButton mOrgUnitButton;
    protected CardTextViewButton mProgramButton;
    private FloatingActionButton searchAndDownloadButton;
    private FloatingActionButton createNewTeiButton;
    private TextView mDialogLabel;

    protected SelectProgramFragmentState mState;
    protected SelectProgramFragmentPreferences mPrefs;
    private static OnlineSearchResultFragment.CallBack mCallBack;

    private static final String EXTRA_TRACKEDENTITYINSTANCEID = "extra:trackedEntityInstanceId";
    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";
    private static final String EXTRA_SAVED_ACTION = "extra:savedAction";
    public static SelectProgramDialogFragment newInstance(long trackedEntityInstanceId,
            Action action, OnlineSearchResultFragment.CallBack callBack) {
        mCallBack=callBack;
        SelectProgramDialogFragment dialogFragment = new SelectProgramDialogFragment();
        Bundle args = new Bundle();

        args.putLong(EXTRA_TRACKEDENTITYINSTANCEID, trackedEntityInstanceId);
        args.putSerializable(EXTRA_SAVED_ACTION, action);
        dialogFragment.setArguments(args);
        return dialogFragment;
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
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        return inflater.inflate(org.hisp.dhis.android.trackercapture.R.layout.dialog_fragment_selection_program, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mPrefs = new SelectProgramFragmentPreferences(
                getActivity().getApplicationContext());
        Action action = (Action) getArguments().getSerializable(EXTRA_SAVED_ACTION);

        if(action==Action.QUERY) {
            searchAndDownloadButton = (FloatingActionButton) view
                    .findViewById(
                            org.hisp.dhis.android.trackercapture.R.id.search_and_download_button);
            searchAndDownloadButton.setOnClickListener(this);
            searchAndDownloadButton.setVisibility(View.VISIBLE);
        }else if (action==Action.CREATE){
            createNewTeiButton = (FloatingActionButton) view
                    .findViewById(
                            org.hisp.dhis.android.trackercapture.R.id.create_new_tei_button);
            createNewTeiButton.setOnClickListener(this);
            createNewTeiButton.setVisibility(View.VISIBLE);
        }

        ImageView closeDialogButton = (ImageView) view
                .findViewById(R.id.close_dialog_button);
        closeDialogButton.setOnClickListener(this);
        mDialogLabel = (TextView) view
                .findViewById(R.id.dialog_label);
        setDialogLabel(org.hisp.dhis.android.trackercapture.R.string.download_entities_title);

        setOUAndProgramButtons(view);

        setState(savedInstanceState);
    }

    private void setState(Bundle savedInstanceState) {

        if (savedInstanceState != null &&
                savedInstanceState.getParcelable(STATE) != null) {
            mState = savedInstanceState.getParcelable(STATE);
        }

        if (mState == null) {
            // restoring last selection of program
            Pair<String, String> orgUnit = mPrefs.getOrgUnit();
            Pair<String, String> program = mPrefs.getProgram();
            Pair<String, String> filter = mPrefs.getFilter();
            mState = new SelectProgramFragmentState();
            if (orgUnit != null) {
                mState.setOrgUnit(orgUnit.first, orgUnit.second);
                if (program != null) {
                    mState.setProgram(program.first, program.second);
                }
                if(filter != null) {
                    mState.setFilter(filter.first, filter.second);
                }
                else {
                    mState.setFilter("0", Arrays.asList(UpcomingEventsDialogFilter.Type.values()).get(0).toString());
                }


            }
        }

        onRestoreState(true);
    }

    private void setOUAndProgramButtons(View view) {
        mOrgUnitButton = (CardTextViewButton) view.findViewById(R.id.select_organisation_unit);
        mProgramButton = (CardTextViewButton) view.findViewById(R.id.select_program);

        mOrgUnitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OrgUnitDialogFragment fragment = OrgUnitDialogFragment
                        .newInstance(SelectProgramDialogFragment.this, getProgramTypes());
                fragment.show(getChildFragmentManager());
            }
        });
        mProgramButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgramDialogFragment fragment = ProgramDialogFragment
                        .newInstance(SelectProgramDialogFragment.this, mState.getOrgUnitId(),
                                getProgramTypes());
                fragment.show(getChildFragmentManager());
            }
        });

        mOrgUnitButton.setEnabled(true);
        mProgramButton.setEnabled(false);
    }

    /* This method must be called only after onViewCreated() */
    public void setDialogLabel(int resourceId) {
        if (mDialogLabel != null) {
            mDialogLabel.setText(resourceId);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
    }

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, TAG);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == org.hisp.dhis.android.trackercapture.R.id.relationshiptypebutton ) {
            RelationshipTypesDialogFragment fragment = RelationshipTypesDialogFragment
                    .newInstance(this);
            fragment.show(getChildFragmentManager());
        } else if(v.getId() == R.id.close_dialog_button) {
            dismiss();
        } else if(v.getId() == org.hisp.dhis.android.trackercapture.R.id.search_and_download_button){
            HolderActivity.navigateToOnlineSearchFragment(getActivity(), mState.getProgramId(), mState.getOrgUnitId(), true, mCallBack);
            dismiss();
        } else if(v.getId() == org.hisp.dhis.android.trackercapture.R.id.create_new_tei_button){
            HolderActivity.navigateToTrackedEntityInstanceDataEntryFragment(getActivity(), mState.getProgramId(), mState.getOrgUnitId(), true, mCallBack);
            dismiss();
        }
    }

    @Override
    public void onOptionSelected(int dialogId, int position, String id, String name) {
        switch (dialogId) {
            case OrgUnitDialogFragment.ID: {
                onUnitSelected(id, name);
                break;
            }
            case ProgramDialogFragment.ID: {
                onProgramSelected(id, name);
                break;
            }
        }
    }

    public void onRestoreState(boolean hasUnits) {
        mOrgUnitButton.setEnabled(hasUnits);
        if (!hasUnits) {
            return;
        }

        SelectProgramFragmentState backedUpState = new SelectProgramFragmentState(mState);
        if (!backedUpState.isOrgUnitEmpty()) {
            onUnitSelected(
                    backedUpState.getOrgUnitId(),
                    backedUpState.getOrgUnitLabel()
            );

            if (!backedUpState.isProgramEmpty()) {
                onProgramSelected(
                        backedUpState.getProgramId(),
                        backedUpState.getProgramName()
                );
            }
        }

    }

    public void onUnitSelected(String orgUnitId, String orgUnitLabel) {
        mOrgUnitButton.setText(orgUnitLabel);
        mProgramButton.setEnabled(true);

        mState.setOrgUnit(orgUnitId, orgUnitLabel);
        mState.resetProgram();

        mPrefs.putOrgUnit(new Pair<>(orgUnitId, orgUnitLabel));
        mPrefs.putProgram(null);
    }

    public void onProgramSelected(String programId, String programName) {
        mProgramButton.setText(programName);

        mState.setProgram(programId, programName);
        mPrefs.putProgram(new Pair<>(programId, programName));

        // this call will trigger onCreateLoader method
    }

    protected ProgramType[] getProgramTypes() {
        return new ProgramType[]{
                ProgramType.WITH_REGISTRATION
        };
    }
}
