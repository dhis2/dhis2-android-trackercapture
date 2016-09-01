package org.hisp.dhis.android.app.views;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.hisp.dhis.android.app.R;
import org.hisp.dhis.android.app.TrackerCaptureApp;
import org.hisp.dhis.android.app.presenters.DataEntryPresenter;
import org.hisp.dhis.client.sdk.ui.fragments.BaseFragment;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;
import org.hisp.dhis.client.sdk.ui.models.FormEntityAction;
import org.hisp.dhis.client.sdk.ui.rows.RowViewAdapter;
import org.hisp.dhis.client.sdk.ui.views.DividerDecoration;

import java.util.List;

import javax.inject.Inject;

import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

public class DataEntryFragment extends BaseFragment implements DataEntryView {
    private static final String ARG_ENROLLMENT_ID = "arg:enrollmentId";
    private static final String ARG_EVENT_ID = "arg:eventId";
    private static final String ARG_PROGRAM_ID = "arg:programId";
    private static final String ARG_PROGRAM_STAGE_ID = "arg:programStageId";
    private static final String ARG_PROGRAM_STAGE_SECTION_ID = "arg:programStageSectionId";

    @Inject
    DataEntryPresenter dataEntryPresenter;

    RecyclerView recyclerView;

    RowViewAdapter rowViewAdapter;

    public static DataEntryFragment newInstanceForEnrollment(@NonNull String enrollmentId,
                                                        @NonNull String programId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_ENROLLMENT_ID, enrollmentId);
        arguments.putString(ARG_PROGRAM_ID, programId);

        DataEntryFragment dataEntryFragment = new DataEntryFragment();
        dataEntryFragment.setArguments(arguments);

        return dataEntryFragment;
    }

    public static DataEntryFragment newInstanceForStage(@NonNull String eventId,
                                                        @NonNull String programStageId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_EVENT_ID, eventId);
        arguments.putString(ARG_PROGRAM_STAGE_ID, programStageId);

        DataEntryFragment dataEntryFragment = new DataEntryFragment();
        dataEntryFragment.setArguments(arguments);

        return dataEntryFragment;
    }

    public static DataEntryFragment newInstanceForSection(@NonNull String eventId,
                                                          @NonNull String programStageSectionId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_EVENT_ID, eventId);
        arguments.putString(ARG_PROGRAM_STAGE_SECTION_ID, programStageSectionId);

        DataEntryFragment dataEntryFragment = new DataEntryFragment();
        dataEntryFragment.setArguments(arguments);

        return dataEntryFragment;
    }

    private String getEventId() {
        return getArguments().getString(ARG_EVENT_ID, null);
    }

    private String getProgramStageId() {
        return getArguments().getString(ARG_PROGRAM_STAGE_ID, null);
    }

    private String getEnrollmentId() {
        return getArguments().getString(ARG_ENROLLMENT_ID, null);
    }

    private String getProgramId() {
        return getArguments().getString(ARG_PROGRAM_ID, null);
    }

    private String getProgramStageSectionId() {
        return getArguments().getString(ARG_PROGRAM_STAGE_SECTION_ID, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            ((TrackerCaptureApp) getActivity().getApplication())
                    .getFormComponent().inject(this);

            // attach view is called in this case from onCreate(),
            // in order to prevent unnecessary work which should be done
            // if case it will be i onResume()
            dataEntryPresenter.attachView(this);
        } catch (Exception e) {
            Log.e("DataEntryFragment", "Activity or Application is null. Vital resources have been killed.", e);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_data_entry, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rowViewAdapter = new RowViewAdapter(getChildFragmentManager());

        // Using ItemDecoration in order to implement divider
        DividerDecoration itemDecoration = new DividerDecoration(
                ContextCompat.getDrawable(getActivity(), R.drawable.divider));

        recyclerView = (RecyclerView) view;
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(rowViewAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // injection point was changed from onCreate() to onActivityCreated()
        // because od stupid fragment lifecycle
        // ((EventCaptureApp) getActivity().getApplication()).getFormComponent().inject(this);

        if (!isEmpty(getEnrollmentId())) {
            // Pass event id into presenter
            dataEntryPresenter.createDataEntryFormStage(getEnrollmentId(), getProgramId());
            return;
        }

        if (!isEmpty(getProgramStageSectionId())) {
            // Pass event id into presenter
            dataEntryPresenter.createDataEntryFormSection(getEventId(), getProgramStageSectionId());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataEntryPresenter.detachView();
    }

    @Override
    public void showDataEntryForm(List<FormEntity> formEntities, List<FormEntityAction> actions) {
        rowViewAdapter.swap(formEntities, actions);
    }

    @Override
    public void updateDataEntryForm(List<FormEntityAction> formEntityActions) {
        rowViewAdapter.update(formEntityActions);
    }
}