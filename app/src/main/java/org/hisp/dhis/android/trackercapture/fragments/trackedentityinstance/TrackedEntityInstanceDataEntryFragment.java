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

package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstance;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.ErrorType;
import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeGeneratedValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.ui.adapters.SectionAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowTypes;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.RunProgramRulesEvent;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.HideLoadingDialogEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RefreshListViewEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackedEntityInstanceDataEntryFragment extends
        DataEntryFragment<TrackedEntityInstanceDataEntryFragmentForm>
        implements OnBackPressedListener {
    public static final String TAG = TrackedEntityInstanceDataEntryFragment.class.getSimpleName();
    private static final String EMPTY_FIELD = "";
    public static final String ORG_UNIT_ID = "extra:orgUnitId";
    public static final String PROGRAM_ID = "extra:ProgramId";
    public static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";
    public static final String PROGRAMRULES_FORCED_TRIGGER = "forced";
    public static final String EXTRA_NAVIGATION = "extra:Navigation";
    public static boolean backNavigation;
    private TrackedEntityInstanceDataEntryFragmentForm form;
    private SaveThread saveThread;
    private Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes;

    //the TEI before anything is changed, used to backtrack
    private TrackedEntityInstance originalTrackedEntityInstance;

    //the trackedEntityAttributeValues before anything is changed, used to backtrack
    private Map<String, TrackedEntityAttributeValue> originalTrackedEntityAttributeValueMap;

    public TrackedEntityInstanceDataEntryFragment() {
        originalTrackedEntityInstance = null;
        setProgramRuleFragmentHelper(new TrackedEntityInstanceDataEntryRuleHelper(this));
    }

    public static TrackedEntityInstanceDataEntryFragment newInstance(String unitId,
            String programId, String enrollmentDate, String incidentDate) {
        TrackedEntityInstanceDataEntryFragment
                fragment = new TrackedEntityInstanceDataEntryFragment();
        Bundle args = new Bundle();
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);
        fragment.setArguments(args);
        return fragment;
    }

    public static TrackedEntityInstanceDataEntryFragment newInstance(String unitId,
            String programId, long trackedEntityInstanceId, String enrollmentDate,
            String incidentDate) {
        TrackedEntityInstanceDataEntryFragment
                fragment = new TrackedEntityInstanceDataEntryFragment();
        Bundle args = new Bundle();
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);
        args.putLong(TRACKEDENTITYINSTANCE_ID, trackedEntityInstanceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (saveThread == null || saveThread.isKilled()) {
            saveThread = new SaveThread();
            saveThread.start();
        }
        saveThread.init(this);
    }

    @Override
    public void onDestroy() {
        saveThread.kill();
        super.onDestroy();
    }

    @Override
    public SectionAdapter getSpinnerAdapter() {
        return null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public Loader<TrackedEntityInstanceDataEntryFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String orgUnitId = fragmentArguments.getString(ORG_UNIT_ID);
            String programId = fragmentArguments.getString(PROGRAM_ID);
            long trackedEntityInstance = fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new TrackedEntityInstanceDataEntryFragmentQuery(
                            orgUnitId, programId, trackedEntityInstance)
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<TrackedEntityInstanceDataEntryFragmentForm> loader,
            TrackedEntityInstanceDataEntryFragmentForm data) {
        if (loader.getId() == LOADER_ID && isAdded()) {
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            form = data;
            if (data.getTrackedEntityInstance().getLocalId() >= 0) {
                originalTrackedEntityInstance = new TrackedEntityInstance(
                        data.getTrackedEntityInstance());
            }
            if (originalTrackedEntityAttributeValueMap == null) {
                originalTrackedEntityAttributeValueMap = new HashMap<>();
                for (TrackedEntityAttributeValue trackedEntityAttributeValue : form
                        .getTrackedEntityAttributeValueMap().values()) {
                    TrackedEntityAttributeValue copiedTrackedEntityAttributeValue =
                            new TrackedEntityAttributeValue(trackedEntityAttributeValue);
                    originalTrackedEntityAttributeValueMap.put(
                            copiedTrackedEntityAttributeValue.getTrackedEntityAttributeId(),
                            copiedTrackedEntityAttributeValue);
                }
            }
            if (data.getProgram() != null) {
                getActionBar().setTitle(form.getProgram().getName());
            }
            if (form.isOutOfTrackedEntityAttributeGeneratedValues()) {
                for (Row row : form.getDataEntryRows()) {
                    row.setEditable(false);
                }
                UiUtils.showErrorDialog(getActivity(),
                        getString(R.string.error_message),
                        getString(
                                org.hisp.dhis.android.trackercapture.R.string.out_of_generated_ids),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                getActivity().finish();
                            }
                        });
            }
            if (data.getDataEntryRows() != null && !data.getDataEntryRows().isEmpty()) {
                listViewAdapter.swapData(data.getDataEntryRows());
            }
            if (data.getProgram().getProgramRules() != null &&
                    !data.getProgram().getProgramRules().isEmpty()) {
                initiateEvaluateProgramRules();
            }
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

    @Override
    public void onLoaderReset(Loader<TrackedEntityInstanceDataEntryFragmentForm> loader) {
        if (loader.getId() == LOADER_ID) {
            if (listViewAdapter != null) {
                listViewAdapter.swapData(null);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    protected HashMap<ErrorType, ArrayList<String>>  getValidationErrors() {
        HashMap<ErrorType, ArrayList<String>> errors = new HashMap<>();

        if (form.getEnrollment() == null || form.getProgram() == null
                || form.getOrganisationUnit() == null) {
            return errors;
        }

        if (isEmpty(form.getEnrollment().getEnrollmentDate())) {
            String dateOfEnrollmentDescription =
                    form.getProgram().getEnrollmentDateLabel() == null ?
                            getString(R.string.report_date)
                            : form.getProgram().getEnrollmentDateLabel();

            if (!errors.containsKey(ErrorType.MANDATORY)) {
                errors.put(ErrorType.MANDATORY, new ArrayList<String>());
            }
            errors.get(ErrorType.MANDATORY).add(dateOfEnrollmentDescription);
        }

        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(form.getProgram().getUid())
        );

        for (TrackedEntityAttributeValue value : form.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(
                    value.getTrackedEntityAttributeId());

            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                if (!errors.containsKey(ErrorType.MANDATORY)) {
                    errors.put(ErrorType.MANDATORY, new ArrayList<String>());
                }
                errors.get(ErrorType.MANDATORY).add(programTrackedEntityAttribute.getTrackedEntityAttribute().getName());
            }
        }
        return errors;
    }

    @Override
    public boolean isValid() {
        if (form.getProgram() == null || form.getOrganisationUnit() == null) {
            return false;
        }

        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(form.getProgram().getUid())
        );

        for (TrackedEntityAttributeValue value : form.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(
                    value.getTrackedEntityAttributeId());
            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static Map<String, ProgramTrackedEntityAttribute> toMap(
            List<ProgramTrackedEntityAttribute> attributes) {
        Map<String, ProgramTrackedEntityAttribute> attributeMap = new HashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            for (ProgramTrackedEntityAttribute attribute : attributes) {
                attributeMap.put(attribute.getTrackedEntityAttributeId(), attribute);
            }
        }
        return attributeMap;
    }

    @Override
    protected void save() {
    }

    @Override
    protected void proceed() {
        if (validate()) {
            confirmSave();
            HolderActivity.mCallBack.onSuccess();
            getActivity().finish();
        }
    }

    private boolean validate() {
        if (isMapEmpty(form.getTrackedEntityAttributeValueMap())) {
            UiUtils.showErrorDialog(getActivity(), getContext().getString(
                    org.hisp.dhis.android.trackercapture.R.string.error_message),
                    getContext().getString(
                            org.hisp.dhis.android.trackercapture.R.string.profile_form_empty));
            return false;
        }

        HashMap<ErrorType, ArrayList<String>>  allErrors = getValidationErrors();

        allErrors.put(ErrorType.PROGRAM_RULE, getProgramRuleFragmentHelper().getProgramRuleValidationErrors());

        allErrors.put(ErrorType.INVALID_FIELD, new ArrayList<String>());

        for (DataEntryRow dataEntryRow : form.getDataEntryRows()) {
            if (dataEntryRow.getValidationError() != null) {
                allErrors.get(ErrorType.INVALID_FIELD).add(getContext().getString(dataEntryRow.getValidationError()));
            }
        }

        if (allErrors != null &&
                (allErrors.get(ErrorType.INVALID_FIELD) != null && allErrors.get(ErrorType.INVALID_FIELD).size() > 0) ||
                (allErrors.get(ErrorType.MANDATORY) != null && allErrors.get(ErrorType.MANDATORY).size() > 0) ||
                (allErrors.get(ErrorType.PROGRAM_RULE) != null && allErrors.get(ErrorType.PROGRAM_RULE).size() > 0)) {
            showValidationErrorDialog(allErrors);
            return false;
        } else {
            return true;
        }
    }

    private boolean isMapEmpty(
            Map<String, TrackedEntityAttributeValue> trackedEntityAttributeValueMap) {
        boolean isEmpty = true;
        for (String key : trackedEntityAttributeValueMap.keySet()) {
            TrackedEntityAttributeValue value = trackedEntityAttributeValueMap.get(key);
            if (value.getValue() != null && !value.getValue().equals("")) {
                isEmpty = false;
            }
        }
        return isEmpty;
    }

    private void evaluateRules(String trackedEntityAttribute) {
        if (trackedEntityAttribute == null || form == null) {
            return;
        }
        if (PROGRAMRULES_FORCED_TRIGGER.equals(trackedEntityAttribute)) {
            getProgramRuleFragmentHelper().getProgramRuleValidationErrors().clear();
            initiateEvaluateProgramRules();
        }
        if (hasRules(trackedEntityAttribute)) {
            getProgramRuleFragmentHelper().getProgramRuleValidationErrors().clear();
            initiateEvaluateProgramRules();
        }
    }

    private boolean hasRules(String trackedEntityAttribute) {
        if (programRulesForTrackedEntityAttributes == null) {
            return false;
        }
        return programRulesForTrackedEntityAttributes.containsKey(trackedEntityAttribute);
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        super.onRowValueChanged(event);

        // do not run program rules for EditTextRows - DelayedDispatcher takes care of this
        if (event.getRow() == null || !(event.getRow().isEditTextRow())) {
            evaluateRules(event.getId());
        }

        if (DataEntryRowTypes.ENROLLMENT_DATE.toString().equals(event.getRowType())
                || DataEntryRowTypes.EVENT_DATE.toString().equals(event.getRowType())) {
            evaluateRules(PROGRAMRULES_FORCED_TRIGGER);
        }

        saveThread.schedule();
    }

    @Subscribe
    public void onRunProgramRules(final RunProgramRulesEvent event) {
        evaluateRules(event.getId());
    }

    @Subscribe
    public void onDetailedInfoClick(OnDetailedInfoButtonClick eventClick) {
        super.onShowDetailedInfo(eventClick);
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

    public TrackedEntityInstanceDataEntryFragmentForm getForm() {
        return form;
    }

    public void setForm(TrackedEntityInstanceDataEntryFragmentForm form) {
        this.form = form;
    }

    public Map<String, List<ProgramRule>> getProgramRulesForTrackedEntityAttributes() {
        return programRulesForTrackedEntityAttributes;
    }

    public void setProgramRulesForTrackedEntityAttributes(
            Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes) {
        this.programRulesForTrackedEntityAttributes = programRulesForTrackedEntityAttributes;
    }

    private void showConfirmDiscardDialog() {
        UiUtils.showConfirmDialog(getActivity(),
                getString(R.string.discard), getString(R.string.discard_confirm_changes),
                getString(R.string.discard),
                getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //discard
                        discardChanges();
                        getActivity().finish();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //cancel
                        dialog.dismiss();
                    }
                });
    }

    /**
     * confirms that we want to save changes, which flags the data to be sent to server
     */
    private void confirmSave() {
        if (form != null && form.getTrackedEntityInstance() != null) {
            if (form.getTrackedEntityInstance().getLocalId() < 0) {
                form.getTrackedEntityInstance().setFromServer(false);
                form.getTrackedEntityInstance().save();
            }

            for (String key : form.getTrackedEntityAttributeValueMap().keySet()) {
                TrackedEntityAttributeValue trackedEntityAttributeValue =
                        form.getTrackedEntityAttributeValueMap().get(key);
                trackedEntityAttributeValue.setLocalTrackedEntityInstanceId(
                        form.getTrackedEntityInstance().getLocalId());
                trackedEntityAttributeValue.setTrackedEntityInstanceId(
                        form.getTrackedEntityInstance().getTrackedEntityInstance());
                if (trackedEntityAttributeValue.getValue() != null
                        && !trackedEntityAttributeValue.getValue().equals("")) {
                    trackedEntityAttributeValue.save();
                }
            }

            flagDataChanged(false);
        }

        for (ProgramTrackedEntityAttribute ptea : form.getProgram()
                .getProgramTrackedEntityAttributes()) {
            if (ptea.getTrackedEntityAttribute().isGenerated()) {
                TrackedEntityAttributeValue attributeValue = TrackerController
                        .getTrackedEntityAttributeValue(ptea.getTrackedEntityAttributeId(),
                                form.getTrackedEntityInstance().getUid());

                TrackedEntityAttributeGeneratedValue trackedEntityAttributeGeneratedValue =
                        MetaDataController.getTrackedEntityAttributeGeneratedValue(
                                attributeValue.getValue());
                if (trackedEntityAttributeGeneratedValue != null) {
                    trackedEntityAttributeGeneratedValue.delete();
                } else {
                    trackedEntityAttributeGeneratedValue =
                            MetaDataController.getTrackedEntityAttributeGeneratedValue(
                                    ptea.getTrackedEntityAttributeId());
                    if (trackedEntityAttributeGeneratedValue != null) {
                        trackedEntityAttributeGeneratedValue.delete();
                    }
                }
            }
        }
    }

    private void discardChanges() {
        if (form == null) {
            return;
        }
        if (originalTrackedEntityInstance == null && form.getTrackedEntityInstance() != null) {
            form.getTrackedEntityInstance().delete();
        }
        for (TrackedEntityAttributeValue newValue : form.getTrackedEntityAttributeValueMap()
                .values()) {
            TrackedEntityAttributeValue originalValue = originalTrackedEntityAttributeValueMap.get(
                    newValue.getTrackedEntityAttributeId());
            if (originalValue == null) {
                newValue.delete();
            } else if (newValue.getValue() == null && originalValue.getValue() == null) {

            } else if (newValue.getValue() == null && originalValue.getValue() != null) {
                originalValue.save();
            } else if (!newValue.getValue().equals(originalValue.getValue())) {
                originalValue.save();
            }
        }
    }

    private boolean checkIfDataHasBeenEdited() {
        if (form == null || form.getEnrollment() == null) {
            return false;
        }

        for (TrackedEntityAttributeValue newValue : form.getTrackedEntityAttributeValueMap()
                .values()) {
            TrackedEntityAttributeValue originalValue = originalTrackedEntityAttributeValueMap.get(
                    newValue.getTrackedEntityAttributeId());
            if (originalValue == null) {
                return true;
            } else if (newValue.getValue() == null && originalValue.getValue() == null) {

            } else if (newValue.getValue() == null && originalValue.getValue() != null) {
                return true;
            } else if (!newValue.getValue().equals(originalValue.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doBack() {
        showConfirmDiscardDialog();
        return false;
    }

    @Override
    public boolean onBackPressed() {
        showConfirmDiscardDialog();
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            showConfirmDiscardDialog();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        GpsController.disableGps();
    }
}
