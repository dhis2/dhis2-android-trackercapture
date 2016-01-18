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

package org.hisp.dhis.android.trackercapture.fragments.enrollmentdate;

import android.content.Context;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.EnrollmentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IncidentDatePickerRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;

import java.util.ArrayList;
import java.util.List;

public class EnrollmentDateFragmentQuery implements Query<EnrollmentDateFragmentForm> {
    private final long enrollmentId;

    public EnrollmentDateFragmentQuery(long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    @Override
    public EnrollmentDateFragmentForm query(Context context) {
        //should return two enrollmentdatepickerrows when developed (enrollment date & incident date)
        EnrollmentDateFragmentForm fragmentForm = new EnrollmentDateFragmentForm();

        Enrollment enrollment = TrackerController.getEnrollment(enrollmentId);
        if (enrollment == null)
            return fragmentForm;

        List<Row> dataEntryRows = new ArrayList<>();
        dataEntryRows.add(new EnrollmentDatePickerRow(enrollment.getProgram().getEnrollmentDateLabel(), enrollment, enrollment.getEnrollmentDate()));

        if (enrollment.getProgram().getDisplayIncidentDate()) {
            dataEntryRows.add(new IncidentDatePickerRow(enrollment.getProgram().getIncidentDateLabel(), enrollment, enrollment.getIncidentDate()));
        }

        fragmentForm.setEnrollment(enrollment);
        fragmentForm.setDataEntryRows(dataEntryRows);

        return fragmentForm;
    }
}
