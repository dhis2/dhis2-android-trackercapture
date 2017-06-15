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

package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.DatePicker;

import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.views.CustomDatePickerDialog;
import org.hisp.dhis.android.trackercapture.R;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * Helper-class for showing DatePickers for selecting Enrollment date, then incident date,
 * and then triggering Enrollment-creation in a chain of events.
 */
public class EnrollmentDateSetterHelper {
    private final IEnroller enroller;
    private final Context context;
    private final boolean showIncidentDate;
    private final boolean enrollmentDatesInFuture;
    private final boolean incidentDatesInFuture;
    private final String enrollmentDateLabel;
    private final String incidentDateLabel;
    private TrackedEntityInstance trackedEntityInstance;
    private DateTime enrollmentDate;
    private DateTime incidentDate;

    public EnrollmentDateSetterHelper(IEnroller enroller, Context context, boolean showIncidentDate,
                                      boolean enrollmentDatesInFuture, boolean incidentDatesInFuture, String enrollmentDateLabel, String incidentDateLabel) {
        this.enroller = enroller;
        this.context = context;
        this.showIncidentDate = showIncidentDate;
        this.enrollmentDatesInFuture = enrollmentDatesInFuture;
        this.incidentDatesInFuture = incidentDatesInFuture;
        this.enrollmentDateLabel = enrollmentDateLabel;
        this.incidentDateLabel = incidentDateLabel;
    }

    public EnrollmentDateSetterHelper(TrackedEntityInstance trackedEntityInstance, IEnroller enroller, Context context, boolean showIncidentDate,
                                      boolean enrollmentDatesInFuture, boolean incidentDatesInFuture, String enrollmentDateLabel, String incidentDateLabel) {
        this.trackedEntityInstance = trackedEntityInstance;
        this.enroller = enroller;
        this.context = context;
        this.showIncidentDate = showIncidentDate;
        this.enrollmentDatesInFuture = enrollmentDatesInFuture;
        this.incidentDatesInFuture = incidentDatesInFuture;
        this.enrollmentDateLabel = enrollmentDateLabel;
        this.incidentDateLabel = incidentDateLabel;
    }

    public static void createEnrollment(TrackedEntityInstance trackedEntityInstance, IEnroller enroller, Context context, boolean showIncidentDate,
                                        boolean enrollmentDatesInFuture, boolean incidentDatesInFuture, String enrollmentDateLabel, String incidentDateLabel) {
        EnrollmentDateSetterHelper enrollmentDateSetterHelper = new EnrollmentDateSetterHelper(trackedEntityInstance, enroller, context, showIncidentDate, enrollmentDatesInFuture, incidentDatesInFuture, enrollmentDateLabel, incidentDateLabel);
        enrollmentDateSetterHelper.showEnrollmentDatePicker();
    }

    public static void createEnrollment(IEnroller enroller, Context context, boolean showIncidentDate,
                                        boolean enrollmentDatesInFuture, boolean incidentDatesInFuture, String enrollmentDateLabel, String incidentDateLabel) {
        EnrollmentDateSetterHelper enrollmentDateSetterHelper = new EnrollmentDateSetterHelper(enroller, context, showIncidentDate, enrollmentDatesInFuture, incidentDatesInFuture, enrollmentDateLabel, incidentDateLabel);
        enrollmentDateSetterHelper.showEnrollmentDatePicker();
    }

    private void showEnrollmentDatePicker() {
        enrollmentDate = new DateTime(1, 1, 1, 1, 0);
        LocalDate currentDate = new LocalDate();
        final CustomDatePickerDialog enrollmentDatePickerDialog =
                new CustomDatePickerDialog(context,
                        null, currentDate.getYear(),
                        currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());
        enrollmentDatePickerDialog.setPermanentTitle(context.getString(R.string.please_enter) + " " + enrollmentDateLabel);
        enrollmentDatePickerDialog.setCanceledOnTouchOutside(true);
        if(!enrollmentDatesInFuture) {
            enrollmentDatePickerDialog.getDatePicker().setMaxDate(DateTime.now().getMillis());
        }
        enrollmentDatePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatePicker dp = enrollmentDatePickerDialog.getDatePicker();
                        enrollmentDate = enrollmentDate.withYear(dp.getYear());
                        enrollmentDate = enrollmentDate.withMonthOfYear(dp.getMonth() + 1);
                        enrollmentDate = enrollmentDate.withDayOfMonth(dp.getDayOfMonth());

                        if (showIncidentDate) {
                            showIncidentDatePicker();
                        } else {
                            showEnrollmentFragment();
                        }
                    }
                });
        enrollmentDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        enrollmentDatePickerDialog.show();
    }

    private void showIncidentDatePicker() {
        LocalDate currentDate = new LocalDate();
        incidentDate = new DateTime(1, 1, 1, 1, 0);
        final CustomDatePickerDialog incidentDatePickerDialog =
                new CustomDatePickerDialog(context,
                        null, currentDate.getYear(),
                        currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());
        incidentDatePickerDialog.setPermanentTitle(context.getString(R.string.please_enter) + " " + incidentDateLabel);
        incidentDatePickerDialog.setCanceledOnTouchOutside(true);
        if(!incidentDatesInFuture) {
            incidentDatePickerDialog.getDatePicker().setMaxDate(DateTime.now().getMillis());
        }
        incidentDatePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatePicker dp = incidentDatePickerDialog.getDatePicker();
                        incidentDate = incidentDate.withYear(dp.getYear());
                        incidentDate = incidentDate.withMonthOfYear(dp.getMonth() + 1);
                        incidentDate = incidentDate.withDayOfMonth(dp.getDayOfMonth());
                        showEnrollmentFragment();
                    }
                });
        incidentDatePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        incidentDatePickerDialog.show();
    }

    private void showEnrollmentFragment() {
        enroller.showEnrollmentFragment(trackedEntityInstance, enrollmentDate, incidentDate);
    }
}
