package org.hisp.dhis.android.app.presenters;

import org.hisp.dhis.client.sdk.ui.bindings.presenters.Presenter;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;

import java.util.List;

public interface SelectorPresenter extends Presenter {
    void sync();

    void listPickers();

    void listEvents(String organisationUnitId, String programId);

    void createEvent(String organisationUnitId, String programId);

    void deleteEvent(ReportEntity reportEntity);

    void onPickersSelectionsChanged(List<Picker> pickerList);

    void handleError(final Throwable throwable);
}
