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
    public ArrayList<String> getProgramRuleValidationErrors() {
        return null;
    }

    @Override
    public void recycle() {
        programOverviewFragment = null;
    }

    @Override
    public void initiateEvaluateProgramRules() {
        //do nothing
    }

    @Override
    public void mapFieldsToRulesAndIndicators() {
        //do nothing
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
        //do nothing
    }

    @Override
    public void applyShowErrorRuleAction(ProgramRuleAction programRuleAction) {
        //do nothing
    }

    @Override
    public void applyHideSectionRuleAction(ProgramRuleAction programRuleAction) {
        //do nothing
    }

    @Override
    public void applyCreateEventRuleAction(ProgramRuleAction programRuleAction) {
        //do nothing
    }

    @Override
    public void applyDisplayKeyValuePairRuleAction(ProgramRuleAction programRuleAction) {
        programOverviewFragment.displayKeyValuePair(programRuleAction);
    }

    @Override
    public void applyDisplayTextRuleAction(ProgramRuleAction programRuleAction) {
        programOverviewFragment.displayText(programRuleAction);
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