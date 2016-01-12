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

import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramIndicator;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IndicatorRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageRow;

import java.util.List;
import java.util.Map;

class ProgramOverviewFragmentForm {
    private Enrollment enrollment;
    private Program program;
    private TrackedEntityInstance trackedEntityInstance;
    private String dateOfEnrollmentLabel;
    private String dateOfEnrollmentValue;
    private String incidentDateLabel;
    private String incidentDateValue;

    private String attribute1Label;
    private String attribute1Value;
    private String attribute2Label;
    private String attribute2Value;

    private List<ProgramStageRow> programStageRows;

    private Map<ProgramIndicator, IndicatorRow> programIndicatorRows;

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public TrackedEntityInstance getTrackedEntityInstance() {
        return trackedEntityInstance;
    }

    public void setTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        this.trackedEntityInstance = trackedEntityInstance;
    }

    public String getDateOfEnrollmentLabel() {
        return dateOfEnrollmentLabel;
    }

    public void setDateOfEnrollmentLabel(String dateOfEnrollmentLabel) {
        this.dateOfEnrollmentLabel = dateOfEnrollmentLabel;
    }

    public String getDateOfEnrollmentValue() {
        return dateOfEnrollmentValue;
    }

    public void setDateOfEnrollmentValue(String dateOfEnrollmentValue) {
        this.dateOfEnrollmentValue = dateOfEnrollmentValue;
    }

    public String getIncidentDateLabel() {
        return incidentDateLabel;
    }

    public void setIncidentDateLabel(String incidentDateLabel) {
        this.incidentDateLabel = incidentDateLabel;
    }

    public String getIncidentDateValue() {
        return incidentDateValue;
    }

    public void setIncidentDateValue(String incidentDateValue) {
        this.incidentDateValue = incidentDateValue;
    }

    public String getAttribute1Label() {
        return attribute1Label;
    }

    public void setAttribute1Label(String attribute1Label) {
        this.attribute1Label = attribute1Label;
    }

    public String getAttribute1Value() {
        return attribute1Value;
    }

    public void setAttribute1Value(String attribute1Value) {
        this.attribute1Value = attribute1Value;
    }

    public String getAttribute2Label() {
        return attribute2Label;
    }

    public void setAttribute2Label(String attribute2Label) {
        this.attribute2Label = attribute2Label;
    }

    public String getAttribute2Value() {
        return attribute2Value;
    }

    public void setAttribute2Value(String attribute2Value) {
        this.attribute2Value = attribute2Value;
    }

    public List<ProgramStageRow> getProgramStageRows() {
        return programStageRows;
    }

    public void setProgramStageRows(List<ProgramStageRow> programStageRows) {
        this.programStageRows = programStageRows;
    }

    public Map<ProgramIndicator, IndicatorRow> getProgramIndicatorRows() {
        return programIndicatorRows;
    }

    public void setProgramIndicatorRows(Map<ProgramIndicator, IndicatorRow> programIndicatorRows) {
        this.programIndicatorRows = programIndicatorRows;
    }
}