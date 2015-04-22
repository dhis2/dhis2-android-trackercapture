/*
 *  Copyright (c) 2015, University of Oslo
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

package org.hisp.dhis2.android.trackercapture.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.squareup.otto.Subscribe;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.InvalidateEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.sdk.utils.ui.views.CardSpinner;
import org.hisp.dhis2.android.trackercapture.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 18.03.15.
 */
public class ProgramOverviewFragment extends Fragment {

    private static final String CLASS_TAG = "ProgramOverviewFragment";

    private CardSpinner programSpinner;
    private Program selectedProgram;
    private ProgramStage selectedProgramStage;
    private OrganisationUnit selectedOrganisationUnit;
    private TrackedEntityInstance selectedTrackedEntityInstance;
    private CardView enrollmentCardView;
    private CardView profileCardView;
    private CardView programStageCardView;
    private Button completeButton;
    private Button terminateButton;
    private Button markForFollowupButton;
    private Button newEnrollmentButton;
    private Button enrollmentHistoryButton;
    private Enrollment currentEnrollment;
    private Event selectedEvent;
    private View rootView;
    private LinearLayout enrollmentContainer;
    private LinearLayout profileContainer;
    private LinearLayout programStageContainer;
    private Activity activity; /*need this because invalidate is sometimes called from service*/
    List<Program> programs;
    private FlowContentObserver flowContentObserver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        activity = getActivity();
        rootView = inflater.inflate(R.layout.fragment_programoverview,
                container, false);


        flowContentObserver = new FlowContentObserver();
        flowContentObserver.registerForContentChanges(activity, Enrollment.class);
        flowContentObserver.registerForContentChanges(activity, Event.class);
        FlowContentObserver.ModelChangeListener modelChangeListener = new FlowContentObserver.ModelChangeListener() {
            @Override
            public void onModelChanged() {
                // called in SDK<14
                checkForUpdatedData();
            }

            @Override
            public void onModelSaved() {
                checkForUpdatedData();
            }

            @Override
            public void onModelDeleted() {
                checkForUpdatedData();
            }

            @Override
            public void onModelInserted() {
                checkForUpdatedData();
            }

            @Override
            public void onModelUpdated() {
                checkForUpdatedData();
            }
        };
        flowContentObserver.addModelChangeListener(modelChangeListener);

        setSpinner();
        setupUi(rootView);
        return rootView;
    }

    /**
     * checks if the data currently loaded in the UI needs to be updated because of changes in
     * the database.
     */
    public void checkForUpdatedData() {
        //todo: do it
    }

    @Override
    public void onStart() {
        Dhis2Application.bus.register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        Dhis2Application.bus.unregister(this);
        super.onStop();
    }

    /**
     * Populates the program selection spinner
     */
    public void setSpinner() {
        programSpinner = (CardSpinner) rootView.findViewById(R.id.program_spinner);
        programs = MetaDataController.getProgramsForOrganisationUnit(selectedOrganisationUnit.getId(),
                Program.MULTIPLE_EVENTS_WITH_REGISTRATION, Program.SINGLE_EVENT_WITH_REGISTRATION);

        List<String> programNames = new ArrayList<String>();
        for( Program program: programs )
            programNames.add(program.getName());
        populateSpinner(programSpinner, programNames);

        for(int i = 0; i<programs.size(); i++) {
            Program program = programs.get(i);
            if(program.id.equals(selectedProgram.id)) programSpinner.setSelection(i);
        }
        programSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedProgram = programs.get(position);
                invalidate();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void setupUi(View rootView) {
        if(selectedOrganisationUnit== null || selectedProgram == null) return;

        enrollmentCardView = (CardView) rootView.findViewById(R.id.enrollment_cardview);
        profileCardView = (CardView) rootView.findViewById(R.id.profile_cardview);
        programStageCardView = (CardView) rootView.findViewById(R.id.programstages_cardview);
        enrollmentHistoryButton = (Button) enrollmentCardView.findViewById(R.id.enrollmenthistorybutton);
        newEnrollmentButton = (Button) enrollmentCardView.findViewById(R.id.newenrollmentbutton);

        populateProgramData(selectedProgram);
    }

    /**
     * Populates the current fragment with data for the selected program
     * @param program
     */
    public void populateProgramData(Program program) {
        if(program == null) return;
        List<Enrollment> enrollments = DataValueController.getEnrollments(program.id, selectedTrackedEntityInstance);
        if(enrollments==null || enrollments.isEmpty()) {
            currentEnrollment = null;
        } else {
            for(Enrollment enrollment: enrollments) {
                if(enrollment.status.equals(Enrollment.ACTIVE)) {
                    currentEnrollment = enrollment;
                }
            }
        }

        enrollmentHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEnrollmentHistory();
            }
        });

        newEnrollmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enroll();
            }
        });

        if(currentEnrollment == null) {
            newEnrollmentButton.setVisibility(View.VISIBLE);
        } else {
            newEnrollmentButton.setVisibility(View.INVISIBLE);
        }

        /**
         * Setting data for enrollment
         */
        enrollmentContainer = (LinearLayout) enrollmentCardView.findViewById(R.id.enrollmentrowcontainer);
        completeButton = (Button) enrollmentCardView.findViewById(R.id.button_complete);
        terminateButton = (Button) enrollmentCardView.findViewById(R.id.button_terminate);
        markForFollowupButton = (Button) enrollmentCardView.findViewById(R.id.button_flagfollowup);

        if( currentEnrollment == null ) {
            //todo: show no enrollment ..
        } else {
            completeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    completeEnrollment();
                }
            });
            terminateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    terminateEnrollment();
                }
            });
            markForFollowupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    flagForFollowup();
                }
            });

            LinearLayout dateOfEnrollmentLayout = (LinearLayout) activity.getLayoutInflater().inflate(org.hisp.dhis2.android.sdk.R.layout.eventlistlinearlayoutitem, enrollmentContainer, false);
            TextView dateOfEnrollmentDescription = new TextView(activity);
            dateOfEnrollmentDescription.setWidth(0);
            dateOfEnrollmentDescription.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
            dateOfEnrollmentDescription.setMaxLines(1);
            dateOfEnrollmentDescription.setMinLines(1);
            dateOfEnrollmentDescription.setText(selectedProgram.dateOfEnrollmentDescription);
            dateOfEnrollmentLayout.addView(dateOfEnrollmentDescription);
            TextView dateOfEnrollment = new TextView(activity);
            dateOfEnrollment.setWidth(0);
            dateOfEnrollment.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
            dateOfEnrollment.setMaxLines(1);
            dateOfEnrollment.setMinLines(1);
            dateOfEnrollment.setText(currentEnrollment.dateOfEnrollment);
            dateOfEnrollmentLayout.addView(dateOfEnrollment);

            enrollmentContainer.addView(dateOfEnrollmentLayout);

            LinearLayout dateOfIncidentLayout = (LinearLayout) activity.getLayoutInflater().inflate(org.hisp.dhis2.android.sdk.R.layout.eventlistlinearlayoutitem, enrollmentContainer, false);
            TextView dateOfIncidentDescription = new TextView(activity);
            dateOfIncidentDescription.setWidth(0);
            dateOfIncidentDescription.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
            dateOfIncidentDescription.setMaxLines(1);
            dateOfIncidentDescription.setMinLines(1);
            dateOfIncidentDescription.setText(selectedProgram.dateOfIncidentDescription);
            dateOfIncidentLayout.addView(dateOfIncidentDescription);
            TextView dateOfIncident = new TextView(activity);
            dateOfIncident.setWidth(0);
            dateOfIncident.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
            dateOfIncident.setMaxLines(1);
            dateOfIncident.setMinLines(1);
            dateOfIncident.setText(currentEnrollment.dateOfIncident);
            dateOfIncidentLayout.addView(dateOfIncident);

            enrollmentContainer.addView(dateOfIncidentLayout);
        }

        /**
         * Setting data for profile
         */
        profileContainer = (LinearLayout) profileCardView.findViewById(R.id.attributerowcontainer);
        List<ProgramTrackedEntityAttribute> attributes;
        if( currentEnrollment == null ) {
            attributes = new ArrayList<>(); //todo: show non program specific attributes
        } else {
            attributes = selectedProgram.getProgramTrackedEntityAttributes();
        }
        for(ProgramTrackedEntityAttribute ptea: attributes) {
            TrackedEntityAttribute tea = ptea.getTrackedEntityAttribute();
            if(tea != null) {
                TrackedEntityAttributeValue teav = DataValueController.
                        getTrackedEntityAttributeValue(tea.id,
                                selectedTrackedEntityInstance.trackedEntityInstance);
                String valueString;
                if(teav != null) valueString = teav.value;
                else valueString = "";

                LinearLayout ll = (LinearLayout) activity.getLayoutInflater().
                        inflate(org.hisp.dhis2.android.sdk.R.layout.eventlistlinearlayoutitem,
                                profileContainer, false);
                TextView label = new TextView(activity);
                label.setWidth(0);
                label.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                label.setMaxLines(1);
                label.setMinLines(1);
                label.setText(tea.getName());
                ll.addView(label);
                TextView value = new TextView(activity);
                value.setWidth(0);
                value.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
                value.setMaxLines(1);
                value.setMinLines(1);
                value.setText(valueString);
                ll.addView(value);

                profileContainer.addView(ll);
            }
        }

        /**
         * Populate data for Program stages
         */

        programStageContainer = (LinearLayout) programStageCardView.findViewById(R.id.programstagerowcontainer);
        if( currentEnrollment == null ) {
            //todo: show no enrollment ...
        } else {
            List<Event> events = DataValueController.getEventsByEnrollment(currentEnrollment.enrollment);
            List<ProgramStage> programStages = selectedProgram.getProgramStages();
            for(ProgramStage programStage: programStages) {
                LinearLayout programStageLayout = (LinearLayout) activity.getLayoutInflater().
                        inflate(org.hisp.dhis2.android.sdk.R.layout.programstagelayout,
                                programStageContainer, false);
                Button createRepeatableEventButton = (Button) programStageLayout.findViewById(R.id.neweventbutton);
                createRepeatableEventButton.setContentDescription(programStage.id);
                if(!programStage.repeatable) {
                    createRepeatableEventButton.setVisibility(View.INVISIBLE);
                } else {
                    createRepeatableEventButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            createNewEvent(v.getContentDescription().toString());
                        }
                    });
                }
                TextView programStageName = (TextView) programStageLayout.findViewById(R.id.programstagename);
                programStageName.setText(programStage.getName());
                for(Event event: events) {
                    if(event.programStageId.equals(programStage.id)) {
                        LinearLayout eventLayout = (LinearLayout) activity.getLayoutInflater().
                                inflate(org.hisp.dhis2.android.sdk.R.layout.eventlayout,
                                        programStageLayout, false);
                        eventLayout.setContentDescription(event.localId+"");
                        TextView organisationUnit = (TextView) eventLayout.findViewById(R.id.organisationunit);
                        TextView date = (TextView) eventLayout.findViewById(R.id.date);
                        String organisationUnitLabel = getString(R.string.remote);
                        OrganisationUnit eventOrgUnit = MetaDataController.getOrganisationUnit(event.organisationUnitId);
                        if(eventOrgUnit!=null) organisationUnitLabel = eventOrgUnit.label;
                        organisationUnit.setText(organisationUnitLabel);
                        if(event.eventDate!=null)
                            date.setText(event.eventDate);
                        else if(event.dueDate!=null)
                            date.setText(event.dueDate);
                        else
                            date.setText("");

                        if(event.status.equals(Event.STATUS_ACTIVE)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_visited));
                        } else if(event.status.equals(Event.STATUS_COMPLETED)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_Completed));
                        } else if(event.status.equals(Event.STATUS_FUTURE_VISIT)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_Scheduled_in_future));
                        } else if(event.status.equals(Event.STATUS_LATE_VISIT)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_Overdue));
                        } else if(event.status.equals(Event.STATUS_SKIPPED)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_Skipped));
                        } else if(event.status.equals(Event.STATUS_VISITED)) {
                            eventLayout.setBackgroundColor(getResources().getColor(R.color.stage_visited));
                        }

                        programStageLayout.addView(eventLayout);
                        eventLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String localIdString = v.getContentDescription().toString();
                                long localId = Long.parseLong(localIdString);
                                editEvent(localId);
                            }
                        });
                    }
                }
                if(programStageLayout.getChildCount() <= 0) {
                    //todo: no event exists, create a view. Or determine that there is no event some
                    //todo: other way
                }
                programStageContainer.addView(programStageLayout);
            }
        }
    }

    public void completeEnrollment() {
        Dhis2.showErrorDialog(activity, "not implemented", "not implemented");
        //needs update in API
        /*if(currentEnrollment!=null && !currentEnrollment.status.equals(Enrollment.COMPLETED)) {
            currentEnrollment.status = Enrollment.COMPLETED;
            currentEnrollment.fromServer = false;
            //currentEnrollment.save(false);
            //todo: server gives duplicate error - figure out how to update an enrollment rather than create new..
            invalidate();
        }*/
    }

    public void terminateEnrollment() {
        //todo: implement
        Dhis2.showErrorDialog(activity, "not implemented", "not implemented");
    }

    public void flagForFollowup() {
        //todo: implement
        Dhis2.showErrorDialog(activity, "not implemented", "not implemented");
    }

    public void editEvent(long localId) {
        selectedEvent = DataValueController.getEvent(localId);
        selectedProgramStage = MetaDataController.getProgramStage(selectedEvent.programStageId);
        MessageEvent event = new MessageEvent(BaseEvent.EventType.showDataEntryFragment);
        Dhis2Application.bus.post(event);
    }

    public void createNewEvent(String programStageId) {
        if(currentEnrollment.status.equals(Enrollment.COMPLETED)) return;
        selectedEvent = null;
        selectedProgramStage = MetaDataController.getProgramStage(programStageId);
        MessageEvent event = new MessageEvent(BaseEvent.EventType.showDataEntryFragment);
        Dhis2Application.bus.post(event);
    }

    public void showEnrollmentHistory() {
        //todo: implement
        Dhis2.showErrorDialog(activity, "not implemented", "not implemented");
    }

    public void enroll() {
        MessageEvent messageEvent = new MessageEvent(BaseEvent.EventType.showEnrollmentFragment);
        Dhis2Application.bus.post(messageEvent);
    }

    public void populateSpinner( CardSpinner spinner, List<String> list )
    {
        if(activity == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( activity,
                R.layout.spinner_item, list );
        spinner.setAdapter( adapter );
    }

    /**
     * Reloads enrollment data and repopulates
     */
    public void invalidate() {
        if(activity==null)return;
        Log.d(CLASS_TAG, "invalidate !");
        activity.runOnUiThread(new Thread() {
            public void run() {
                if(rootView != null) {
                    enrollmentContainer.removeAllViews();
                    profileContainer.removeAllViews();
                    programStageContainer.removeAllViews();
                    setupUi(rootView);
                }
            }
        });
    }

    public void setSelectedProgram(Program program) {
        this.selectedProgram = program;
    }

    public void setSelectedOrganisationUnit(OrganisationUnit organisationUnit) {
        this.selectedOrganisationUnit = organisationUnit;
    }

    public void setSelectedTrackedEntityInstance(TrackedEntityInstance trackedEntityInstance) {
        this.selectedTrackedEntityInstance = trackedEntityInstance;
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public Program getSelectedProgram() {
        return selectedProgram;
    }

    public OrganisationUnit getSelectedOrganisationUnit() {
        return selectedOrganisationUnit;
    }

    public TrackedEntityInstance getSelectedTrackedEntityInstance() {
        return selectedTrackedEntityInstance;
    }

    public Enrollment getCurrentEnrollment() {
        return currentEnrollment;
    }

    public ProgramStage getSelectedProgramStage() {
        return selectedProgramStage;
    }

    @Subscribe
    public void onReceiveMessage(InvalidateEvent event) {
        Log.d(CLASS_TAG, "onreceivemessage.");
        if(event.eventType.equals(InvalidateEvent.EventType.event) ||
                event.eventType.equals(InvalidateEvent.EventType.enrollment)){
            invalidate();
        }
    }
}
