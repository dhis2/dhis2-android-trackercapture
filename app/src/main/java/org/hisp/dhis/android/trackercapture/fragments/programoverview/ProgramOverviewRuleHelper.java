package org.hisp.dhis.android.trackercapture.fragments.programoverview;


import android.support.v4.app.Fragment;

import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRule;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRuleAction;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.fragments.common.IProgramRuleFragmentHelper;

import java.util.ArrayList;
import java.util.List;

class ProgramOverviewRuleHelper implements IProgramRuleFragmentHelper {

    private ProgramOverviewFragment programOverviewFragment;

    ProgramOverviewRuleHelper(ProgramOverviewFragment programOverviewFragment) {
        this.programOverviewFragment = programOverviewFragment;
    }

    @Override
    public void initiateEvaluateProgramRules() {

    }

    @Override
    public void mapFieldsToRulesAndIndicators() {

    }

    @Override
    public Fragment getFragment() {
        return programOverviewFragment;
    }

    @Override
    public void updateUi() {
        //do nothing
    }

    @Override
    public List<ProgramRule> getProgramRules() {
        return programOverviewFragment.getForm().getProgram().getProgramRules();
    }

    @Override
    public Enrollment getEnrollment() {
        return programOverviewFragment.getForm().getEnrollment();
    }

    @Override
    public Event getEvent() {
        return null;
    }

    @Override
    public void applyShowWarningRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public void applyShowErrorRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public void applyHideSectionRuleAction(ProgramRuleAction programRuleAction) {
        //do nothing
    }

    @Override
    public void applyCreateEventRuleAction(ProgramRuleAction programRuleAction) {

    }

    @Override
    public void applyDisplayKeyValuePairRuleAction(ProgramRuleAction programRuleAction) {
        programOverviewFragment.displayKeyValuePair(programRuleAction);
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
        return null;
    }

    @Override
    public void flagDataChanged(boolean dataChanged) {
        //do nothing
    }

    @Override
    public void saveDataElement(String uid) {
        //do nothing
    }

    @Override
    public void saveTrackedEntityAttribute(String uid) {
        //do nothing
    }

    @Override
    public void applyHideFieldRuleAction(ProgramRuleAction programRuleAction, List affectedFieldsWithValue) {
        //do nothing
    }

    @Override
    public void showWarningHiddenValuesDialog(Fragment fragment, ArrayList affectedValues) {
        //do nothing
    }
}