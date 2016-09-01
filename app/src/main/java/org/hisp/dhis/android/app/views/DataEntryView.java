package org.hisp.dhis.android.app.views;

import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;
import org.hisp.dhis.client.sdk.ui.models.FormEntityAction;

import java.util.List;

public interface DataEntryView extends View {
    void showDataEntryForm(List<FormEntity> formEntities, List<FormEntityAction> actions);

    void updateDataEntryForm(List<FormEntityAction> formEntityActions);
}
