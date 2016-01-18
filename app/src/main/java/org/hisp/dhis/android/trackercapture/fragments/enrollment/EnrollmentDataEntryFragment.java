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

package org.hisp.dhis.android.trackercapture.fragments.enrollment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.ui.adapters.SectionAdapter;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.DataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.HideLoadingDialogEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RefreshListViewEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.SaveThread;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.ProgramOverviewFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class EnrollmentDataEntryFragment extends DataEntryFragment<EnrollmentDataEntryFragmentForm> implements OnBackPressedListener {
    public static final String TAG = EnrollmentDataEntryFragment.class.getSimpleName();
    private static final String EMPTY_FIELD = "";
    private static final String ORG_UNIT_ID = "extra:orgUnitId";
    private static final String PROGRAM_ID = "extra:ProgramId";
    private static final String ENROLLMENT_DATE = "extra:enrollmentDate";
    private static final String INCIDENT_DATE = "extra:incidentDate";
    private static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";
    private EnrollmentDataEntryFragmentForm form;
    private SaveThread saveThread;
    private Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes;

    //the enrollment before anything is changed, used to backtrack
    private Enrollment originalEnrollment;

    //the TEI before anything is changed, used to backtrack
    private TrackedEntityInstance originalTrackedEntityInstance;

    //the trackedEntityAttributeValues before anything is changed, used to backtrack
    private Map<String, TrackedEntityAttributeValue> originalTrackedEntityAttributeValueMap;

    public EnrollmentDataEntryFragment() {
        originalEnrollment = null;
        originalTrackedEntityInstance = null;
        setProgramRuleFragmentHelper(new EnrollmentDataEntryRuleHelper(this));
    }

    public static EnrollmentDataEntryFragment newInstance(String unitId, String programId, String enrollmentDate, String incidentDate) {
        EnrollmentDataEntryFragment fragment = new EnrollmentDataEntryFragment();
        Bundle args = new Bundle();
        args.putString(ENROLLMENT_DATE, enrollmentDate);
        args.putString(INCIDENT_DATE, incidentDate);
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);
        fragment.setArguments(args);
        return fragment;
    }

    public static EnrollmentDataEntryFragment newInstance(String unitId, String programId, long trackedEntityInstanceId, String enrollmentDate, String incidentDate) {
        EnrollmentDataEntryFragment fragment = new EnrollmentDataEntryFragment();
        Bundle args = new Bundle();
        args.putString(ENROLLMENT_DATE, enrollmentDate);
        args.putString(INCIDENT_DATE, incidentDate);
        args.putString(ORG_UNIT_ID, unitId);
        args.putString(PROGRAM_ID, programId);
        args.putLong(TRACKEDENTITYINSTANCE_ID, trackedEntityInstanceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(saveThread == null || saveThread.isKilled()) {
            saveThread = new SaveThread();
            saveThread.start();
        }
        saveThread.init(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        navigationHandler.setBackPressedListener(this);
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
    }

    @Override
    public Loader<EnrollmentDataEntryFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            // Adding Tables for tracking here is dangerous (since MetaData updates in background
            // can trigger reload of values from db which will reset all fields).
            // Hence, it would be more safe not to track any changes in any tables
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            Bundle fragmentArguments = args.getBundle(EXTRA_ARGUMENTS);
            String orgUnitId = fragmentArguments.getString(ORG_UNIT_ID);
            String programId = fragmentArguments.getString(PROGRAM_ID);
            String enrollmentDate = fragmentArguments.getString(ENROLLMENT_DATE);
            String incidentDate = fragmentArguments.getString(INCIDENT_DATE);
            long trackedEntityInstance = fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1);

            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack, new EnrollmentDataEntryFragmentQuery(
                    orgUnitId,programId, trackedEntityInstance, enrollmentDate, incidentDate)
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<EnrollmentDataEntryFragmentForm> loader, EnrollmentDataEntryFragmentForm data) {
        if (loader.getId() == LOADER_ID && isAdded()) {
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);

            if(originalEnrollment == null) {
                originalEnrollment = new Enrollment(data.getEnrollment());
            }

            form = data;
            if(data.getTrackedEntityInstance().getLocalId() >= 0) {
                originalTrackedEntityInstance = new TrackedEntityInstance(data.getTrackedEntityInstance());
            }
            if(originalTrackedEntityAttributeValueMap == null) {
                originalTrackedEntityAttributeValueMap = new HashMap<>();
                for(TrackedEntityAttributeValue trackedEntityAttributeValue : form.getTrackedEntityAttributeValueMap().values()) {
                    TrackedEntityAttributeValue copiedTrackedEntityAttributeValue = new TrackedEntityAttributeValue(trackedEntityAttributeValue);
                    originalTrackedEntityAttributeValueMap.put(copiedTrackedEntityAttributeValue.getTrackedEntityAttributeId(), copiedTrackedEntityAttributeValue);
                }
            }

            if (data.getProgram() != null) {
                getActionBarToolbar().setTitle(form.getProgram().getName());
            }

            if(data.getDataEntryRows() != null && !data.getDataEntryRows().isEmpty())
            {
                listViewAdapter.swapData(data.getDataEntryRows());
            }
            initiateEvaluateProgramRules();
        }
    }

    /**
     * Schedules evaluation and updating of views based on ProgramRules in a thread.
     * This is used to avoid stacking up calls to evaluateAndApplyProgramRules
     */
    public void initiateEvaluateProgramRules() {
        if(rulesEvaluatorThread!=null) {
            rulesEvaluatorThread.schedule();
        }
    }

    @Override
    public void onLoaderReset(Loader<EnrollmentDataEntryFragmentForm> loader) {
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
    protected ArrayList<String> getValidationErrors() {
        ArrayList<String> errors = new ArrayList<>();

        if (form.getEnrollment() == null || form.getProgram() == null || form.getOrganisationUnit() == null) {
            return errors;
        }

        if (isEmpty(form.getEnrollment().getEnrollmentDate())) {
            String dateOfEnrollmentDescription = form.getProgram().getEnrollmentDateLabel() == null ?
                    getString(R.string.report_date) : form.getProgram().getEnrollmentDateLabel();
            errors.add(dateOfEnrollmentDescription);
        }

        Map<String, ProgramTrackedEntityAttribute> dataElements = toMap(
                MetaDataController.getProgramTrackedEntityAttributes(form.getProgram().getUid())
        );

        for (TrackedEntityAttributeValue value : form.getEnrollment().getAttributes()) {
            ProgramTrackedEntityAttribute programTrackedEntityAttribute = dataElements.get(value.getTrackedEntityAttributeId());

            if (programTrackedEntityAttribute.getMandatory() && isEmpty(value.getValue())) {
                errors.add(programTrackedEntityAttribute.getTrackedEntityAttribute().getName());
            }
        }
        return errors;
    }

    @Override
    public boolean isValid() {
        if (form.getEnrollment() == null || form.getProgram() == null || form.getOrganisationUnit() == null) {
            return false;
        }

        if (isEmpty(form.getEnrollment().getEnrollmentDate())) {
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

    private static Map<String, ProgramTrackedEntityAttribute> toMap(List<ProgramTrackedEntityAttribute> attributes) {
        Map<String, ProgramTrackedEntityAttribute> attributeMap = new HashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            for (ProgramTrackedEntityAttribute attribute : attributes) {
                attributeMap.put(attribute.getTrackedEntityAttributeId(), attribute);
            }
        }
        return attributeMap;
    }

    /**
     * returns true if the enrollment was successfully saved
     * @return
     */
    @Override
    protected void save() {
        if (form != null && form.getTrackedEntityInstance() != null) {
            if (form.getTrackedEntityInstance().getLocalId() < 0) {
                //saving tei first to get auto-increment reference for enrollment
                form.getTrackedEntityInstance().setFromServer(true);
                form.getTrackedEntityInstance().save();
            }
            for(Event event : form.getEnrollment().getEvents()) {
                event.setFromServer(true);
            }
            form.getEnrollment().setLocalTrackedEntityInstanceId(form.getTrackedEntityInstance().getLocalId());
            form.getEnrollment().setFromServer(true); //setting from server true to avoid sending to server before we finish editing
            form.getEnrollment().save();
            flagDataChanged(false);
        }
    }

    @Override
    protected void proceed() {
        if(validate()) {
            confirmSave();
            showProgramOverviewFragment();
        }
    }

    private void showProgramOverviewFragment() {
        boolean fragmentPopped = getFragmentManager().popBackStackImmediate(ProgramOverviewFragment.CLASS_TAG, 0);
        if(!fragmentPopped) {
            navigationHandler.setBackPressedListener(null);
            navigationHandler.switchFragment(
                    ProgramOverviewFragment.newInstance(
                            form.getOrganisationUnit().getId(), form.getProgram().getUid(), form.getTrackedEntityInstance().getLocalId()),
                    ProgramOverviewFragment.CLASS_TAG, false);
        }
    }

    private boolean validate() {
        ArrayList<String> programRulesValidationErrors = getProgramRuleFragmentHelper().getProgramRuleValidationErrors();
        ArrayList<String> mandatoryValidationErrors = getValidationErrors();
        if(programRulesValidationErrors.isEmpty() && mandatoryValidationErrors.isEmpty()) {
            return true;
        } else {
            showValidationErrorDialog(mandatoryValidationErrors, programRulesValidationErrors);
            return false;
        }
    }

    private void evaluateRules(String trackedEntityAttribute) {
        if (trackedEntityAttribute == null || form == null) {
            return;
        }
        if(hasRules(trackedEntityAttribute)) {
            initiateEvaluateProgramRules();
        }
    }

    private boolean hasRules(String trackedEntityAttribute) {
        if(programRulesForTrackedEntityAttributes==null) {
            return false;
        }
        return programRulesForTrackedEntityAttributes.containsKey(trackedEntityAttribute);
    }

    @Subscribe
    public void onRowValueChanged(final RowValueChangedEvent event) {
        super.onRowValueChanged(event);
        evaluateRules(event.getId());
        saveThread.schedule();
    }

    @Subscribe
    public void onDetailedInfoClick(OnDetailedInfoButtonClick eventClick)
    {
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

    public EnrollmentDataEntryFragmentForm getForm() {
        return form;
    }

    public void setForm(EnrollmentDataEntryFragmentForm form) {
        this.form = form;
    }

    public Map<String, List<ProgramRule>> getProgramRulesForTrackedEntityAttributes() {
        return programRulesForTrackedEntityAttributes;
    }

    public void setProgramRulesForTrackedEntityAttributes(Map<String, List<ProgramRule>> programRulesForTrackedEntityAttributes) {
        this.programRulesForTrackedEntityAttributes = programRulesForTrackedEntityAttributes;
    }

    private void showConfirmDiscardDialog() {
        UiUtils.showConfirmDialog(getActivity(),
                getString(org.hisp.dhis.android.sdk.R.string.discard), getString(org.hisp.dhis.android.sdk.R.string.discard_confirm_changes),
                getString(org.hisp.dhis.android.sdk.R.string.discard),
                getString(org.hisp.dhis.android.sdk.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //discard
                        discardChanges();
                        navigationHandler.setBackPressedListener(null);
                        navigationHandler.onBackPressed();
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
        form.getTrackedEntityInstance().setFromServer(false);
        form.getEnrollment().setFromServer(false);
        form.getTrackedEntityInstance().save();
        form.getEnrollment().save();
        for(Event event : form.getEnrollment().getEvents()) {
            event.setFromServer(false);
            event.save();
        }
    }

    private void discardChanges() {
        if(originalEnrollment == null && form.getEnrollment() != null) {
            form.getEnrollment().delete();
            List<Event> events = form.getEnrollment().getEvents();
            if(events != null) {
                for (Event event : events) {
                    event.delete();
                }
            }
        }
        if(originalEnrollment != null && originalEnrollment.getLocalId() != form.getEnrollment().getLocalId()) {
            form.getEnrollment().delete();
            List<Event> events = form.getEnrollment().getEvents();
            if(events != null)
            for(Event event : events) {
                event.delete();
            }
        }
        if(originalEnrollment != null && !originalEnrollment.equals(form.getEnrollment())) {
            originalEnrollment.save();
        }
        if(originalTrackedEntityInstance == null && form.getTrackedEntityInstance() != null) {
            form.getTrackedEntityInstance().delete();
        }
        for(TrackedEntityAttributeValue newValue : form.getTrackedEntityAttributeValueMap().values()) {
            TrackedEntityAttributeValue originalValue = originalTrackedEntityAttributeValueMap.get(newValue.getTrackedEntityAttributeId());
            if(originalValue == null) {
                newValue.delete();
            } else if(newValue.getValue() == null && originalValue.getValue() == null) {

            } else if(newValue.getValue() == null && originalValue.getValue() != null) {
                originalValue.save();
            } else if(!newValue.getValue().equals(originalValue.getValue())) {
                originalValue.save();
            }
        }
    }

    private boolean checkIfDataHasBeenEdited() {
        if(originalEnrollment != null && !originalEnrollment.equals(form.getEnrollment())) {
            return true;
        }
        if(originalEnrollment != null && originalEnrollment.getLocalId() != form.getEnrollment().getLocalId()) {
            return true;
        }
        for(TrackedEntityAttributeValue newValue : form.getTrackedEntityAttributeValueMap().values()) {
            TrackedEntityAttributeValue originalValue = originalTrackedEntityAttributeValueMap.get(newValue.getTrackedEntityAttributeId());
            if(originalValue == null) {
                return true;
            } else if(newValue.getValue() == null && originalValue.getValue() == null) {

            } else if(newValue.getValue() == null && originalValue.getValue() != null) {
                return true;
            } else if(!newValue.getValue().equals(originalValue.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean doBack() {
        if(checkIfDataHasBeenEdited()) {
            showConfirmDiscardDialog();
            return false;
        } else {
            return true;
        }
    }
}
