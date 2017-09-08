/*
 *  Copyright (c) 2016, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.android.trackercapture.fragments.programoverview;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.raizlabs.android.dbflow.structure.Model;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.DhisApi;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.loaders.DbLoader;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramRuleAction;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.Relationship;
import org.hisp.dhis.android.sdk.persistence.models.RelationshipType;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentRepository;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventRepository;
import org.hisp.dhis.android.sdk.synchronization.data.faileditem.FailedItemRepository;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.enrollment.IEnrollmentRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.trackedentityinstance
        .ITrackedEntityInstanceRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.trackedentityinstance
        .SyncTrackedEntityInstanceUseCase;
import org.hisp.dhis.android.sdk.ui.activities.OnBackPressedListener;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IndicatorRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.PlainTextRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.OnDetailedInfoButtonClick;
import org.hisp.dhis.android.sdk.ui.dialogs.ProgramDialogFragment;
import org.hisp.dhis.android.sdk.ui.fragments.common.AbsProgramRuleFragment;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.ui.views.FontTextView;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.sdk.utils.comparators.EnrollmentDateComparator;
import org.hisp.dhis.android.sdk.utils.services.ProgramRuleService;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.activities.HolderActivity;
import org.hisp.dhis.android.trackercapture.fragments.programoverview
        .registerrelationshipdialogfragment.RegisterRelationshipDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.EnrollmentDateSetterHelper;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.IEnroller;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs
        .ItemStatusDialogFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.ProgramAdapter;
import org.hisp.dhis.android.trackercapture.ui.adapters.ProgramStageAdapter;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.OnProgramStageEventClick;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageEventRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageLabelRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageRow;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ProgramOverviewFragment extends AbsProgramRuleFragment implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        ProgramDialogFragment.OnOptionSelectedListener,
        LoaderManager.LoaderCallbacks<ProgramOverviewFragmentForm>,
        AdapterView.OnItemSelectedListener, SwipeRefreshLayout.OnRefreshListener, IEnroller,
        OnBackPressedListener {

    public static final String CLASS_TAG = ProgramOverviewFragment.class.getSimpleName();
    private static final String STATE = "state:UpcomingEventsFragment";
    private static final int LOADER_ID = 578922123;

    private static final String EXTRA_ARGUMENTS = "extra:Arguments";
    private static final String EXTRA_SAVED_INSTANCE_STATE = "extra:savedInstanceState";

    private static final String ORG_UNIT_ID = "extra:orgUnitId";
    private static final String PROGRAM_ID = "extra:ProgramId";
    private static final String TRACKEDENTITYINSTANCE_ID = "extra:TrackedEntityInstanceId";

    private ListView listView;
    private ProgressBar mProgressBar;
    private ProgramStageAdapter adapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private View mSpinnerContainer;
    private Spinner mSpinner;
    private ProgramAdapter mSpinnerAdapter;

    private LinearLayout enrollmentLayout;
    private TextView enrollmentDateLabel;
    private TextView enrollmentDateValue;
    private TextView incidentDateLabel;
    private TextView incidentDateValue;
    private TextView noActiveEnrollment;

    private LinearLayout missingEnrollmentLayout;
    private FloatingActionButton newEnrollmentButton;

    private CardView profileCardView;
    private CardView enrollmentCardview;

    private CardView programIndicatorCardView;
    private CardView eventsCardView;

    private ImageButton followupButton;
    private ImageButton profileButton;
    private ImageView enrollmentServerStatus;
    private Button completeButton;
    private Button reOpenButton;
    private Button terminateButton;

    private TextView attribute1Label;
    private TextView attribute1Value;
    private TextView attribute2Label;
    private TextView attribute2Value;

    private LinearLayout relationshipsLinearLayout;
    private Button newRelationshipButton;
    private Button refreshRelationshipButton;

    private ProgramOverviewFragmentState mState;
    private ProgramOverviewFragmentForm mForm;

    private OnProgramStageEventClick eventLongPressed;

    public ProgramOverviewFragment() {
        setProgramRuleFragmentHelper(new ProgramOverviewRuleHelper(this));
    }

    public static ProgramOverviewFragment newInstance(String orgUnitId, String programId,
            long trackedEntityInstanceId) {
        ProgramOverviewFragment fragment = new ProgramOverviewFragment();
        Bundle args = new Bundle();
        args.putString(ORG_UNIT_ID, orgUnitId);
        args.putString(PROGRAM_ID, programId);
        args.putLong(TRACKEDENTITYINSTANCE_ID, trackedEntityInstanceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, getArguments(), this);

        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }

        detachSpinner();
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        getProgramRuleFragmentHelper().recycle();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_programoverview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        listView = (ListView) view.findViewById(R.id.listview);
        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_programoverview_header, listView, false
        );

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(
                org.hisp.dhis.android.sdk.R.id.swipe_to_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(org.hisp.dhis.android.sdk.R.color.Green,
                org.hisp.dhis.android.sdk.R.color.Blue, org.hisp.dhis.android.sdk.R.color.orange);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        relationshipsLinearLayout = (LinearLayout) header.findViewById(
                R.id.relationships_linearlayout);

        refreshRelationshipButton = (Button) header.findViewById(R.id.pullrelationshipbutton);
        refreshRelationshipButton.setOnClickListener(this);
        newRelationshipButton = (Button) header.findViewById(R.id.addrelationshipbutton);
        newRelationshipButton.setOnClickListener(this);

        mProgressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        adapter = new ProgramStageAdapter(getLayoutInflater(savedInstanceState));
        listView.addHeaderView(header, CLASS_TAG, false);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        registerForContextMenu(listView);

        enrollmentServerStatus = (ImageView) header.findViewById(R.id.enrollmentstatus);
        enrollmentLayout = (LinearLayout) header.findViewById(R.id.enrollmentLayout);
        enrollmentDateLabel = (TextView) header.findViewById(R.id.dateOfEnrollmentLabel);
        enrollmentDateValue = (TextView) header.findViewById(R.id.dateOfEnrollmentValue);
        incidentDateLabel = (TextView) header.findViewById(R.id.dateOfIncidentLabel);
        incidentDateValue = (TextView) header.findViewById(R.id.dateOfIncidentValue);
        profileCardView = (CardView) header.findViewById(R.id.profile_cardview);
        enrollmentCardview = (CardView) header.findViewById(R.id.enrollment_cardview);
        noActiveEnrollment = (TextView) header.findViewById(R.id.noactiveenrollment);
        programIndicatorCardView = (CardView) header.findViewById(R.id.programindicators_cardview);
        eventsCardView = (CardView) header.findViewById(R.id.events_cardview);

        completeButton = (Button) header.findViewById(R.id.complete);
        reOpenButton = (Button) header.findViewById(R.id.re_open);
        terminateButton = (Button) header.findViewById(R.id.terminate);
        followupButton = (ImageButton) header.findViewById(R.id.followupButton);
        profileButton = (ImageButton) header.findViewById(R.id.profile_button);
        completeButton.setOnClickListener(this);
        reOpenButton.setOnClickListener(this);
        terminateButton.setOnClickListener(this);
        followupButton.setOnClickListener(this);
        followupButton.setVisibility(View.GONE);
        profileButton.setOnClickListener(this);
        profileCardView.setOnClickListener(this);
        enrollmentServerStatus.setOnClickListener(this);
        enrollmentLayout.setOnClickListener(this);

        missingEnrollmentLayout = (LinearLayout) header.findViewById(R.id.missingenrollmentlayout);
        newEnrollmentButton = (FloatingActionButton) header.findViewById(R.id.newenrollmentbutton);
        newEnrollmentButton.setOnClickListener(this);

        attribute1Label = (TextView) header.findViewById(R.id.headerItem1label);
        attribute1Value = (TextView) header.findViewById(R.id.headerItem1value);
        attribute2Label = (TextView) header.findViewById(R.id.headerItem2label);
        attribute2Value = (TextView) header.findViewById(R.id.headerItem2value);

        Bundle fragmentArguments = getArguments();

        if (savedInstanceState != null &&
                savedInstanceState.getParcelable(STATE) != null) {
            mState = savedInstanceState.getParcelable(STATE);
        }
        if (mState == null) {
            mState = new ProgramOverviewFragmentState();
            OrganisationUnit ou = MetaDataController.getOrganisationUnit(
                    fragmentArguments.getString(ORG_UNIT_ID));
            Program program = MetaDataController.getProgram(
                    fragmentArguments.getString(PROGRAM_ID));
            mState.setOrgUnit(ou.getId(), ou.getLabel());
            mState.setProgram(program.getUid(), program.getName());
            mState.setTrackedEntityInstance(
                    fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1));
        }
        attachSpinner();
        mSpinnerAdapter.swapData(MetaDataController.getProgramsForOrganisationUnit
                (fragmentArguments.getString(ORG_UNIT_ID),
                        ProgramType.WITH_REGISTRATION));

        onRestoreState(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.getEventBus().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.getEventBus().register(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            getActivity().finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void onRestoreState(boolean hasPrograms) {

        ProgramOverviewFragmentState backedUpState = new ProgramOverviewFragmentState(mState);
        if (!backedUpState.isProgramEmpty()) {
            onProgramSelected(
                    backedUpState.getProgramId(),
                    backedUpState.getProgramName()
            );
        } else {
            //todo
        }
    }

    private ActionBar getActionBar() {
        if (getActivity() != null &&
                getActivity() instanceof AppCompatActivity) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            throw new IllegalArgumentException("Fragment should be attached to ActionBarActivity");
        }
    }

    private Toolbar getActionBarToolbar() {
        if (isAdded() && getActivity() != null) {
            return (Toolbar) getActivity().findViewById(R.id.toolbar);
        } else {
            throw new IllegalArgumentException("Fragment should be attached to MainActivity");
        }
    }

    private int getSpinnerIndex(String programName) {
        int index = -1;
        for (int i = 0; i < mSpinnerAdapter.getCount(); i++) {
            Program program = (Program) mSpinnerAdapter.getItem(i);
            if (program.getName().equals(programName)) {
                index = i;
            }
        }
        return index;
    }

    private void attachSpinner() {
        if (!isSpinnerAttached()) {
            Toolbar toolbar = getActionBarToolbar();

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            mSpinnerContainer = inflater.inflate(
                    org.hisp.dhis.android.sdk.R.layout.toolbar_spinner_simple, toolbar, false);

            ActionBar.LayoutParams lp = new ActionBar.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            toolbar.addView(mSpinnerContainer, lp);

            mSpinnerAdapter = new ProgramAdapter(inflater);

            mSpinner = (Spinner) mSpinnerContainer.findViewById(
                    org.hisp.dhis.android.sdk.R.id.toolbar_spinner);
            mSpinner.setAdapter(mSpinnerAdapter);
            mSpinner.post(new Runnable() {
                public void run() {
                    if (mSpinner != null) {
                        mSpinner.setOnItemSelectedListener(ProgramOverviewFragment.this);
                    }
                }
            });
        }
    }

    private void detachSpinner() {
        if (isSpinnerAttached()) {
            if (mSpinnerContainer != null) {
                ((ViewGroup) mSpinnerContainer.getParent()).removeView(mSpinnerContainer);
                mSpinnerContainer = null;
                mSpinner = null;
                if (mSpinnerAdapter != null) {
                    mSpinnerAdapter.swapData(null);
                    mSpinnerAdapter = null;
                }
            }
        }
    }

    private boolean isSpinnerAttached() {
        return mSpinnerContainer != null;
    }

    public void onProgramSelected(String programId, String programName) {
        mState.setProgram(programId, programName);
        Bundle args = getArguments();
        args.putString(PROGRAM_ID, programId);
        clearViews();
        getLoaderManager().restartLoader(LOADER_ID, args, this);
    }

    @Override
    public Loader<ProgramOverviewFragmentForm> onCreateLoader(int id, Bundle args) {
        if (LOADER_ID == id && isAdded()) {
            List<Class<? extends Model>> modelsToTrack = new ArrayList<>();
            modelsToTrack.add(Event.class);
            modelsToTrack.add(Enrollment.class);
            modelsToTrack.add(TrackedEntityInstance.class);
            modelsToTrack.add(TrackedEntityAttributeValue.class);
            modelsToTrack.add(Relationship.class);
            modelsToTrack.add(FailedItem.class);
            return new DbLoader<>(
                    getActivity().getBaseContext(), modelsToTrack,
                    new ProgramOverviewFragmentQuery(args.getString(PROGRAM_ID),
                            args.getLong(TRACKEDENTITYINSTANCE_ID, -1)));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<ProgramOverviewFragmentForm> loader,
            ProgramOverviewFragmentForm data) {
        if (LOADER_ID == loader.getId()) {
            mForm = data;
            mProgressBar.setVisibility(View.GONE);
            setRefreshing(false);

            mSpinner.setSelection(getSpinnerIndex(mState.getProgramName()));

            if(mForm!=null){
                setRelationships(
                        getLayoutInflater(getArguments().getBundle(EXTRA_SAVED_INSTANCE_STATE)));
            }

            LinearLayout programEventsLayout =
                    (LinearLayout) eventsCardView.findViewById(
                            R.id.programeventlayout);

            LinearLayout programIndicatorLayout =
                    (LinearLayout) programIndicatorCardView.findViewById(
                            R.id.programindicatorlayout);

            initializeEventsViews(programEventsLayout);
            initializeIndicatorViews(programIndicatorLayout);

            if (mForm == null || mForm.getEnrollment() == null) {
                showNoActiveEnrollment(mForm);
                return;
            } else {
                enrollmentLayout.setVisibility(View.VISIBLE);
                missingEnrollmentLayout.setVisibility(View.GONE);
                profileCardView.setClickable(
                        true); //is set to false when TEI doesn't have an applicable enrollment.
                // todo why?
                profileButton.setClickable(true);
            }
            enrollmentDateLabel.setText(data.getDateOfEnrollmentLabel());
            enrollmentDateValue.setText(data.getDateOfEnrollmentValue());

            if (!(data.getProgram().getDisplayIncidentDate())) {
                incidentDateValue.setVisibility(View.GONE);
                incidentDateLabel.setVisibility(View.GONE);
            } else {
                incidentDateLabel.setText(data.getIncidentDateLabel());
                incidentDateValue.setText(data.getIncidentDateValue());
            }
            FailedItem failedItem = TrackerController.getFailedItem(FailedItem.ENROLLMENT,
                    mForm.getEnrollment().getLocalId());

            if (failedItem != null && failedItem.getHttpStatusCode() >= 0) {
                enrollmentServerStatus.setImageResource(R.drawable.ic_event_error);
            } else if (!mForm.getEnrollment().isFromServer()) {
                enrollmentServerStatus.setImageResource(R.drawable.ic_legacy_offline);
            } else {
                enrollmentServerStatus.setImageResource(R.drawable.ic_from_server);
            }

            refreshRelationshipButton.setEnabled(mForm.getEnrollment().isFromServer());

            if (mForm.getEnrollment().getStatus().equals(Enrollment.CANCELLED)) {
                setTerminated();
            }

            if (mForm.getEnrollment().getFollowup()) {
                setFollowupButton(true);
            }

            if (data.getAttribute1Label() == null || data.getAttribute1Value() == null) {
                attribute1Label.setVisibility(View.GONE);
                attribute1Value.setVisibility(View.GONE);
            } else {
                attribute1Label.setText(data.getAttribute1Label());
                attribute1Value.setText(data.getAttribute1Value());
            }

            if (data.getAttribute2Label() == null || data.getAttribute2Value() == null) {
                attribute2Label.setVisibility(View.GONE);
                attribute2Value.setVisibility(View.GONE);
            } else {
                attribute2Label.setText(data.getAttribute2Label());
                attribute2Value.setText(data.getAttribute2Value());
            }

            final Map<Long, FailedItem> failedEvents = getFailedEvents();

            for (ProgramStageRow row : data.getProgramStageRows()) {
                if (row instanceof ProgramStageLabelRow) {
                    ProgramStageLabelRow stageRow = (ProgramStageLabelRow) row;
                    if (stageRow.getProgramStage().getRepeatable()) {
                        stageRow.setButtonListener(this);
                    } else {
                        if (stageRow.getEventRows().size()
                                < 1) { // if stage is not autogen and not repeatable, allow user
                            // to create exactly one event
                            stageRow.setButtonListener(this);
                        }
                    }

                } else if (row instanceof ProgramStageEventRow) {
                    final ProgramStageEventRow eventRow = (ProgramStageEventRow) row;

                    FailedItem failedItem1 = TrackerController.getFailedItem(FailedItem.EVENT,
                            eventRow.getEvent().getLocalId());

                    if (failedItem1 != null && failedItem1.getHttpStatusCode() >= 0) {
                        eventRow.setHasFailed(true);
                        eventRow.setMessage(failedEvents.get(
                                eventRow.getEvent().getLocalId()).getErrorMessage());
                    } else if (eventRow.getEvent().isFromServer()) {
                        eventRow.setSynchronized(true);
                        eventRow.setMessage(getString(R.string.status_sent_description));
                    } else {
                        eventRow.setSynchronized(false);
                        eventRow.setMessage(getString(R.string.status_offline_description));
                    }
                }
            }
            for (IndicatorRow indicatorRow : mForm.getProgramIndicatorRows().values()) {
                View view = indicatorRow.getView(getChildFragmentManager(),
                        getLayoutInflater(getArguments()), null, programIndicatorLayout);
                programIndicatorLayout.addView(view);
            }
            for (ProgramStageRow programStageRow :data.getProgramStageRows()) {
                    ProgramStageRow programStageEventRow = programStageRow;
                    View view = programStageEventRow.getView(getLayoutInflater(getArguments()),
                            null, programEventsLayout);
                    programEventsLayout.addView(view);
            }
            evaluateAndApplyProgramRules();
        }
    }

    private void initializeEventsViews(LinearLayout programEventsLayout) {
        programEventsLayout.removeAllViews();
        FlowLayout keyValueLayout = (FlowLayout) eventsCardView.findViewById(
                R.id.keyvalueeventlayout);
        keyValueLayout.removeAllViews();
        LinearLayout displayTextLayout = (LinearLayout) eventsCardView.findViewById(
                R.id.texteventlayout);
        displayTextLayout.removeAllViews();
        programEventsLayout.removeAllViews();
    }

    private void initializeIndicatorViews(LinearLayout programIndicatorLayout) {
        programIndicatorLayout.removeAllViews();
        FlowLayout keyValueLayout = (FlowLayout) programIndicatorCardView.findViewById(
                R.id.keyvaluelayout);
        keyValueLayout.removeAllViews();
        LinearLayout displayTextLayout = (LinearLayout) programIndicatorCardView.findViewById(
                R.id.textlayout);
        displayTextLayout.removeAllViews();
        programIndicatorLayout.removeAllViews();
    }

    /**
     * Inflates views and adds them to linear layout for relationships, sort of like a listview, but
     * inside another listview
     */
    public void setRelationships(LayoutInflater inflater) {
        relationshipsLinearLayout.removeAllViews();
        if (mForm.getTrackedEntityInstance() != null
                && mForm.getTrackedEntityInstance().getRelationships() != null) {
            ListIterator<Relationship> it =
                    mForm.getTrackedEntityInstance().getRelationships().listIterator();
            while (it.hasNext()) {
                final Relationship relationship = it.next();
                if (relationship == null) {
                    continue;
                }
                LinearLayout ll = (LinearLayout) inflater.inflate(
                        R.layout.listview_row_relationship, null);
                FontTextView currentTeiRelationshipLabel = (FontTextView) ll.findViewById(
                        R.id.current_tei_relationship_label);
                FontTextView relativeLabel = (FontTextView) ll.findViewById(
                        R.id.relative_relationship_label);
                Button deleteButton = (Button) ll.findViewById(R.id.delete_relationship);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showConfirmDeleteRelationshipDialog(relationship,
                                mForm.getTrackedEntityInstance(), getActivity());
                    }
                });
                RelationshipType relationshipType = MetaDataController.getRelationshipType(
                        relationship.getRelationship());

                if (relationshipType != null) {

                    /* establishing if the relative is A or B in Relationship Type */
                    final TrackedEntityInstance relative;
                    if (mForm.getTrackedEntityInstance().getTrackedEntityInstance() != null &&
                            mForm.getTrackedEntityInstance().getTrackedEntityInstance().equals(
                                    relationship.getTrackedEntityInstanceA())) {

                        currentTeiRelationshipLabel.setText(relationshipType.getaIsToB());
                        relative = TrackerController.getTrackedEntityInstance(
                                relationship.getTrackedEntityInstanceB());

                    } else if (mForm.getTrackedEntityInstance().getTrackedEntityInstance() != null
                            &&
                            mForm.getTrackedEntityInstance().getTrackedEntityInstance().equals(
                                    relationship.getTrackedEntityInstanceB())) {

                        currentTeiRelationshipLabel.setText(relationshipType.getbIsToA());
                        relative = TrackerController.getTrackedEntityInstance(
                                relationship.getTrackedEntityInstanceA());
                    } else {
                        continue;
                    }

                    String relativeString = getRelativeString(relative);

                    relativeLabel.setText(relativeString);

                    relativeLabel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(!relationship.getTrackedEntityInstanceA().equals(mForm.getTrackedEntityInstance().getUid())) {
                                moveToRelative(relationship.getTrackedEntityInstanceA(), getActivity());
                            }
                            else{
                                moveToRelative(relationship.getTrackedEntityInstanceB(), getActivity());
                            }
                        }
                    });
                    ll.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (relative != null) {
                                ProgramOverviewFragment fragment = ProgramOverviewFragment.
                                        newInstance(getArguments().getString(ORG_UNIT_ID),
                                                getArguments().getString(PROGRAM_ID),
                                                relative.getLocalId());
//                                mNavigationHandler.switchFragment(fragment, CLASS_TAG, true);
                            }
                        }
                    });
                    relationshipsLinearLayout.addView(ll);
                    if (it.hasNext()) {
                        View view = new View(getActivity());
                        view.setLayoutParams(
                                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                        1));
                        view.setBackgroundColor(getResources().getColor(R.color.light_grey));
                        relationshipsLinearLayout.addView(view);
                    }
                }
            }
        }
    }

    private void moveToRelative(String trackedEntityInstanceUid, FragmentActivity activity) {
        activity.finish();
        TrackedEntityInstance trackedEntityInstance =TrackerController.getTrackedEntityInstance(trackedEntityInstanceUid);
        HolderActivity.navigateToProgramOverviewFragment(activity, mState.getOrgUnitId(), mState.getProgramId(), trackedEntityInstance.getLocalId());
    }

    private String getRelativeString(TrackedEntityInstance relative) {

        String relativeString = "";

        if (relative != null && relative.getAttributes() != null) {
            List<Enrollment> enrollments = TrackerController.getEnrollments(relative);
            List<TrackedEntityAttribute> attributesToShow = new ArrayList<>();
            List<TrackedEntityAttributeValue> attributes =
                    TrackerController.getVisibleTrackedEntityAttributeValues(relative.getLocalId());
            for (int i = 0; i < attributes.size() && i < 2; i++) {
                relativeString += attributes.get(i).getValue() + " ";
            }
            if (attributes.size() == 0) {
                if (enrollments != null && !enrollments.isEmpty()) {
                    Program program = null;
                    for (Enrollment e : enrollments) {
                        if (e != null && e.getProgram() != null
                                && e.getProgram().getProgramTrackedEntityAttributes()
                                != null) {
                            program = e.getProgram();
                            break;
                        }
                    }
                    List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes =
                            program.getProgramTrackedEntityAttributes();
                    for (int i = 0; i < programTrackedEntityAttributes.size() && i < 2;
                            i++) {
                        attributesToShow.add(programTrackedEntityAttributes.get(
                                i).getTrackedEntityAttribute());
                    }
                    for (int i = 0; i < attributesToShow.size() && i < 2; i++) {
                        TrackedEntityAttributeValue av =
                                TrackerController.getTrackedEntityAttributeValue(
                                        attributesToShow.get(i).getUid(),
                                        relative.getLocalId());
                        if (av != null && av.getValue() != null) {
                            relativeString += av.getValue() + " ";
                        }
                    }
                } else {
                    for (int i = 0; i < relative.getAttributes().size() && i < 2; i++) {
                        if (relative.getAttributes().get(i) != null
                                && relative.getAttributes().get(i).getValue() != null) {
                            relativeString += relative.getAttributes().get(i).getValue()
                                    + " ";
                        }
                    }
                }
            }
        }
        if (relativeString.isEmpty()) {
            relativeString = getString(R.string.unknown);
        }
        return relativeString;
    }

    public static void showConfirmDeleteRelationshipDialog(final Relationship relationship,
            final TrackedEntityInstance trackedEntityInstance, Activity activity) {
        if (activity == null) return;
        UiUtils.showConfirmDialog(activity, activity.getString(R.string.confirm),
                activity.getString(R.string.confirm_delete_relationship),
                activity.getString(R.string.delete), activity.getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        relationship.delete();
                        trackedEntityInstance.setFromServer(false);
                        trackedEntityInstance.save();
                        dialog.dismiss();
                    }
                });
    }

    @Subscribe
    public void onItemClick(OnProgramStageEventClick eventClick) {
        if (eventClick.isHasPressedFailedButton()) {
            if (eventClick.getEvent() != null) {
                showStatusDialog(eventClick.getEvent());
            }
        } else if (eventClick.isLongPressed()) {
            eventLongPressed = eventClick;
            getActivity().openContextMenu(eventClick.getView());
        } else {
            showDataEntryFragment(eventClick.getEvent(), eventClick.getEvent().getProgramStageId());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {
        new MenuInflater(this.getActivity()).inflate(R.menu.long_click_event_menu, menu);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Event eventClicked = eventLongPressed.getEvent();

        switch (item.getItemId()) {
            case R.id.edit_event:
                if (eventClicked != null) {
                    showDataEntryFragment(eventClicked,
                            eventClicked.getProgramStageId());
                }
                return true;
            case R.id.delete_event:
                if (eventClicked != null) {
                    deleteEvent(eventClicked);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void deleteEvent(final Event eventItemRow) {
        UiUtils.showConfirmDialog(getActivity(), getActivity().getString(R.string.confirm),
                getActivity().getString(R.string.warning_delete_event),
                getActivity().getString(R.string.delete), getActivity().getString(R.string.cancel),
                (R.drawable.ic_event_error),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eventItemRow.setStatus(Event.STATUS_DELETED);
                        eventItemRow.setFromServer(false);
                        Enrollment enrollment = TrackerController.getEnrollment(eventItemRow.getEnrollment());
                        enrollment.setFromServer(false);
                        enrollment.save();
                        TrackedEntityInstance trackedEntityInstance = TrackerController.getTrackedEntityInstance(enrollment.getTrackedEntityInstance());
                        trackedEntityInstance.setFromServer(false);
                        trackedEntityInstance.save();
                        eventItemRow.save();

                        dialog.dismiss();
                    }
                });
    }

    public Map<Long, FailedItem> getFailedEvents() {
        Map<Long, FailedItem> failedItemMap = new HashMap<>();
        List<FailedItem> failedItems = TrackerController.getFailedItems();
        if (failedItems != null && failedItems.size() > 0) {
            for (FailedItem failedItem : failedItems) {
                if (failedItem.getItemType().equals(FailedItem.EVENT)) {
                    failedItemMap.put(failedItem.getItemId(), failedItem);
                }
            }
        }
        return failedItemMap;
    }

    public void showNoActiveEnrollment(ProgramOverviewFragmentForm mForm) {
        enrollmentLayout.setVisibility(View.GONE);

        //start values
        reOpenButton.setVisibility(View.VISIBLE);
        newEnrollmentButton.setVisibility(View.VISIBLE);
        noActiveEnrollment.setText(R.string.no_active_enrollment);

        missingEnrollmentLayout.setVisibility(View.VISIBLE);
        Enrollment lastEnrollment = TrackerController.getLastEnrollment(mForm.getProgram().getUid(),
                mForm.getTrackedEntityInstance());
        if(lastEnrollment!=null) {
            if (mForm.getProgram() != null && mForm.getProgram().getOnlyEnrollOnce()) {
                if(lastEnrollment.getStatus().equals(Enrollment.CANCELLED)) {
                    newEnrollmentButton.setVisibility(View.VISIBLE);
                    noActiveEnrollment.setText(R.string.enrollment_cancelled);
                }else{
                    newEnrollmentButton.setVisibility(View.GONE);
                    noActiveEnrollment.setText(R.string.enrollment_complete);
                }
            }
        }
        if(getLastEnrollmentForTrackedEntityInstance()==null){
            reOpenButton.setVisibility(View.GONE);
        }

        TrackedEntityInstance trackedEntityInstance = TrackerController.getTrackedEntityInstance(
                mForm.getTrackedEntityInstance().getTrackedEntityInstance());
        List<TrackedEntityAttributeValue> trackedEntityAttributeValues =
                TrackerController.getVisibleTrackedEntityAttributeValues(
                        trackedEntityInstance.getLocalId());
        {
            //update profile view
            if (trackedEntityAttributeValues != null && trackedEntityAttributeValues.size()>0) {
                TrackedEntityAttribute attribute = MetaDataController.getTrackedEntityAttribute(
                        trackedEntityAttributeValues.get(0).getTrackedEntityAttributeId());
                if (attribute != null) {
                    attribute1Label.setText(attribute.getName());
                    attribute1Value.setText(trackedEntityAttributeValues.get(0).getValue());
                }

                if (trackedEntityAttributeValues.size()>1) {
                    attribute = MetaDataController.getTrackedEntityAttribute(
                            trackedEntityAttributeValues.get(1).getTrackedEntityAttributeId());
                    if (attribute != null) {
                        attribute2Label.setText(attribute.getName());
                        attribute2Value.setText(trackedEntityAttributeValues.get(1).getValue());
                    }
                }
            }

            List<Enrollment> enrollmentsForTEI = TrackerController.getEnrollments(
                    TrackerController.getTrackedEntityInstance(
                            mState.getTrackedEntityInstanceId()));
            for (Enrollment enrollment : enrollmentsForTEI) {
                Program selectedProgram = (Program) mSpinner.getSelectedItem();

                if (selectedProgram.getUid().equals(enrollment.getProgram().getUid())) {
                    profileCardView.setClickable(false); // Enrollment attributes is applicable.
                    profileButton.setClickable(false);
                    break;
                } else {
                    profileCardView.setClickable(
                            false); // Enrollment attributes not applicable. Clickable(false) to
                    // prevent crash

                    profileButton.setClickable(false);
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<ProgramOverviewFragmentForm> loader) {
        clearViews();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ProgramStageRow row = (ProgramStageRow) listView.getItemAtPosition(position);
        if (row instanceof ProgramStageEventRow) {
            ProgramStageEventRow eventRow = (ProgramStageEventRow) row;
            Event event = eventRow.getEvent();
            showDataEntryFragment(event, event.getProgramStageId());
        }
    }

    private void createEnrollment() {
        EnrollmentDateSetterHelper.createEnrollment(mForm.getTrackedEntityInstance(), this,
                getActivity(), mForm.getProgram().
                        getDisplayIncidentDate(),
                mForm.getProgram().getSelectEnrollmentDatesInFuture(),
                mForm.getProgram().getSelectIncidentDatesInFuture(),
                mForm.getProgram().getEnrollmentDateLabel(),
                mForm.getProgram().getIncidentDateLabel());

        markParentsAsNonFromServer();
    }

    @Override
    public void showEnrollmentFragment(TrackedEntityInstance trackedEntityInstance,
            DateTime enrollmentDate, DateTime incidentDate) {
        String enrollmentDateString = enrollmentDate.toString();
        String incidentDateString = null;
        if (incidentDate != null) {
            incidentDateString = incidentDate.toString();
        }
        if (trackedEntityInstance == null) {
            HolderActivity.navigateToEnrollmentDataEntryFragment(getActivity(),
                    mState.getOrgUnitId(), mState.getProgramId(), enrollmentDateString,
                    incidentDateString);
        } else {
            HolderActivity.navigateToEnrollmentDataEntryFragment(getActivity(),
                    mState.getOrgUnitId(), mState.getProgramId(),
                    trackedEntityInstance.getLocalId(), enrollmentDateString, incidentDateString);
        }
    }

    public void showDataEntryFragment(Event event, String programStage) {
        Bundle args = getArguments();
        if (event == null) {
            HolderActivity.navigateToDataEntryFragment(getActivity(), args.getString(ORG_UNIT_ID),
                    args.getString(PROGRAM_ID), programStage, mForm.getEnrollment().getLocalId());
        } else {
            HolderActivity.navigateToDataEntryFragment(getActivity(), args.getString(ORG_UNIT_ID),
                    args.getString(PROGRAM_ID), programStage,
                    mForm.getEnrollment().getLocalId(), event.getLocalId());
        }
    }

    public void completeEnrollment() {
        if (mForm == null || mForm.getEnrollment() == null) {
            Log.i("ENROLLMENT",
                    "Unable to complete enrollment. mForm or mForm.getEnrollment() is null");
            return;
        }
        mForm.getEnrollment().setStatus(Enrollment.COMPLETED);
        markParentsAsNonFromServer();
        clearViews();
    }

    private void markParentsAsNonFromServer() {
        if (mForm.getEnrollment() != null) {
            mForm.getEnrollment().setFromServer(false);
            mForm.getEnrollment().async().save();
        }

        if (mForm.getTrackedEntityInstance() != null) {
            mForm.getTrackedEntityInstance().setFromServer(false);
            mForm.getTrackedEntityInstance().async().save();
        }
    }

    public void terminateEnrollment() {
        if (mForm == null || mForm.getEnrollment() == null) {
            Log.i("ENROLLMENT",
                    "Unable to terminate enrollment. mForm or mForm.getEnrollment() is null");
            return;
        }
        mForm.getEnrollment().setStatus(Enrollment.CANCELLED);
        markParentsAsNonFromServer();
        setTerminated();
        clearViews();
    }

    /**
     * Removes the currently selected enrollment from being currently selected
     */
    public void setTerminated() {
        onProgramSelected(mForm.getProgram().getUid(), mForm.getProgram().getName());
    }

    public void toggleFollowup() {
        if (mForm == null || mForm.getEnrollment() == null) return;
        mForm.getEnrollment().setFollowup(!mForm.getEnrollment().getFollowup());
        markParentsAsNonFromServer();
        setFollowupButton(mForm.getEnrollment().getFollowup());
    }

    public void setFollowupButton(boolean enabled) {
        if (followupButton == null) return;
        if (enabled) {
            followupButton.setBackgroundResource(R.drawable.rounded_imagebutton_red);
        } else {
            followupButton.setBackgroundResource(R.drawable.rounded_imagebutton_gray);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_program: {
                ProgramDialogFragment fragment = ProgramDialogFragment
                        .newInstance(this, mState.getOrgUnitId(),
                                ProgramType.WITH_REGISTRATION);
                fragment.show(getChildFragmentManager());
                break;
            }

            case R.id.neweventbutton: {
                if (mForm.getEnrollment().getStatus().equals(Enrollment.ACTIVE)) {
                    ProgramStage programStage = (ProgramStage) view.getTag();
                    showDataEntryFragment(null, programStage.getUid());
                }
                break;
            }

            case R.id.eventbackground: {
                if (mForm.getEnrollment().getStatus().equals(Enrollment.ACTIVE)) {
                    Event event = (Event) view.getTag();
                    showDataEntryFragment(event, event.getProgramStageId());
                }
                break;
            }

            case R.id.complete: {
                UiUtils.showConfirmDialog(getActivity(),
                        getString(R.string.un_enroll),
                        getString(R.string.confirm_complete_enrollment),
                        getString(R.string.un_enroll),
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                completeEnrollment();
                            }
                        });
                break;
            }
            case R.id.re_open: {
                Enrollment enrollment = getLastEnrollmentForTrackedEntityInstance();
                if(enrollment!=null) {
                    enrollment.setStatus(Enrollment.ACTIVE);
                    enrollment.setFromServer(false);
                    enrollment.async().save();
                    markParentsAsNonFromServer();
                    refreshUi();

                }
                break;
            }

            case R.id.terminate: {
                UiUtils.showConfirmDialog(getActivity(),
                        getString(R.string.terminate),
                        getString(R.string.confirm_terminate_enrollment),
                        getString(R.string.yes),
                        getString(R.string.no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                terminateEnrollment();
                            }
                        });
                break;
            }

            case R.id.followupButton: {
                toggleFollowup();
                break;
            }

            case R.id.newenrollmentbutton: {
                createEnrollment();
                break;
            }

            case R.id.profile_cardview: {
                editTrackedEntityInstanceProfile();
                break;
            }
            case R.id.profile_button: {
                editTrackedEntityInstanceProfile();
                break;
            }
            case R.id.enrollmentstatus: {
                if (mForm != null && mForm.getEnrollment() != null) {
                    showStatusDialog(mForm.getEnrollment());
                }
                break;
            }
            case R.id.pullrelationshipbutton: {
                refreshRelationships();
                break;
            }
            case R.id.addrelationshipbutton: {
                showAddRelationshipFragment();
                break;
            }
            case R.id.enrollmentLayout: {
                editEnrollmentDates();
            }
        }
    }

    private void refreshRelationships() {
        Context context = getActivity().getBaseContext();
        Toast.makeText(context, getString(org.hisp.dhis.android.sdk.R.string.refresh_relations),
                Toast.LENGTH_SHORT).show();
        if (mForm != null && mForm.getTrackedEntityInstance() != null) {
            refreshTrackedEntityRelationships(mForm.getTrackedEntityInstance().getUid());
        }
    }

    private void refreshUi() {
        getLoaderManager().restartLoader(LOADER_ID, getArguments(), this);
    }

    private Enrollment getLastEnrollmentForTrackedEntityInstance() {
        List<Enrollment> enrollments = TrackerController.getEnrollments(
                mForm.getTrackedEntityInstance(), mForm.getProgram().getUid(), mForm.getTrackedEntityInstance().getOrgUnit());
         if(enrollments==null || enrollments.size()==0) {
            return null;
        }
        EnrollmentDateComparator comparator = new EnrollmentDateComparator();
        Collections.reverseOrder(comparator);
        Collections.sort(enrollments, comparator);
        return enrollments.get(0);
    }

    private void clearViews() {
        adapter.swapData(null);
    }

    public void showStatusDialog(BaseSerializableModel model) {
        ItemStatusDialogFragment fragment = ItemStatusDialogFragment.newInstance(model);
        fragment.show(getChildFragmentManager());
    }

    private void editEnrollmentDates() {
        if (mForm != null && mForm.getEnrollment() != null) {
            HolderActivity.navigateToEnrollmentDateFragment(getActivity(),
                    mForm.getEnrollment().getLocalId());
        }

    }

    private void editTrackedEntityInstanceProfile() {
        HolderActivity.navigateToTrackedEntityInstanceProfileFragment(getActivity(), getArguments().
                        getLong(TRACKEDENTITYINSTANCE_ID), getArguments().getString(PROGRAM_ID),
                mForm.getEnrollment().getLocalId());
    }

    private void showAddRelationshipFragment() {
        if (mForm == null || mForm.getTrackedEntityInstance() == null) return;
        RegisterRelationshipDialogFragment fragment =
                RegisterRelationshipDialogFragment.newInstance(
                        mForm.getTrackedEntityInstance().getLocalId(), mForm.getProgram().getUid());
        fragment.show(getChildFragmentManager(), CLASS_TAG);
    }

    void displayKeyValuePair(ProgramRuleAction programRuleAction) {
        FlowLayout programIndicatorLayout = (FlowLayout) programIndicatorCardView.findViewById(
                R.id.keyvaluelayout);
        KeyValueView keyValueView = new KeyValueView(programRuleAction.getContent(),
                ProgramRuleService.getCalculatedConditionValue(programRuleAction.getData()));
        FlowLayout.LayoutParams layoutParams = new FlowLayout.LayoutParams(10, 10);
        View view = keyValueView.getView(getLayoutInflater(getArguments()), programIndicatorLayout);
        view.setLayoutParams(layoutParams);
        programIndicatorLayout.addView(view);
    }

    void displayText(ProgramRuleAction programRuleAction) {
        LinearLayout programIndicatorLayout = (LinearLayout) programIndicatorCardView.findViewById(
                R.id.textlayout);
        PlainTextRow textRow = new PlainTextRow(
                ProgramRuleService.getCalculatedConditionValue(programRuleAction.getData()));
        View view = textRow.getView(getChildFragmentManager(), getLayoutInflater(getArguments()),
                null, programIndicatorLayout);
        view.findViewById(R.id.text_label).setVisibility(View.GONE);
        view.findViewById(R.id.detailed_info_button_layout).setVisibility(View.GONE);
        programIndicatorLayout.addView(view);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Program program = (Program) mSpinnerAdapter.getItem(position);
        onProgramSelected(program.getUid(), program.getName());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onOptionSelected(int dialogId, int position, String id, String name) {
        switch (dialogId) {

            case ProgramDialogFragment.ID: {
                onProgramSelected(id, name);
                break;
            }
        }
    }

    @Override
    public void onRefresh() {
        if (isAdded()) {
            Context context = getActivity().getBaseContext();
            Toast.makeText(context, getString(org.hisp.dhis.android.sdk.R.string.syncing),
                    Toast.LENGTH_SHORT).show();
            synchronize();
        }
    }

    protected void setRefreshing(final boolean refreshing) {
        /* workaround for bug in android support v4 library */
        if (mSwipeRefreshLayout.isRefreshing() != refreshing) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(refreshing);
                }
            });
        }
    }

    @Subscribe
    public void onReceivedUiEvent(UiEvent uiEvent) {
        if (uiEvent.getEventType().equals(UiEvent.UiEventType.SYNCING_START)) {
            setRefreshing(true);
        } else if (uiEvent.getEventType().equals(UiEvent.UiEventType.SYNCING_END)) {
            setRefreshing(false);
        }
    }

    public void synchronize() {
        if (mForm != null) {
            sendTrackedEntityInstance(mForm.getTrackedEntityInstance());
        }
    }

    public void refreshTrackedEntityRelationships(final String trackedEntityInstance) {
        Dhis2Application.getEventBus().post(new UiEvent(UiEvent.UiEventType.SYNCING_START));
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.TRACKEDENTITYINSTANCE) {
            @Override
            public Object execute() {
                TrackerController.refreshRelationsByTrackedEntity(
                        DhisController.getInstance().getDhisApi(), trackedEntityInstance);
                return new Object();
            }
        });
    }

    public void sendTrackedEntityInstance(final TrackedEntityInstance trackedEntityInstance) {
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.TRACKEDENTITYINSTANCE) {
            @Override
            public Object execute() {
                DhisApi dhisApi = DhisController.getInstance().getDhisApi();
                EnrollmentLocalDataSource enrollmentLocalDataSource = new EnrollmentLocalDataSource();
                EnrollmentRemoteDataSource enrollmentRemoteDataSource = new EnrollmentRemoteDataSource(dhisApi);
                IEnrollmentRepository enrollmentRepository = new EnrollmentRepository(enrollmentLocalDataSource, enrollmentRemoteDataSource);

                EventLocalDataSource mLocalDataSource = new EventLocalDataSource();
                EventRemoteDataSource mRemoteDataSource = new EventRemoteDataSource(DhisController.getInstance().getDhisApi());
                EventRepository eventRepository = new EventRepository(mLocalDataSource, mRemoteDataSource);
                FailedItemRepository failedItemRepository = new FailedItemRepository();

                TrackedEntityInstanceLocalDataSource trackedEntityInstanceLocalDataSource = new TrackedEntityInstanceLocalDataSource();
                TrackedEntityInstanceRemoteDataSource trackedEntityInstanceRemoteDataSource = new TrackedEntityInstanceRemoteDataSource(dhisApi);
                ITrackedEntityInstanceRepository
                        trackedEntityInstanceRepository = new TrackedEntityInstanceRepository(trackedEntityInstanceLocalDataSource, trackedEntityInstanceRemoteDataSource);
                SyncTrackedEntityInstanceUseCase syncTrackedEntityInstanceUseCase = new SyncTrackedEntityInstanceUseCase(trackedEntityInstanceRepository, enrollmentRepository, eventRepository, failedItemRepository);
                syncTrackedEntityInstanceUseCase.execute(trackedEntityInstance);
                return new Object();
            }
        });
    }

    public ProgramOverviewFragmentForm getForm() {
        return mForm;
    }

    public void setForm(ProgramOverviewFragmentForm mForm) {
        this.mForm = mForm;
    }

    @Override
    public boolean doBack() {
        getActivity().finish();
        return false;
    }


    @Subscribe
    public void onShowDetailedInfo(OnDetailedInfoButtonClick eventClick)
    {
        UiUtils.showConfirmDialog(getActivity(),
                getResources().getString(org.hisp.dhis.android.sdk.R.string.detailed_info_dataelement),
                eventClick.getRow().getDescription(), getResources().getString(
                        org.hisp.dhis.android.sdk.R.string.ok_option),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
    }
}
