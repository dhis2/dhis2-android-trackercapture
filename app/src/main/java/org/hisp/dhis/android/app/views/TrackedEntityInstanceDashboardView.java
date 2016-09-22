package org.hisp.dhis.android.app.views;

import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;

import java.util.List;

public interface TrackedEntityInstanceDashboardView extends View {
        void showProfileRows(List<FormEntity> formEntities);
}
