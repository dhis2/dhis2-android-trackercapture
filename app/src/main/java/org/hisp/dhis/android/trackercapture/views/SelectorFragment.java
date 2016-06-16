package org.hisp.dhis.android.trackercapture.views;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.TrackerCaptureApp;
import org.hisp.dhis.android.trackercapture.presenters.SelectorPresenter;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.ui.fragments.BaseFragment;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.List;

import javax.inject.Inject;

public class SelectorFragment extends BaseFragment implements SelectorView {
    private static final String TAG = SelectorFragment.class.getSimpleName();
    private static final int ORG_UNIT_PICKER_ID = 0;
    private static final int PROGRAM_UNIT_PICKER_ID = 1;

    @Inject
    SelectorPresenter selectorPresenter;

    @Inject
    Logger logger;

    CoordinatorLayout coordinatorLayout;

    CardView bottomSheetView;

    BottomSheetBehavior<CardView> bottomSheetBehavior;

    View bottomSheetHeaderView;

    TextView selectedOrganisationUnit;
    TextView selectedProgram;

    OnCreateEnrollmentButtonClickListener onCreateEnrollmentButtonClickListener;

    // button which is shown only in case when all pickers are set
    FloatingActionButton createEnrollmentButton;

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
        setupFloatingActionButton(view);
        setupBottomSheet(view,savedInstanceState);

    }

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
    public void onResume() {
        super.onResume();
        selectorPresenter.attachView(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        selectorPresenter.detachView();
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

    private void setupFloatingActionButton(final View rootView) {
        onCreateEnrollmentButtonClickListener = new OnCreateEnrollmentButtonClickListener();

        createEnrollmentButton = (FloatingActionButton) rootView.findViewById(R.id.fab_create_event);
        createEnrollmentButton.setOnClickListener(onCreateEnrollmentButtonClickListener);

        // button visibility will be changed as soon as pickers are loaded
        createEnrollmentButton.setVisibility(View.INVISIBLE);
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

    @Override
    public void showProgressBar() {

    }

    @Override
    public void hideProgressBar() {

    }

    @Override
    public void showPickers(Picker picker) {

    }

    @Override
    public void showReportEntities(List<ReportEntity> reportEntities) {

    }

    @Override
    public void showNoOrganisationUnitsError() {

    }

    @Override
    public void showError(String message) {

    }

    @Override
    public void showUnexpectedError(String message) {

    }

    @Override
    public void onReportEntityDeletionError(ReportEntity failedEntity) {

    }

    @Override
    public void navigateToFormSectionActivity(Event event) {

    }

    @Override
    public String getPickerLabel(@PickerLabelId String pickerLabelId) {
        return null;
    }

    private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            int rightPadding;

            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                // state is expanded. move header out from below the FAB
                rightPadding = getResources().getDimensionPixelSize(R.dimen.keyline_default) + getFabSize() + getResources().getDimensionPixelSize(R.dimen.keyline_default);
                logger.d(TAG, "Bottom sheet expanded. Header padding: " + rightPadding + " px");
                setBottomSheetHeaderPadding(rightPadding);
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                rightPadding = getResources().getDimensionPixelSize(R.dimen.keyline_default);
                logger.d(TAG, "Bottom sheet is collapsed. Header padding: " + rightPadding + " px");
                setBottomSheetHeaderPadding(rightPadding);
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
            return createEnrollmentButton.getWidth();
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
