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
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
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
import org.hisp.dhis.android.sdk.ui.activities.INavigationHandler;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.IndicatorRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.KeyValueRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.NonEditableTextViewRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.PlainTextRow;
import org.hisp.dhis.android.sdk.ui.dialogs.ProgramDialogFragment;
import org.hisp.dhis.android.sdk.ui.fragments.common.AbsProgramRuleFragment;
import org.hisp.dhis.android.sdk.ui.fragments.eventdataentry.EventDataEntryFragment;
import org.hisp.dhis.android.sdk.ui.fragments.settings.SettingsFragment;
import org.hisp.dhis.android.sdk.ui.views.FloatingActionButton;
import org.hisp.dhis.android.sdk.ui.views.FontTextView;
import org.hisp.dhis.android.sdk.utils.UiUtils;
import org.hisp.dhis.android.sdk.utils.api.ProgramType;
import org.hisp.dhis.android.sdk.utils.services.ProgramRuleService;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.fragments.enrollment.EnrollmentDataEntryFragment;
import org.hisp.dhis.android.trackercapture.fragments.enrollmentdate.EnrollmentDateFragment;
import org.hisp.dhis.android.trackercapture.fragments.programoverview.registerrelationshipdialogfragment.RegisterRelationshipDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.EnrollmentDateSetterHelper;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.IEnroller;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs.ItemStatusDialogFragment;
import org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile.TrackedEntityInstanceProfileFragment;
import org.hisp.dhis.android.trackercapture.ui.adapters.ProgramAdapter;
import org.hisp.dhis.android.trackercapture.ui.adapters.ProgramStageAdapter;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.OnProgramStageEventClick;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageEventRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageLabelRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.ProgramStageRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ProgramOverviewFragment extends AbsProgramRuleFragment implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        ProgramDialogFragment.OnOptionSelectedListener,
        LoaderManager.LoaderCallbacks<ProgramOverviewFragmentForm>, AdapterView.OnItemSelectedListener, SwipeRefreshLayout.OnRefreshListener, IEnroller {

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

    private LinearLayout missingEnrollmentLayout;
    private FloatingActionButton newEnrollmentButton;

    private CardView profileCardView;
    private CardView enrollmentCardview;
    private CardView programIndicatorCardView;

    private ImageButton followupButton;
    private ImageButton profileButton;
    private ImageView enrollmentServerStatus;
    private Button completeButton;
    private Button terminateButton;

    private TextView attribute1Label;
    private TextView attribute1Value;
    private TextView attribute2Label;
    private TextView attribute2Value;

    private LinearLayout relationshipsLinearLayout;
    private Button newRelationshipButton;

    private ProgramOverviewFragmentState mState;
    private ProgramOverviewFragmentForm mForm;

    private INavigationHandler mNavigationHandler;

    public ProgramOverviewFragment() {
        setProgramRuleFragmentHelper(new ProgramOverviewRuleHelper(this));
    }

    public static ProgramOverviewFragment newInstance(String orgUnitId, String programId, long trackedEntityInstanceId) {
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

        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBundle(EXTRA_ARGUMENTS, getArguments());
        argumentsBundle.putBundle(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, argumentsBundle, this);

        mProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof INavigationHandler) {
            mNavigationHandler = (INavigationHandler) activity;
        } else {
            throw new IllegalArgumentException("Activity must implement INavigationHandler interface");
        }
    }

    @Override
    public void onDetach() {

        // we need to nullify reference
        // to parent activity in order not to leak it
        if (getActivity() != null &&
                getActivity() instanceof INavigationHandler) {
            ((INavigationHandler) getActivity()).setBackPressedListener(null);
        }
        // we need to nullify reference
        // to parent activity in order not to leak it
        mNavigationHandler = null;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_programoverview, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(getActivity() instanceof AppCompatActivity) {
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        listView = (ListView) view.findViewById(R.id.listview);
        View header = getLayoutInflater(savedInstanceState).inflate(
                R.layout.fragment_programoverview_header, listView, false
        );

        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(org.hisp.dhis.android.sdk.R.id.swipe_to_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeResources(org.hisp.dhis.android.sdk.R.color.Green, org.hisp.dhis.android.sdk.R.color.Blue, org.hisp.dhis.android.sdk.R.color.orange);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        relationshipsLinearLayout = (LinearLayout) header.findViewById(R.id.relationships_linearlayout);
        newRelationshipButton = (Button) header.findViewById(R.id.addrelationshipbutton);
        newRelationshipButton.setOnClickListener(this);

        mProgressBar = (ProgressBar) header.findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.GONE);

        adapter = new ProgramStageAdapter(getLayoutInflater(savedInstanceState));
        listView.addHeaderView(header, CLASS_TAG, false);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        enrollmentServerStatus = (ImageView) header.findViewById(R.id.enrollmentstatus);
        enrollmentLayout = (LinearLayout) header.findViewById(R.id.enrollmentLayout);
        enrollmentDateLabel = (TextView) header.findViewById(R.id.dateOfEnrollmentLabel);
        enrollmentDateValue = (TextView) header.findViewById(R.id.dateOfEnrollmentValue);
        incidentDateLabel = (TextView) header.findViewById(R.id.dateOfIncidentLabel);
        incidentDateValue = (TextView) header.findViewById(R.id.dateOfIncidentValue);
        profileCardView = (CardView) header.findViewById(R.id.profile_cardview);
        enrollmentCardview = (CardView) header.findViewById(R.id.enrollment_cardview);
        programIndicatorCardView = (CardView) header.findViewById(R.id.programindicators_cardview);

        completeButton = (Button) header.findViewById(R.id.complete);
        terminateButton = (Button) header.findViewById(R.id.terminate);
        followupButton = (ImageButton) header.findViewById(R.id.followupButton);
        profileButton = (ImageButton) header.findViewById(R.id.profile_button);
        completeButton.setOnClickListener(this);
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
            OrganisationUnit ou = MetaDataController.getOrganisationUnit(fragmentArguments.getString(ORG_UNIT_ID));
            Program program = MetaDataController.getProgram(fragmentArguments.getString(PROGRAM_ID));
            mState.setOrgUnit(ou.getId(), ou.getLabel());
            mState.setProgram(program.getUid(), program.getName());
            mState.setTrackedEntityInstance(fragmentArguments.getLong(TRACKEDENTITYINSTANCE_ID, -1));
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
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            mNavigationHandler.switchFragment(
                    new SettingsFragment(), SettingsFragment.TAG, true);
        }
        else if (id == android.R.id.home) {
            getFragmentManager().popBackStack();
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
            if (program.getName().equals(programName))
                index = i;
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

            mSpinner = (Spinner) mSpinnerContainer.findViewById(org.hisp.dhis.android.sdk.R.id.toolbar_spinner);
            mSpinner.setAdapter(mSpinnerAdapter);
            mSpinner.post(new Runnable() {
                public void run() {
                    if(mSpinner != null) {
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
    public void onLoadFinished(Loader<ProgramOverviewFragmentForm> loader, ProgramOverviewFragmentForm data) {
        if (LOADER_ID == loader.getId()) {
            mForm = data;
            mProgressBar.setVisibility(View.GONE);
            setRefreshing(false);

            mSpinner.setSelection(getSpinnerIndex(mState.getProgramName()));

            if (mForm == null || mForm.getEnrollment() == null) {
                showNoActiveEnrollment();
                return;
            } else {
                enrollmentLayout.setVisibility(View.VISIBLE);
                missingEnrollmentLayout.setVisibility(View.GONE);
                profileCardView.setClickable(true); //is set to false when TEI doesn't have an applicable enrollment. todo why?
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
            FailedItem failedItem = TrackerController.getFailedItem(FailedItem.ENROLLMENT, mForm.getEnrollment().getLocalId());

            if (failedItem != null && failedItem.getHttpStatusCode() >= 0) {
                enrollmentServerStatus.setImageResource(R.drawable.ic_event_error);
            } else if (!mForm.getEnrollment().isFromServer()) {
                enrollmentServerStatus.setImageResource(R.drawable.ic_offline);
            } else {
                enrollmentServerStatus.setImageResource(R.drawable.ic_from_server);
            }

            if (mForm.getEnrollment().getStatus().equals(Enrollment.COMPLETED)) {
                setCompleted();
            }

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
                    }
                    else
                    {
                        if(stageRow.getEventRows().size() < 1) { // if stage is not autogen and not repeatable, allow user to create exactly one event
                            stageRow.setButtonListener(this);
                        }
                        if(stageRow.getProgramStage().getAutoGenerateEvent()) {
                            stageRow.setButtonListener(this);
                        }
                    }

                } else if (row instanceof ProgramStageEventRow) {
                    final ProgramStageEventRow eventRow = (ProgramStageEventRow) row;

                    FailedItem failedItem1 = TrackerController.getFailedItem(FailedItem.EVENT, eventRow.getEvent().getLocalId());

                    if (failedItem1 != null && failedItem1.getHttpStatusCode() >= 0) {
                        eventRow.setHasFailed(true);
                        eventRow.setMessage(failedEvents.get(eventRow.getEvent().getLocalId()).getErrorMessage());
                    } else if (eventRow.getEvent().isFromServer()) {
                        eventRow.setSynchronized(true);
                        eventRow.setMessage(getString(R.string.status_sent_description));
                    } else {
                        eventRow.setSynchronized(false);
                        eventRow.setMessage(getString(R.string.status_offline_description));
                    }
                }
            }
            setRelationships(getLayoutInflater(getArguments().getBundle(EXTRA_SAVED_INSTANCE_STATE)));

            LinearLayout programIndicatorLayout = (LinearLayout) programIndicatorCardView.findViewById(R.id.programindicatorlayout);
            programIndicatorLayout.removeAllViews();
            FlowLayout keyValueLayout = (FlowLayout) programIndicatorCardView.findViewById(R.id.keyvaluelayout);
            keyValueLayout.removeAllViews();
            LinearLayout displayTextLayout = (LinearLayout) programIndicatorCardView.findViewById(R.id.textlayout);
            displayTextLayout.removeAllViews();
            for(IndicatorRow indicatorRow : mForm.getProgramIndicatorRows().values()) {
                View view = indicatorRow.getView(getChildFragmentManager(), getLayoutInflater(getArguments()), null, programIndicatorLayout);
                programIndicatorLayout.addView(view);
            }

            evaluateAndApplyProgramRules();
            adapter.swapData(data.getProgramStageRows());
        }
    }

    /**
     * Inflates views and adds them to linear layout for relationships, sort of like a listview, but
     * inside another listview
     */
    public void setRelationships(LayoutInflater inflater) {
        relationshipsLinearLayout.removeAllViews();
        if (mForm.getTrackedEntityInstance() != null && mForm.getTrackedEntityInstance().getRelationships() != null) {
            ListIterator<Relationship> it = mForm.getTrackedEntityInstance().getRelationships().listIterator();
            while (it.hasNext()) {
                final Relationship relationship = it.next();
                if (relationship == null) {
                    continue;
                }
                LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.listview_row_relationship, null);
                FontTextView currentTeiRelationshipLabel = (FontTextView) ll.findViewById(R.id.current_tei_relationship_label);
                FontTextView relativeLabel = (FontTextView) ll.findViewById(R.id.relative_relationship_label);
                Button deleteButton = (Button) ll.findViewById(R.id.delete_relationship);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showConfirmDeleteRelationshipDialog(relationship,
                                mForm.getTrackedEntityInstance(), getActivity());
                    }
                });
                RelationshipType relationshipType = MetaDataController.getRelationshipType(relationship.getRelationship());

                if (relationshipType != null) {

                    /* establishing if the relative is A or B in Relationship Type */
                    final TrackedEntityInstance relative;
                    if (mForm.getTrackedEntityInstance().getTrackedEntityInstance() != null &&
                            mForm.getTrackedEntityInstance().getTrackedEntityInstance().equals(relationship.getTrackedEntityInstanceA())) {

                        currentTeiRelationshipLabel.setText(relationshipType.getaIsToB());
                        relative = TrackerController.getTrackedEntityInstance(relationship.getTrackedEntityInstanceB());

                    } else if (mForm.getTrackedEntityInstance().getTrackedEntityInstance() != null &&
                            mForm.getTrackedEntityInstance().getTrackedEntityInstance().equals(relationship.getTrackedEntityInstanceB())) {

                        currentTeiRelationshipLabel.setText(relationshipType.getbIsToA());
                        relative = TrackerController.getTrackedEntityInstance(relationship.getTrackedEntityInstanceA());
                    } else {
                        continue;
                    }

                    /* Creating a string to display as name of relative from attributes */
                    String relativeString = "";
                    if (relative != null && relative.getAttributes() != null) {
                        List<Enrollment> enrollments = TrackerController.getEnrollments(relative);
                        List<TrackedEntityAttribute> attributesToShow = new ArrayList<>();
                        if (enrollments != null && !enrollments.isEmpty()) {
                            Program program = null;
                            for (Enrollment e : enrollments) {
                                if (e != null && e.getProgram() != null && e.getProgram().getProgramTrackedEntityAttributes() != null) {
                                    program = e.getProgram();
                                    break;
                                }
                            }
                            List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = program.getProgramTrackedEntityAttributes();
                            for (int i = 0; i < programTrackedEntityAttributes.size() && i < 2; i++) {
                                attributesToShow.add(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute());
                            }
                            for (int i = 0; i < attributesToShow.size() && i < 2; i++) {
                                TrackedEntityAttributeValue av = TrackerController.getTrackedEntityAttributeValue(attributesToShow.get(i).getUid(), relative.getLocalId());
                                if (av != null && av.getValue() != null) {
                                    relativeString += av.getValue() + " ";
                                }
                            }
                        } else {
                            for (int i = 0; i < relative.getAttributes().size() && i < 2; i++) {
                                if (relative.getAttributes().get(i) != null && relative.getAttributes().get(i).getValue() != null) {
                                    relativeString += relative.getAttributes().get(i).getValue() + " ";
                                }
                            }
                        }
                    }
                    if (relativeString.isEmpty()) {
                        relativeString = getString(R.string.unknown);
                    }
                    relativeLabel.setText(relativeString);

                    ll.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (relative != null) {
                                ProgramOverviewFragment fragment = ProgramOverviewFragment.
                                        newInstance(getArguments().getString(ORG_UNIT_ID),
                                                getArguments().getString(PROGRAM_ID), relative.getLocalId());
                                mNavigationHandler.switchFragment(fragment, CLASS_TAG, true);
                            }
                        }
                    });
                    relationshipsLinearLayout.addView(ll);
                    if (it.hasNext()) {
                        View view = new View(getActivity());
                        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
                        view.setBackgroundColor(getResources().getColor(R.color.light_grey));
                        relationshipsLinearLayout.addView(view);
                    }
                }
            }
        }
    }

    public static void showConfirmDeleteRelationshipDialog(final Relationship relationship,
                                                           final TrackedEntityInstance trackedEntityInstance,
                                                           Activity activity) {
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
            if (eventClick.getEvent() != null)
                showStatusDialog(eventClick.getEvent());
        } else {
            showDataEntryFragment(eventClick.getEvent(), eventClick.getEvent().getProgramStageId());
        }
    }

    public Map<Long, FailedItem> getFailedEvents() {
        Map<Long, FailedItem> failedItemMap = new HashMap<>();
        List<FailedItem> failedItems = TrackerController.getFailedItems();
        if (failedItems != null && failedItems.size() > 0) {
            for (FailedItem failedItem : failedItems) {
                if (failedItem.getItemType().equals(FailedItem.EVENT))
                    failedItemMap.put(failedItem.getItemId(), failedItem);
            }
        }
        return failedItemMap;
    }

    public void showNoActiveEnrollment() {
        enrollmentLayout.setVisibility(View.GONE);
        missingEnrollmentLayout.setVisibility(View.VISIBLE);

        //update profile view
        List<Enrollment> enrollmentsForTEI = TrackerController.getEnrollments(TrackerController.getTrackedEntityInstance(mState.getTrackedEntityInstanceId()));
        for (Enrollment enrollment : enrollmentsForTEI) {
            Program selectedProgram = (Program) mSpinner.getSelectedItem();

            if (selectedProgram.getUid().equals(enrollment.getProgram().getUid())) {
                profileCardView.setClickable(false); // Enrollment attributes is applicable.
                profileButton.setClickable(false);
                TrackedEntityInstance trackedEntityInstance = TrackerController.getTrackedEntityInstance(enrollment.getLocalTrackedEntityInstanceId());

                int numberOfProgramTrackedEntityAttributes = selectedProgram.getProgramTrackedEntityAttributes().size();
                int numberOfTrackedEntityAttributeValues = trackedEntityInstance.getAttributes().size();

                if (numberOfProgramTrackedEntityAttributes > 0 && numberOfTrackedEntityAttributeValues > 0) {
                    TrackedEntityAttribute attribute1 = selectedProgram.getProgramTrackedEntityAttributes().get(0).getTrackedEntityAttribute();
                    attribute1Label.setText(attribute1.getName());
                    TrackedEntityAttributeValue attribute1Val = TrackerController.getTrackedEntityAttributeValue(attribute1.getUid(), trackedEntityInstance.getLocalId());
                    attribute1Value.setText(attribute1Val.getValue());
                } else {
                    attribute1Label.setText("");
                    attribute1Value.setText("");
                }

                if (numberOfProgramTrackedEntityAttributes > 1 && numberOfTrackedEntityAttributeValues > 1) {
                    TrackedEntityAttribute attribute2 = selectedProgram.getProgramTrackedEntityAttributes().get(1).getTrackedEntityAttribute();
                    TrackedEntityAttributeValue attribute2Val = TrackerController.getTrackedEntityAttributeValue(attribute2.getUid(), trackedEntityInstance.getLocalId());

                    attribute2Label.setText(attribute2.getName());
                    attribute2Value.setText(attribute2Val.getValue());
                } else {
                    attribute2Label.setText("");
                    attribute2Value.setText("");
                }

                break;
            } else {
                profileCardView.setClickable(false); // Enrollment attributes not applicable. Clickable(false) to prevent crash
                profileButton.setClickable(false);
                int numberOfProgramTrackedEntityAttributes = selectedProgram.getProgramTrackedEntityAttributes().size();

                if (numberOfProgramTrackedEntityAttributes > 0)
                    attribute1Label.setText(selectedProgram.getProgramTrackedEntityAttributes().get(0).getTrackedEntityAttribute().getName());
                else
                    attribute1Label.setText("");

                if (numberOfProgramTrackedEntityAttributes > 1)
                    attribute2Label.setText(selectedProgram.getProgramTrackedEntityAttributes().get(1).getTrackedEntityAttribute().getName());
                else
                    attribute2Label.setText("");

                attribute1Value.setText("");
                attribute2Value.setText("");
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
        EnrollmentDateSetterHelper.createEnrollment(mForm.getTrackedEntityInstance(), this, getActivity(), mForm.getProgram().
                        getDisplayIncidentDate(), mForm.getProgram().getSelectEnrollmentDatesInFuture(),
                mForm.getProgram().getSelectIncidentDatesInFuture(), mForm.getProgram().getEnrollmentDateLabel(),
                mForm.getProgram().getIncidentDateLabel());
    }

    @Override
    public void showEnrollmentFragment(TrackedEntityInstance trackedEntityInstance, DateTime enrollmentDate, DateTime incidentDate) {
        String enrollmentDateString = enrollmentDate.toString();
        String incidentDateString = null;
        if(incidentDate != null) {
            incidentDateString = incidentDate.toString();
        }
        EnrollmentDataEntryFragment enrollmentDataEntryFragment;
        if(trackedEntityInstance == null) {
            enrollmentDataEntryFragment = EnrollmentDataEntryFragment.newInstance(mState.getOrgUnitId(), mState.getProgramId(), enrollmentDateString, incidentDateString);
        } else {
            enrollmentDataEntryFragment = EnrollmentDataEntryFragment.newInstance(mState.getOrgUnitId(), mState.getProgramId(), trackedEntityInstance.getLocalId(), enrollmentDateString, incidentDateString);
        }
        mNavigationHandler.switchFragment(enrollmentDataEntryFragment, EnrollmentDataEntryFragment.class.getName(), true);
    }

    public void showDataEntryFragment(Event event, String programStage) {
        Bundle args = getArguments();
        EventDataEntryFragment fragment;
        if (event == null) {
            fragment = EventDataEntryFragment.newInstanceWithEnrollment(args.getString(ORG_UNIT_ID), args.getString(PROGRAM_ID), programStage, mForm.getEnrollment().getLocalId());
        } else {
            fragment = EventDataEntryFragment.newInstanceWithEnrollment(args.getString(ORG_UNIT_ID), args.getString(PROGRAM_ID), programStage,
                    event.getLocalEnrollmentId(), event.getLocalId());
        }
        mNavigationHandler.switchFragment(fragment, ProgramOverviewFragment.CLASS_TAG, true);
    }

    public void completeEnrollment() {
        mForm.getEnrollment().setStatus(Enrollment.COMPLETED);
        mForm.getEnrollment().setFromServer(false);
        mForm.getEnrollment().async().save();
        setCompleted();
        clearViews();
    }

    /**
     * Disables the ability to edit enrollment info
     * Program stages can still be viewed but not changed.
     */
    public void setCompleted() {
        completeButton.setEnabled(false);
        completeButton.setAlpha(0.2f);
        terminateButton.setEnabled(false);
        terminateButton.setAlpha(0.2f);
        followupButton.setEnabled(false);
        followupButton.setAlpha(0x40);
    }

    public void terminateEnrollment() {
        mForm.getEnrollment().setStatus(Enrollment.CANCELLED);
        mForm.getEnrollment().setFromServer(false);
        mForm.getEnrollment().async().save();
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
        mForm.getEnrollment().setFromServer(false);
        mForm.getEnrollment().async().save();
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
                        getString(R.string.complete),
                        getString(R.string.confirm_complete_enrollment),
                        getString(R.string.complete),
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                completeEnrollment();
                            }
                        });
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
                if (mForm != null && mForm.getEnrollment() != null)
                    showStatusDialog(mForm.getEnrollment());
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

    private void clearViews() {
        adapter.swapData(null);
    }

    public void showStatusDialog(BaseSerializableModel model) {
        ItemStatusDialogFragment fragment = ItemStatusDialogFragment.newInstance(model);
        fragment.show(getChildFragmentManager());
    }

    private void editEnrollmentDates() {
        EnrollmentDateFragment fragment = EnrollmentDateFragment.newInstance(mForm.getEnrollment().getLocalId());
        mNavigationHandler.switchFragment(fragment, EnrollmentDateFragment.TAG, true);
    }

    private void editTrackedEntityInstanceProfile() {
        TrackedEntityInstanceProfileFragment fragment = TrackedEntityInstanceProfileFragment.newInstance(getArguments().
                getLong(TRACKEDENTITYINSTANCE_ID), getArguments().getString(PROGRAM_ID));
        mNavigationHandler.switchFragment(fragment, TrackedEntityInstanceProfileFragment.TAG, true);
    }

    private void showAddRelationshipFragment() {
        if (mForm == null || mForm.getTrackedEntityInstance() == null) return;
        RegisterRelationshipDialogFragment fragment = RegisterRelationshipDialogFragment.newInstance(mForm.getTrackedEntityInstance().getLocalId());
        fragment.show(getChildFragmentManager(), CLASS_TAG);
    }

    void displayKeyValuePair(ProgramRuleAction programRuleAction) {
        FlowLayout programIndicatorLayout = (FlowLayout) programIndicatorCardView.findViewById(R.id.keyvaluelayout);
        KeyValueView keyValueView = new KeyValueView(programRuleAction.getContent(), ProgramRuleService.getCalculatedConditionValue(programRuleAction.getData()));
        FlowLayout.LayoutParams layoutParams = new FlowLayout.LayoutParams(10, 10);
        View view = keyValueView.getView(getLayoutInflater(getArguments()), programIndicatorLayout);
        view.setLayoutParams(layoutParams);
        programIndicatorLayout.addView(view);
    }

    void displayText(ProgramRuleAction programRuleAction) {
        LinearLayout programIndicatorLayout = (LinearLayout) programIndicatorCardView.findViewById(R.id.textlayout);
        PlainTextRow textRow = new PlainTextRow(ProgramRuleService.getCalculatedConditionValue(programRuleAction.getData()));
        View view = textRow.getView(getChildFragmentManager(), getLayoutInflater(getArguments()), null, programIndicatorLayout);
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
            Toast.makeText(context, getString(org.hisp.dhis.android.sdk.R.string.syncing), Toast.LENGTH_SHORT).show();
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
        sendTrackedEntityInstance(mForm.getTrackedEntityInstance());
    }


    public void sendTrackedEntityInstance(final TrackedEntityInstance trackedEntityInstance) {
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.TRACKEDENTITYINSTANCE) {
            @Override
            public Object execute() {
                TrackerController.sendTrackedEntityInstanceChanges(DhisController.getInstance().getDhisApi(), trackedEntityInstance, true);
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

}
