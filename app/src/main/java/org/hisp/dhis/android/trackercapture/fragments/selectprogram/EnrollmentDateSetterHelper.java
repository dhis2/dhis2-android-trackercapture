package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.DatePicker;

import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
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
        final DatePickerDialog enrollmentDatePickerDialog =
                new DatePickerDialog(context,
                        null, currentDate.getYear(),
                        currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());
        enrollmentDatePickerDialog.setTitle(context.getString(R.string.please_enter) + " " + enrollmentDateLabel);
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
        final DatePickerDialog incidentDatePickerDialog =
                new DatePickerDialog(context,
                        null, currentDate.getYear(),
                        currentDate.getMonthOfYear() - 1, currentDate.getDayOfMonth());
        incidentDatePickerDialog.setTitle(context.getString(R.string.please_enter) + " " + incidentDateLabel);
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
