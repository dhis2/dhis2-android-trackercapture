package org.hisp.dhis.android.app.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import org.hisp.dhis.android.app.R;
import org.hisp.dhis.android.app.presenters.TrackedEntityInstanceDashboardPresenterImpl;

import javax.inject.Inject;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;

public class TrackedEntityInstanceDashboardActivity extends AppCompatActivity implements TrackedEntityInstanceDashboardView {
    private static final String ARG_ENROLLMENT_UID = "arg:EnrollmentUid";


    private TrackedEntityInstanceDashboardPresenterImpl trackedEntityInstanceDashboardPresenter;

    public static void navigateTo(Activity activity, String enrollmentUid) {
        isNull(activity, "activity must not be null");

        Intent intent = new Intent(activity, TrackedEntityInstanceDashboardActivity.class);
        intent.putExtra(ARG_ENROLLMENT_UID, enrollmentUid);
        activity.startActivity(intent);

    }

    private String getEnrollmentUid() {
        if (getIntent().getExtras() == null || getIntent().getExtras()
                .getString(ARG_ENROLLMENT_UID, null) == null) {
            throw new IllegalArgumentException("You must pass enrollment uid in intent extras");
        }

        return getIntent().getExtras().getString(ARG_ENROLLMENT_UID, null);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tei_dashboard);
    }
}
