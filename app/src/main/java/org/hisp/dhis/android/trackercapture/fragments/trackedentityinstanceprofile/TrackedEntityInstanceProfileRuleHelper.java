package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import android.support.v4.app.Fragment;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRuleAction;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.fragments.common.IProgramRuleFragmentHelper;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.ValidationErrorDialog;
import org.hisp.dhis.android.sdk.utils.services.ProgramRuleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TrackedEntityInstanceProfileRuleHelper implements IProgramRuleFragmentHelper {

    private TrackedEntityInstanceProfileFragment fragment;
    private ArrayList<String> programRuleValidationErrors;

    public TrackedEntityInstanceProfileRuleHelper(TrackedEntityInstanceProfileFragment fragment) {
        this.fragment = fragment;
        programRuleValidationErrors = new ArrayList<>();
    }

    @Override
    public ArrayList<String> getProgramRuleValidationErrors() {
        return programRuleValidationErrors;
    }

    @Override
    public void recycle() {
        fragment = null;
        programRuleValidationErrors.clear();
        programRuleValidationErrors = null;
    }

    @Override
    public void initiateEvaluateProgramRules() {
        programRuleValidationErrors.clear();
        fragment.initiateEvaluateProgramRules();
    }

    @Override
    public void mapFieldsToRulesAndIndicators() {
        fragment.setProgramRulesForTrackedEntityAttributes(new HashMap<String, List<ProgramRule>>());
        for (ProgramRule programRule : fragment.getForm().getProgram().getProgramRules()) {
            for (String trackedEntityAttribute : ProgramRuleService.getTrackedEntityAttributesInRule(programRule)) {
                List<ProgramRule> rulesForTrackedEntityAttribute =
                        fragment.getProgramRulesForTrackedEntityAttributes()
                                .get(trackedEntityAttribute);
                if (rulesForTrackedEntityAttribute == null) {
                    rulesForTrackedEntityAttribute = new ArrayList<>();
                    rulesForTrackedEntityAttribute.add(programRule);
                    fragment.getProgramRulesForTrackedEntityAttributes()
                            .put(trackedEntityAttribute, rulesForTrackedEntityAttribute);
                } else {
                    rulesForTrackedEntityAttribute.add(programRule);
                }
            }
        }
    }

    @Override
    public Fragment getFragment() {
        return fragment;
    }

    @Override
    public void showWarningHiddenValuesDialog(Fragment parentFragment, ArrayList<String> affectedValues) {
        ArrayList<String> dataElementNames = new ArrayList<>();
        for (String s : affectedValues) {
            DataElement de = MetaDataController.getDataElement(s);
            if (de != null) {
                dataElementNames.add(de.getDisplayName());
            }
        }
        if (dataElementNames.isEmpty()) {
            return;
        }
        if (fragment.getValidationErrorDialog() == null || !fragment.getValidationErrorDialog().isVisible()) {
            ValidationErrorDialog validationErrorDialog = ValidationErrorDialog.newInstance(
                    parentFragment.getString(org.hisp.dhis.android.sdk.R.string.warning_hidefieldwithvalue),
                    dataElementNames);
            fragment.setValidationErrorDialog(validationErrorDialog);
            if (parentFragment.isAdded()) {
                fragment.getValidationErrorDialog().show(parentFragment.getChildFragmentManager());
            }
        }
    }

    @Override
    public void updateUi() {
    }

    @Override
    public List<ProgramRule> getProgramRules() {
        return fragment.getForm().getProgram().getProgramRules();
    }

    @Override
    public Enrollment getEnrollment() {
        return fragment.getForm().getEnrollment();
    }

    @Override
    public Event getEvent() {
        return null;
    }

    @Override
    public void applyShowWarningRuleAction(ProgramRuleAction programRuleAction) {
        String uid = programRuleAction.getDataElement();
        if (uid == null) {
            uid = programRuleAction.getTrackedEntityAttribute();
        }
        fragment.getListViewAdapter().showWarningOnIndex(uid, programRuleAction.getContent());
    }

    @Override
    public void applyShowErrorRuleAction(ProgramRuleAction programRuleAction) {
        String uid = programRuleAction.getDataElement();
        if (uid == null) {
            uid = programRuleAction.getTrackedEntityAttribute();
        }
        fragment.getListViewAdapter().showErrorOnIndex(uid, programRuleAction.getContent());
        if (!programRuleValidationErrors.contains(programRuleAction.getContent())) {
            TrackedEntityAttributeValue value = getTrackedEntityAttributeValue(uid);
            programRuleValidationErrors.add(programRuleAction.getContent() + " " + value.getValue());
        }
    }

    @Override
    public void applyHideFieldRuleAction(ProgramRuleAction programRuleAction, List<String> affectedFieldsWithValue) {
        fragment.getListViewAdapter().hideIndex(programRuleAction.getDataElement());
        if (fragment.containsValue(getDataElementValue(programRuleAction.getDataElement()))) {
            affectedFieldsWithValue.add(programRuleAction.getDataElement());
        }
    }

    @Override
    public void applyHideSectionRuleAction(ProgramRuleAction programRuleAction) {
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
    public DataValue getDataElementValue(String uid) {
        return null;
    }

    @Override
    public TrackedEntityAttributeValue getTrackedEntityAttributeValue(String uid) {
        return fragment.getForm().getTrackedEntityAttributeValueMap().get(uid);
    }

    @Override
    public void flagDataChanged(boolean dataChanged) {
        fragment.flagDataChanged(dataChanged);
    }

    @Override
    public void saveDataElement(String uid) {
    }

    @Override
    public void saveTrackedEntityAttribute(String uid) {
        fragment.getSaveThread().schedule();
    }

    @Override
    public boolean blockingSpinnerNeeded() {
        return true;
    }
}