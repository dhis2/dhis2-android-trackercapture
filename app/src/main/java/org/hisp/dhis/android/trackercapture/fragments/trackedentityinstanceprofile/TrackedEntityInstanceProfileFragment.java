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

package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.ErrorType;
import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.ui.adapters.SectionAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.RunProgramRulesEvent;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.HideLoadingDialogEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RefreshListViewEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.sdk.utils.ScreenSizeConfigurator;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by erling on 5/18/15.
 */
public class TrackedEntityInstanceProfileFragment extends DataEntryFragment<TrackedEntityInstanceProfileFragmentForm>
        implements OnBackPressedListener {
    public static final String TAG = TrackedEntityInstanceProfileFragment.class.getName();
    public static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";
    public static final String PROGRAM_ID = "extra:ProgramId";
    public static final String ENROLLMENT_ID = "extra:EnrollmentID";

    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    private static final int LOADER_ID = 95640;
    private static final String TRACKEDENTITYINSTANCE_ORIGINAL = "extra:OriginalTEI";

    private boolean edit;
    private boolean editableDataEntryRows;

    private Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes;
    private TrackedEntityInstanceProfileFragmentForm form;
    private SaveThread saveThread;

    //the TEI before anything is changed, used to backtrack
    private TrackedEntityInstance originalTrackedEntityInstance;

    public TrackedEntityInstanceProfileFragment() {
        originalTrackedEntityInstance = null;
        setProgramRuleFragmentHelper(new TrackedEntityInstanceProfileRuleHelper(this));
    }

    public static TrackedEntityInstanceProfileFragment newInstance(long mTrackedEntityInstanceId, String mProgramId, Long enrollmentId) {
        TrackedEntityInstanceProfileFragment fragment = new TrackedEntityInstanceProfileFragment();
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putLong(TRACKEDENTITYINSTANCE_ID, mTrackedEntityInstanceId);
        fragmentArgs.putString(PROGRAM_ID, mProgramId);
        fragmentArgs.putLong(ENROLLMENT_ID, enrollmentId);
        fragment.setArguments(fragmentArgs);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setTitle(getString(R.string.profile));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (saveThread == null || saveThread.isKilled()) {
            saveThread = new SaveThread();
            saveThread.start();
        }
        saveThread.init(this);
        setHasOptionsMenu(true);
        editableDataEntryRows = false;
    }

    @Override
    public void onDestroy() {
        saveThread.kill();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(org.hisp.dhis.android.sdk.R.menu.menu_data_entry, menu);
        final MenuItem editFormButton = menu.findItem(org.hisp.dhis.android.sdk.R.id.action_new_event);
        editFormButton.setEnabled(true);
        editFormButton.setIcon(R.drawable.ic_edit);
        editFormButton.getIcon().setAlpha(0xFF);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            doBack();
            return true;
        } else if (menuItem.getItemId() == org.hisp.dhis.android.sdk.R.id.action_new_event) {
            if (editableDataEntryRows) {
                setEditableDataEntryRows(false);
            } else {
                setEditableDataEntryRows(true);
            }
            editableDataEntryRows = !editableDataEntryRows;
            proceed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean doBack() {
        if (edit) {
            UiUtils.showConfirmDialog(getActivity(),
                    getString(org.hisp.dhis.android.sdk.R.string.discard), getString(org.hisp.dhis.android.sdk.R.string.discard_confirm_changes),
                    getString(org.hisp.dhis.android.sdk.R.string.save_and_close),
                    getString(org.hisp.dhis.android.sdk.R.string.discard),
                    getString(org.hisp.dhis.android.sdk.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (validate()) {
                                onDetach();
                                //                            getFragmentManager().popBackStack();
                                DhisController.hasUnSynchronizedDatavalues = true;
                                getActivity().finish();
                            }
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onDetach();
                            getActivity().finish();
//                            getFragmentManager().popBackStack();

                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
        } else {
            onDetach();
            getActivity().finish();
        }
        return false;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    @Override
    public Loader<TrackedEntityInstanceProfileFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String programId = fragmentArguments.getString(PROGRAM_ID);
            long enrollmentId = fragmentArguments.getLong(ENROLLMENT_ID);
            long trackedEntityInstance = fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1);
            return new DbLoader<>(
                    getActivity().getBaseContext(),
                    modelsToTrack,
                    new TrackedEntityInstanceProfileFragmentQuery(
                            trackedEntityInstance,
                            programId,
                            enrollmentId));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<TrackedEntityInstanceProfileFragmentForm> loader, TrackedEntityInstanceProfileFragmentForm data) {
        if (loader.getId() == LOADER_ID && isAdded()) {
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            form = data;
            listViewAdapter.swapData(form.getDataEntryRows());
            programRuleFragmentHelper.mapFieldsToRulesAndIndicators();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(TRACKEDENTITYINSTANCE_ORIGINAL, originalTrackedEntityInstance);
    }

    @Override
    public void onLoaderReset(Loader<TrackedEntityInstanceProfileFragmentForm> loader) {
        if (listViewAdapter != null) {
            listViewAdapter.swapData(null);
        }
    }

    public void setEditableDataEntryRows(boolean editable) {
        listViewAdapter.swapData(null);
        List<Row> rows = new ArrayList<>(form.getDataEntryRows());
        //is that needed now ? :
        for (Row row : rows) {
            if (!row.isShouldNeverBeEdited()) {
                row.setEditable(editable);
            }
        }
        if (editable) {
            if (form.getTrackedEntityInstance().getLocalId() >= 0) {
                originalTrackedEntityInstance = new TrackedEntityInstance(form.getTrackedEntityInstance());
            }
            if (form.isOutOfTrackedEntityAttributeGeneratedValues()) {
                for (Row row : form.getDataEntryRows()) {
                    row.setEditable(false);
                }
                UiUtils.showErrorDialog(getActivity(),
                        getString(R.string.error_message),
                        getString(R.string.out_of_generated_ids),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getActivity().finish();
                            }
                        });
            }
        }
        listViewAdapter.swapData(rows);
        listView.setAdapter(listViewAdapter);
        if (editable) {
            initiateEvaluateProgramRules();
        }
    }

    public void flagDataChanged(boolean changed) {
        edit = changed;
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        flagDataChanged(true);
        if (form == null) {
            return;
        }
        // do not run program rules for EditTextRows - DelayedDispatcher takes care of this
        if (event.getRow() == null || !(event.getRow().isEditTextRow())) {
            evaluateRules(event.getId());
        }
        saveThread.schedule();
    }

    @Subscribe
    public void onRunProgramRules(final RunProgramRulesEvent event) {
        evaluateRules(event.getId());
    }

    private void evaluateRules(String trackedEntityAttribute) {
        if (trackedEntityAttribute == null || form == null) {
            return;
        }
        if (hasRules(trackedEntityAttribute)) {
            getProgramRuleFragmentHelper().getProgramRuleValidationErrors().clear();
            initiateEvaluateProgramRules();
        }
    }

    /**
     * Schedules evaluation and updating of views based on ProgramRules in a thread.
     * This is used to avoid stacking up calls to evaluateAndApplyProgramRules
     */
    public void initiateEvaluateProgramRules() {
        if (rulesEvaluatorThread != null) {
            rulesEvaluatorThread.schedule();
        }
    }

    private boolean hasRules(String trackedEntityAttribute) {
        if (programRulesForTrackedEntityAttributes == null) {
            return false;
        }
        return programRulesForTrackedEntityAttributes.containsKey(trackedEntityAttribute);
    }

    @Subscribe
    public void onRefreshListView(RefreshListViewEvent event) {
        super.onRefreshListView(event);
    }

    @Subscribe
    public void onHideLoadingDialog(HideLoadingDialogEvent event) {
        super.onHideLoadingDialog(event);
    }

    public SaveThread getSaveThread() {
        return saveThread;
    }

    public void setSaveThread(SaveThread saveThread) {
        this.saveThread = saveThread;
    }

    public TrackedEntityInstanceProfileFragmentForm getForm() {
        return form;
    }

    public void setForm(TrackedEntityInstanceProfileFragmentForm form) {
        this.form = form;
    }

    public Map<String, List<ProgramRule>> getProgramRulesForTrackedEntityAttributes() {
        return programRulesForTrackedEntityAttributes;
    }

    public void setProgramRulesForTrackedEntityAttributes(Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes) {
        this.programRulesForTrackedEntityAttributes = programRulesForTrackedEntityAttributes;
    }

    @Override
    public SectionAdapter getSpinnerAdapter() {
        return null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
    }

    @Override
    protected HashMap<ErrorType, ArrayList<String>> getValidationErrors() {
        HashMap<ErrorType, ArrayList<String>> errors = new HashMap<>();
        if (form.getEnrollment() == null || form.getProgram() == null) {
            return errors;
        }
        if (isEmpty(form.getEnrollment().getEnrollmentDate())) {
            String dateOfEnrollmentDescription = form.getProgram().getEnrollmentDateLabel() == null ?
                    getString(R.string.report_date) : form.getProgram().getEnrollmentDateLabel();
            if(!errors.containsKey(ErrorType.MANDATORY)){
                errors.put(ErrorType.MANDATORY, new ArrayList<String>());
            }
            errors.get(ErrorType.MANDATORY).add(dateOfEnrollmentDescription);
        }
        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(form.getProgram().getUid())
        );
        for (TrackedEntityAttributeValue value : form.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(value.getTrackedEntityAttributeId());
            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                if(!errors.containsKey(ErrorType.MANDATORY)){
                    errors.put(ErrorType.MANDATORY, new ArrayList<String>());
                }
                errors.get(ErrorType.MANDATORY).add(programTrackedEntityAttribute.getTrackedEntityAttribute().getName());
            }
            if(programTrackedEntityAttribute.getTrackedEntityAttribute().isUnique()){
                if(value.getValue()==null || value.getValue().isEmpty()) {
                    continue;
                }
                if(TrackerController.countTrackedEntityAttributeValue(value)!=0){
                    if(!errors.containsKey(ErrorType.UNIQUE)){
                        errors.put(ErrorType.UNIQUE, new ArrayList<String>());
                    }
                    errors.get(ErrorType.UNIQUE).add(programTrackedEntityAttribute.getTrackedEntityAttribute().getName());
                }
            }
        }
        return errors;
    }

    private static Map<String, ProgramTrackedEntityAttribute> toMap(List<ProgramTrackedEntityAttribute> attributes) {
        Map<String, ProgramTrackedEntityAttribute> attributeMap = new HashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            for (ProgramTrackedEntityAttribute attribute : attributes) {
                attributeMap.put(attribute.getTrackedEntityAttributeId(), attribute);
            }
        }
        return attributeMap;
    }

    @Override
    protected boolean isValid() {
        if (form.getEnrollment() == null || form.getProgram() == null) {
            return false;
        }
        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(form.getProgram().getUid())
        );
        for (TrackedEntityAttributeValue value : form.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(value.getTrackedEntityAttributeId());
            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void save() { }

    @Override
    protected void proceed() {
        if (!edit) {// if rows are not edited
            return;
        }
        if (validate() &&
                form != null && isAdded() && form.getTrackedEntityInstance() != null) {
            for (TrackedEntityAttributeValue val : form.getTrackedEntityAttributeValues()) {
                val.save();
            }
            form.getTrackedEntityInstance().setFromServer(false);
            form.getTrackedEntityInstance().save();
            flagDataChanged(false);
        }
    }

    private boolean validate() {
        ArrayList<String> programRulesValidationErrors = getProgramRuleFragmentHelper().getProgramRuleValidationErrors();
        HashMap<ErrorType, ArrayList<String>>  allErrors = getValidationErrors();
        ArrayList<String> validationErrors = new ArrayList<>();
        for(DataEntryRow dataEntryRow : form.getDataEntryRows()){
            if(dataEntryRow.getValidationError()!=null)
                validationErrors.add(getContext().getString(dataEntryRow.getValidationError()));
        }
        if (programRulesValidationErrors.isEmpty() && allErrors.isEmpty() && validationErrors.isEmpty()) {
            return true;
        } else {
            allErrors.put(ErrorType.PROGRAM_RULE, programRulesValidationErrors);
            allErrors.put(ErrorType.INVALID_FIELD, validationErrors);
            showValidationErrorDialog(allErrors);
            return false;
        }
    }

    @Subscribe
    public void onDetailedInfoClick(OnDetailedInfoButtonClick eventClick) {
        super.onShowDetailedInfo(eventClick);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {}

    @Override
    public void onDetach() {
        super.onDetach();
        GpsController.disableGps();
    }
}
