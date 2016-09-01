package org.hisp.dhis.android.app.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import org.hisp.dhis.android.app.TrackerCaptureApp;
import org.hisp.dhis.android.app.R;
import org.hisp.dhis.android.app.presenters.SelectorPresenter;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.ui.adapters.PickerAdapter;
import org.hisp.dhis.client.sdk.ui.adapters.ReportEntityAdapter;
import org.hisp.dhis.client.sdk.ui.fragments.BaseFragment;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;
import org.hisp.dhis.client.sdk.ui.views.AbsAnimationListener;
import org.hisp.dhis.client.sdk.ui.views.DividerDecoration;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;
import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

public class SelectorFragment extends BaseFragment implements SelectorView {
    private static final String TAG = SelectorFragment.class.getSimpleName();
    private static final int ORG_UNIT_PICKER_ID = 0;
    private static final int PROGRAM_UNIT_PICKER_ID = 1;
    private static final String STATE_IS_REFRESHING = "state:isRefreshing";
    private static final String LAYOUT_MANAGER_KEY = "LAYOUT_MANAGER_KEY";

    @Inject
    SelectorPresenter selectorPresenter;

    @Inject
    Logger logger;

    // button which is shown only in case when all pickers are set
    FloatingActionButton createEventButton;
    OnCreateEnrollmentButtonClickListener onCreateEnrollmentButtonClickListener;

    // pull-to-refresh
    SwipeRefreshLayout swipeRefreshLayout;
    BottomSheetBehavior<CardView> bottomSheetBehavior;

    // bottom sheet layout
    CoordinatorLayout coordinatorLayout;
    CardView bottomSheetView;

    // selected organisation unit, program and entity count
    TextView selectedOrganisationUnit;
    TextView selectedProgram;

    // list of pickers
    RecyclerView pickerRecyclerView;
    PickerAdapter pickerAdapter;

    // list of events
    RecyclerView reportEntityRecyclerView;
    ReportEntityAdapter reportEntityAdapter;
    View bottomSheetHeaderView;
    AlertDialog alertDialog;

    private static String getOrganisationUnitUid(List<Picker> pickers) {
        if (pickers != null && !pickers.isEmpty() &&
                pickers.get(ORG_UNIT_PICKER_ID).getSelectedChild() != null) {
            return pickers.get(ORG_UNIT_PICKER_ID).getSelectedChild().getId();
        }

        return null;
    }

    private static String getOrganisationUnitLabel(List<Picker> pickers) {
        if (pickers != null && !pickers.isEmpty() &&
                pickers.get(ORG_UNIT_PICKER_ID).getSelectedChild() != null) {
            return pickers.get(ORG_UNIT_PICKER_ID).getSelectedChild().getName();
        }

        return null;
    }

    private static String getProgramUid(List<Picker> pickers) {
        if (pickers != null && pickers.size() > 1 &&
                pickers.get(PROGRAM_UNIT_PICKER_ID).getSelectedChild() != null) {
            return pickers.get(PROGRAM_UNIT_PICKER_ID).getSelectedChild().getId();
        }

        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((TrackerCaptureApp) getActivity().getApplication())
                .getUserComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selector, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setupToolbar();
        setupBottomSheet(view, savedInstanceState);
        setupFloatingActionButton(view);
        setupSwipeRefreshLayout(view, savedInstanceState);
        setupPickerRecyclerView(view, savedInstanceState);
        setupReportEntityRecyclerView(view, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (pickerAdapter != null) {
            pickerAdapter.onSaveInstanceState(outState);
        }

        outState.putBoolean(STATE_IS_REFRESHING, swipeRefreshLayout.isRefreshing());

        outState.putParcelable(ReportEntityAdapter.REPORT_ENTITY_LIST_KEY, reportEntityAdapter.onSaveInstanceState());
        outState.putParcelable(LAYOUT_MANAGER_KEY, reportEntityRecyclerView.getLayoutManager().onSaveInstanceState());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        logger.d(TAG, "onResume()");
        selectorPresenter.attachView(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        logger.d(TAG, "onPause()");
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        selectorPresenter.detachView();
    }

    @Override
    public void showProgressBar() {
        logger.d(SelectorFragment.class.getSimpleName(), "showProgressBar()");

        // this workaround is necessary because of the message queue
        // implementation in android. If you will try to setRefreshing(true) right away,
        // this call will be placed in UI message queue by SwipeRefreshLayout BEFORE
        // message to hide progress bar which probably is created by layout
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    @Override
    public void hideProgressBar() {
        logger.d(SelectorFragment.class.getSimpleName(), "hideProgressBar()");
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void showPickers(Picker pickerTree) {
        if (pickerTree.getChildren().isEmpty()) {
            TextView textView = (TextView) getActivity()
                    .findViewById(R.id.textview_error_no_org_units);
            //in the case that error was shown and the user was assigned organisation units,
            //hide the error message :
            textView.setVisibility(View.GONE);
        }
        //and try toshow the pickers:
        pickerAdapter.swapData(pickerTree);
    }

    @Override
    public void showReportEntities(List<ReportEntity> reportEntities) {
        logger.d(TAG, "amount of report entities: " + reportEntities.size());
        reportEntityAdapter.swapData(reportEntities);

        if (pickerAdapter != null) {
            updateLabels(pickerAdapter.getData());
        }
    }

    private boolean reportEntitiesIsEmpty() {
        return reportEntityAdapter == null || reportEntityAdapter.getItemCount() == 0;
    }

    @Override
    public void showNoOrganisationUnitsError() {
        //pickerAdapter.swapData(null);
        TextView textView = (TextView) getActivity()
                .findViewById(R.id.textview_error_no_org_units);
        textView.setVisibility(View.VISIBLE);
        textView.setText(getString(R.string.no_organisation_units));
    }

    @Override
    public void showError(String message) {
        showErrorDialog(getString(R.string.title_error), message);
    }

    @Override
    public void showUnexpectedError(String message) {
        showErrorDialog(getString(R.string.title_error_unexpected), message);
    }

    @Override
    public void onReportEntityDeletionError(ReportEntity reportEntity) {
        Toast.makeText(getContext(), R.string.report_entity_deletion_error, Toast.LENGTH_SHORT).show();
        reportEntityAdapter.addItem(reportEntity);
    }

    @Override
    public void navigateToFormSectionActivity(Enrollment enrollment) {
        logger.d(TAG, String.format("Enrollment with uid=%s is created", enrollment.getUId()));
        EnrollmentFormActivity.navigateToNewEnrollment(getActivity(), enrollment.getUId());
    }

    @Override
    public String getPickerLabel(@PickerLabelId String pickerLabelId) {
        isNull(pickerLabelId, "pickerLabelId must not be null");

        switch (pickerLabelId) {
            case ID_CHOOSE_ORGANISATION_UNIT:
                return getString(R.string.choose_organisation_unit);
            case ID_CHOOSE_PROGRAM:
                return getString(R.string.choose_program);
            case ID_NO_PROGRAMS:
                return getString(R.string.no_programs);

            default:
                throw new IllegalArgumentException("Unsupported PickerLabelId");
        }
    }

    @Override
    public boolean onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return false;
        }

        return true;
    }

    private void setupToolbar() {
        Drawable buttonDrawable = DrawableCompat.wrap(ContextCompat
                .getDrawable(getActivity(), R.drawable.ic_menu));
        DrawableCompat.setTint(buttonDrawable, ContextCompat
                .getColor(getContext(), android.R.color.white));

        if (getParentToolbar() != null) {
            getParentToolbar().inflateMenu(R.menu.menu_refresh);
            getParentToolbar().setNavigationIcon(buttonDrawable);
            getParentToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return SelectorFragment.this.onMenuItemClick(item);
                }
            });
        }
    }

    private void setupFloatingActionButton(final View rootView) {
        onCreateEnrollmentButtonClickListener = new OnCreateEnrollmentButtonClickListener();

        createEventButton = (FloatingActionButton) rootView.findViewById(R.id.fab_create_event);
        createEventButton.setOnClickListener(onCreateEnrollmentButtonClickListener);

        // button visibility will be changed as soon as pickers are loaded
        createEventButton.setVisibility(View.INVISIBLE);
    }

    private void setupSwipeRefreshLayout(final View rootView, final Bundle savedInstanceState) {
        swipeRefreshLayout = (SwipeRefreshLayout) rootView
                .findViewById(R.id.swiperefreshlayout_selector);
        swipeRefreshLayout.setColorSchemeResources(R.color.color_primary_default);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                selectorPresenter.sync();
            }
        });

        if (savedInstanceState != null) {
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(savedInstanceState
                            .getBoolean(STATE_IS_REFRESHING, false));
                }
            });
        }
    }

    private void setupPickerRecyclerView(final View rootView, final Bundle savedInstanceState) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        pickerAdapter = new PickerAdapter(getChildFragmentManager(), getActivity());
        pickerAdapter.setOnPickerListChangeListener(new PickerAdapter.OnPickerListChangeListener() {
            @Override
            public void onPickerListChanged(List<Picker> pickers) {
                SelectorFragment.this.onPickerListChanged(pickers);
            }
        });

        pickerRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_pickers);
        pickerRecyclerView.setLayoutManager(layoutManager);
        pickerRecyclerView.setAdapter(pickerAdapter);
        pickerRecyclerView.setItemAnimator(new DefaultItemAnimator());

        if (savedInstanceState != null) {
            pickerAdapter.onRestoreInstanceState(savedInstanceState);
        } else {
            selectorPresenter.listPickers();
        }
    }

    private void setupReportEntityRecyclerView(View view, Bundle savedInstanceState) {
        reportEntityRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview_events);

        setupAdapter();
        reportEntityRecyclerView.setAdapter(reportEntityAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        reportEntityRecyclerView.setLayoutManager(layoutManager);
        if (savedInstanceState != null) {
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(LAYOUT_MANAGER_KEY));
        }

        if (savedInstanceState != null) {
            reportEntityAdapter.onRestoreInstanceState(
                    savedInstanceState.getBundle(ReportEntityAdapter.REPORT_ENTITY_LIST_KEY));
        }

        reportEntityRecyclerView.setItemAnimator(new DefaultItemAnimator());
        reportEntityRecyclerView.addItemDecoration(new DividerDecoration(
                ContextCompat.getDrawable(getActivity(), R.drawable.divider)));
    }

    private void setupAdapter() {
        reportEntityAdapter = new ReportEntityAdapter(getActivity());
        reportEntityAdapter.setOnReportEntityInteractionListener(new ReportEntityAdapter.OnReportEntityInteractionListener() {
            @Override
            public void onReportEntityClicked(ReportEntity reportEntity) {
                SelectorFragment.this.onReportEntityClicked(reportEntity);
            }

            @Override
            public void onDeleteReportEntity(ReportEntity reportEntity) {
                logger.d(TAG, "ReportEntity id to be deleted: " + reportEntity.getId());
                selectorPresenter.deleteEnrollment(reportEntity);

                if (pickerAdapter != null) {
                    updateLabels(pickerAdapter.getData());
                }
            }
        });
    }

    private void setupBottomSheet(View view, Bundle savedInstanceState) {
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorlayout_selector);
        bottomSheetView = (CardView) view.findViewById(R.id.card_view_bottom_sheet);

        bottomSheetHeaderView = view.findViewById(R.id.bottom_sheet_header_container);
        selectedOrganisationUnit = (TextView) view.findViewById(R.id.textview_organisation_unit);
        selectedProgram = (TextView) view.findViewById(R.id.textview_program);

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setPeekHeight(getResources()
                .getDimensionPixelSize(R.dimen.bottomsheet_peek_height));

        if (savedInstanceState == null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
        bottomSheetHeaderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
    }

    private void onReportEntityClicked(ReportEntity reportEntity) {
//        FormSectionActivity.navigateToExistingEnrollment(getActivity(), reportEntity.getId());
    }

    private boolean onMenuItemClick(MenuItem item) {
        logger.d(SelectorFragment.class.getSimpleName(), "onMenuItemClick()");

        switch (item.getItemId()) {
            case R.id.action_refresh: {
                selectorPresenter.sync();
                return true;
            }
        }
        return false;
    }

    /* change visibility of floating action button*/
    private void onPickerListChanged(List<Picker> pickers) {

        onCreateEnrollmentButtonClickListener.setPickers(pickers);
        if (areAllPickersPresent(pickers)) {
            showCreateEnrollmentButton();

            // load existing enrollments
            selectorPresenter.listEnrollments(getOrganisationUnitUid(pickers), getProgramUid(pickers));
        } else {
            hideCreateEnrollmentButton();
            //This is uncommented, because it introduces buggy behaviour to the bottomSheet.
            //The bottom sheet is opened, but the pickers don't show unless the user clicks on the position where they should be shown.
            //showBottomSheet();

            // clear out list of existing events
            if (reportEntityAdapter != null) {
                reportEntityAdapter.swapData(null);
            }
        }

        updateLabels(pickers);

        selectorPresenter.onPickersSelectionsChanged(pickers);
    }

    /* check if organisation unit and program are selected */
    private boolean areAllPickersPresent(List<Picker> pickers) {
        return pickers != null && pickers.size() > 1 &&
                pickers.get(ORG_UNIT_PICKER_ID) != null &&
                pickers.get(ORG_UNIT_PICKER_ID).getSelectedChild() != null &&
                pickers.get(PROGRAM_UNIT_PICKER_ID) != null &&
                pickers.get(PROGRAM_UNIT_PICKER_ID).getSelectedChild() != null;
    }

    private void showCreateEnrollmentButton() {
        if (!createEventButton.isShown()) {
            createEventButton.setVisibility(View.VISIBLE);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(createEventButton, "scaleX", 0, 1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(createEventButton, "scaleY", 0, 1);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(new OvershootInterpolator());
            animSetXY.setDuration(256);
            animSetXY.start();
        }

        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }, 256);
        }
    }

    private void hideCreateEnrollmentButton() {
        if (createEventButton.isShown()) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(createEventButton, "scaleX", 1, 0);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(createEventButton, "scaleY", 1, 0);
            AnimatorSet animSetXY = new AnimatorSet();
            animSetXY.playTogether(scaleX, scaleY);
            animSetXY.setInterpolator(new AccelerateInterpolator());
            animSetXY.setDuration(256);
            animSetXY.addListener(new AbsAnimationListener() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    createEventButton.setVisibility(View.INVISIBLE);
                }
            });
            animSetXY.start();
        }
    }

    private void hideBottomSheet() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void showBottomSheet() {
        if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void updateLabels(List<Picker> pickers) {
        String orgUnitLabel;
        String programLabel;

        if (!isEmpty(getOrganisationUnitLabel(pickers))) {
            orgUnitLabel = String.format(Locale.getDefault(), "%s: %s",
                    getString(R.string.organisation_unit), getOrganisationUnitLabel(pickers));
        } else {
            orgUnitLabel = String.format(Locale.getDefault(), "%s: %s",
                    getString(R.string.organisation_unit), getString(R.string.none));
        }

        if (!isEmpty(getProgramLabel(pickers))) {
            programLabel = getProgramLabel(pickers);
        } else {
            programLabel = String.format(Locale.getDefault(), "%s: %s",
                    getString(R.string.program), getString(R.string.none));
        }

        selectedOrganisationUnit.setText(orgUnitLabel);
        selectedProgram.setText(programLabel);
    }

    private String getProgramLabel(List<Picker> pickers) {
        if (pickers != null && pickers.size() > 1 &&
                pickers.get(PROGRAM_UNIT_PICKER_ID).getSelectedChild() != null) {

            String programLabel = pickers.get(PROGRAM_UNIT_PICKER_ID).getSelectedChild().getName();

            if (!reportEntitiesIsEmpty()) {
                programLabel = String.format(Locale.getDefault(), "%s (%s)", programLabel, reportEntityAdapter.getItemCount());
            }

            return programLabel;
        }

        return null;
    }

    private void showErrorDialog(String title, String message) {
        if (alertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(R.string.option_confirm, null);
            alertDialog = builder.create();
        }
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.show();
    }

    private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            try {
                int defaultPadding = getResources().getDimensionPixelSize(R.dimen.keyline_default);
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // state is expanded. move header out from below the FAB
                    setBottomSheetHeaderPadding(defaultPadding + getFabSize() + defaultPadding);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    setBottomSheetHeaderPadding(defaultPadding);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to retrieve Resources. Probable cause: Activity no longer in " +
                        "view or Fragment is not attached", e);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            // not interesting
        }

        private void setBottomSheetHeaderPadding(int rightPadding) {
            bottomSheetHeaderView.setPadding(bottomSheetHeaderView.getPaddingLeft(),
                    bottomSheetHeaderView.getPaddingTop(),
                    rightPadding,
                    bottomSheetHeaderView.getPaddingBottom());
        }

        private int getFabSize() {
            return createEventButton.getWidth();
        }
    }

    private class OnCreateEnrollmentButtonClickListener implements View.OnClickListener {
        private List<Picker> pickers;

        @Override
        public void onClick(View view) {
            String orgUnitUid = getOrganisationUnitUid(pickers);
            String programUid = getProgramUid(pickers);

            if (orgUnitUid != null && programUid != null) {
                selectorPresenter.createEnrollment(orgUnitUid, programUid);
            }
        }

        public void setPickers(List<Picker> pickers) {
            this.pickers = pickers;
        }
    }
}
