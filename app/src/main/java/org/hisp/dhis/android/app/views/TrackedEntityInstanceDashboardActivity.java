package org.hisp.dhis.android.app.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.hisp.dhis.android.app.ActivityComponent;
import org.hisp.dhis.android.app.R;
import org.hisp.dhis.android.app.TrackerCaptureApp;
import org.hisp.dhis.android.app.presenters.TrackedEntityInstanceDashboardPresenter;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;
import org.hisp.dhis.client.sdk.ui.rows.RowViewAdapter;
import org.hisp.dhis.client.sdk.ui.views.DividerDecoration;

import java.util.List;

import javax.inject.Inject;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;

public class TrackedEntityInstanceDashboardActivity extends AppCompatActivity implements TrackedEntityInstanceDashboardView {
    private static final String ARG_ENROLLMENT_UID = "arg:EnrollmentUid";

    @Inject
    TrackedEntityInstanceDashboardPresenter trackedEntityInstanceDashboardPresenter;

    RecyclerView profileRecyclerView;
    RowViewAdapter rowViewAdapter;

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

        setupToolbar();
        setUpView();

        ActivityComponent activityComponent = ((TrackerCaptureApp) getApplication()).getActivityComponent();

        // first time activity is created
        if (savedInstanceState == null) {
            // it means we found old component and we have to release it
            if (activityComponent != null) {
                // create new instance of component
                ((TrackerCaptureApp) getApplication()).releaseFormComponent();
            }

            activityComponent = ((TrackerCaptureApp) getApplication()).createActivityComponent();
        } else {
            activityComponent = ((TrackerCaptureApp) getApplication()).getActivityComponent();
        }


        // inject dependencies
        activityComponent.inject(this);

        trackedEntityInstanceDashboardPresenter.createDashboard(getEnrollmentUid());

    }

    @Override
    protected void onStart() {
        trackedEntityInstanceDashboardPresenter.attachView(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        trackedEntityInstanceDashboardPresenter.detachView();
        super.onStop();
    }

    private void setUpView() {
        profileRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_tei_dashboard);
        rowViewAdapter = new RowViewAdapter(getSupportFragmentManager(), RowViewAdapter.Type.DATA_VIEW);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        DividerDecoration itemDecoration = new DividerDecoration(
                ContextCompat.getDrawable(this, R.drawable.divider));

        profileRecyclerView.setLayoutManager(linearLayoutManager);
        profileRecyclerView.addItemDecoration(itemDecoration);
        profileRecyclerView.setAdapter(rowViewAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }


    @Override
    public void showProfileRows(List<FormEntity> formEntities) {
        rowViewAdapter.swap(formEntities);
    }
}
