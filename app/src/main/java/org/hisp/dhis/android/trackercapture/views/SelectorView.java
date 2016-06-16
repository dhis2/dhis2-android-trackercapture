package org.hisp.dhis.android.trackercapture.views;

import android.support.annotation.StringDef;

import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public interface SelectorView extends View {
    String ID_CHOOSE_ORGANISATION_UNIT = "chooseOrganisationUnit";
    String ID_CHOOSE_PROGRAM = "chooseProgram";
    String ID_NO_PROGRAMS = "noPrograms";

    void showProgressBar();

    void hideProgressBar();

    void showPickers(Picker picker);

    void showReportEntities(List<ReportEntity> reportEntities);

    void showNoOrganisationUnitsError();

    void showError(String message);

    void showUnexpectedError(String message);

    void onReportEntityDeletionError(ReportEntity failedEntity);

    void navigateToFormSectionActivity(Event event);

    String getPickerLabel(@PickerLabelId String pickerLabelId);

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            ID_CHOOSE_ORGANISATION_UNIT,
            ID_CHOOSE_PROGRAM,
            ID_NO_PROGRAMS
    })
    @interface PickerLabelId {
    }

}
