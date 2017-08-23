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

import android.support.v4.app.Fragment;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRuleAction;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.fragments.common.IProgramRuleFragmentHelper;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.ValidationErrorDialog;
import org.hisp.dhis.android.sdk.utils.services.ProgramRuleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrackedEntityInstanceDataEntryRuleHelper implements IProgramRuleFragmentHelper {

    private TrackedEntityInstanceDataEntryFragment mTrackedEntityInstanceDataEntryFragment;
    private ArrayList<String> programRuleValidationErrors;

    public TrackedEntityInstanceDataEntryRuleHelper(TrackedEntityInstanceDataEntryFragment trackedEntityInstanceDataEntryFragment) {
        this.mTrackedEntityInstanceDataEntryFragment = trackedEntityInstanceDataEntryFragment;
        this.programRuleValidationErrors = new ArrayList<>();
    }

    @Override
    public ArrayList<String> getProgramRuleValidationErrors() {
        return programRuleValidationErrors;
    }

    @Override
    public void recycle() {
        mTrackedEntityInstanceDataEntryFragment = null;
    }

    @Override
    public void initiateEvaluateProgramRules() {
        programRuleValidationErrors.clear();
        mTrackedEntityInstanceDataEntryFragment.initiateEvaluateProgramRules();
    }

    @Override
    public void mapFieldsToRulesAndIndicators() {
        mTrackedEntityInstanceDataEntryFragment.setProgramRulesForTrackedEntityAttributes(new HashMap<String, List<ProgramRule>>());
        for (ProgramRule programRule : mTrackedEntityInstanceDataEntryFragment.getForm().getProgram().getProgramRules()) {
            for (String trackedEntityAttribute : ProgramRuleService.getTrackedEntityAttributesInRule(programRule)) {
                List<ProgramRule> rulesForTrackedEntityAttribute = mTrackedEntityInstanceDataEntryFragment.getProgramRulesForTrackedEntityAttributes().get(trackedEntityAttribute);
                if (rulesForTrackedEntityAttribute == null) {
                    rulesForTrackedEntityAttribute = new ArrayList<>();
                    rulesForTrackedEntityAttribute.add(programRule);
                    mTrackedEntityInstanceDataEntryFragment.getProgramRulesForTrackedEntityAttributes().put(trackedEntityAttribute, rulesForTrackedEntityAttribute);
                } else {
                    rulesForTrackedEntityAttribute.add(programRule);
                }
            }
        }
    }

    @Override
    public Fragment getFragment() {
        return mTrackedEntityInstanceDataEntryFragment;
    }

    @Override
    public List<ProgramRule> getProgramRules() {
        return mTrackedEntityInstanceDataEntryFragment.getForm().getProgram().getProgramRules();
    }

    @Override
    public Event getEvent() {
        return null;
    }

    @Override
    public void applyCreateEventRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public void applyDisplayKeyValuePairRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public void applyDisplayTextRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public Enrollment getEnrollment() {
        return mTrackedEntityInstanceDataEntryFragment.getForm().getEnrollment();
    }

    @Override
    public TrackedEntityAttributeValue getTrackedEntityAttributeValue(String id) {
        return mTrackedEntityInstanceDataEntryFragment.getForm().getTrackedEntityAttributeValueMap().get(id);
    }

    @Override
    public DataValue getDataElementValue(String uid) {
        return null;
    }

    @Override
    public void saveDataElement(String uid) {

    }

    @Override
    public void saveTrackedEntityAttribute(String id) {
        mTrackedEntityInstanceDataEntryFragment.getSaveThread().schedule();
    }

    @Override
    public boolean blockingSpinnerNeeded() {
        return true;
    }

    @Override
    public void updateUi() {

    }

    /**
     * Displays a warning dialog to the user, indicating the data entry rows with values in them
     * are being hidden due to program rules.
     *
     * @param fragment
     * @param affectedValues
     */
    @Override
    public void showWarningHiddenValuesDialog(Fragment fragment, ArrayList<String> affectedValues) {
        ArrayList<String> trackedEntityAttributeNames = new ArrayList<>();
        for (String s : affectedValues) {
            TrackedEntityAttribute tea = MetaDataController.getTrackedEntityAttribute(s);
            if (tea != null) {
                trackedEntityAttributeNames.add(tea.getDisplayName());
            }
        }
        if (trackedEntityAttributeNames.isEmpty()) {
            return;
        }
        if (mTrackedEntityInstanceDataEntryFragment.getValidationErrorDialog() == null || !mTrackedEntityInstanceDataEntryFragment.getValidationErrorDialog().isVisible()) {
            ValidationErrorDialog validationErrorDialog = ValidationErrorDialog
                    .newInstance(fragment.getString(org.hisp.dhis.android.sdk.R.string.warning_hidefieldwithvalue), trackedEntityAttributeNames
                    );
            mTrackedEntityInstanceDataEntryFragment.setValidationErrorDialog(validationErrorDialog);
            if (fragment.isAdded()) {
                mTrackedEntityInstanceDataEntryFragment.getValidationErrorDialog().show(fragment.getChildFragmentManager());
            }
        }
    }

    @Override
    public void flagDataChanged(boolean hasChanged) {
        mTrackedEntityInstanceDataEntryFragment.flagDataChanged(hasChanged);
    }

    @Override
    public void applyShowWarningRuleAction(ProgramRuleAction programRuleAction) {
        String uid = programRuleAction.getDataElement();
        if (uid == null) {
            uid = programRuleAction.getTrackedEntityAttribute();
        }
        mTrackedEntityInstanceDataEntryFragment.getListViewAdapter().showWarningOnIndex(uid, programRuleAction.getContent());
    }

    @Override
    public void applyShowErrorRuleAction(ProgramRuleAction programRuleAction) {
        String uid = programRuleAction.getTrackedEntityAttribute();
        mTrackedEntityInstanceDataEntryFragment.getListViewAdapter().showErrorOnIndex(uid, programRuleAction.getContent());
        if (!programRuleValidationErrors.contains(programRuleAction.getContent())) {
            TrackedEntityAttributeValue value = getTrackedEntityAttributeValue(uid);
            String stringValue;
            if(value != null) {
                stringValue = value.getValue();
            } else {
                stringValue = "";
            }
            programRuleValidationErrors.add(programRuleAction.getContent() + " " + stringValue);
        }
    }


    @Override
    public void applyHideFieldRuleAction(ProgramRuleAction programRuleAction, List<String> affectedFieldsWithValue) {
        mTrackedEntityInstanceDataEntryFragment.getListViewAdapter().hideIndex(programRuleAction.getTrackedEntityAttribute());
        if (mTrackedEntityInstanceDataEntryFragment.containsValue(getTrackedEntityAttributeValue(programRuleAction.getTrackedEntityAttribute()))) {
            affectedFieldsWithValue.add(programRuleAction.getTrackedEntityAttribute());
        }
    }

    @Override
    public void applyHideSectionRuleAction(ProgramRuleAction programRuleAction) {
    }
}
