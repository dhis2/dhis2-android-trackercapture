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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.Event$Table;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttribute$Table;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue$Table;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.sdk.utils.AttributeListAdapter;
import org.hisp.dhis2.android.trackercapture.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * @author Simen Skogly Russnes on 20.02.15.
 */
public class SelectProgramFragment extends Fragment {

    private static final String CLASS_TAG = "SelectProgramFragment";

    private List<OrganisationUnit> assignedOrganisationUnits;
    private OrganisationUnit selectedOrganisationUnit;
    private List<Program> programsForSelectedOrganisationUnit;
    private List<String> trackedEntityIds;

    private Spinner organisationUnitSpinner;
    private Spinner programSpinner;
    private Button registerButton;
    private ListView trackedEntityInstancesListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_select_program,
                container, false);
        setupUi(rootView);
        return rootView;
    }

    public void setupUi(View rootView) {
        organisationUnitSpinner = (Spinner) rootView.findViewById(R.id.selectprogram_orgunit_spinner);
        programSpinner = (Spinner) rootView.findViewById(R.id.selectprogram_program_spinner);
        registerButton = (Button) rootView.findViewById(R.id.selectprogram_register_button);
        trackedEntityInstancesListView = (ListView) rootView.findViewById(R.id.selectprogram_resultslistview);
        assignedOrganisationUnits = Dhis2.getInstance().
                getMetaDataController().getAssignedOrganisationUnits();
        if( assignedOrganisationUnits==null || assignedOrganisationUnits.size() <= 0 ) return;

        List<String> organisationUnitNames = new ArrayList<String>();
        for( OrganisationUnit ou: assignedOrganisationUnits )
            organisationUnitNames.add(ou.getLabel());
        populateSpinner(organisationUnitSpinner, organisationUnitNames);

        organisationUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOrganisationUnit = assignedOrganisationUnits.get(position); //displaying first as default
                programsForSelectedOrganisationUnit = Dhis2.getInstance().getMetaDataController().
                        getProgramsForOrganisationUnit(selectedOrganisationUnit.getId(),
                                Program.MULTIPLE_EVENTS_WITH_REGISTRATION);
                if(programsForSelectedOrganisationUnit == null ||
                        programsForSelectedOrganisationUnit.size() <= 0) {
                    populateSpinner(programSpinner, new ArrayList<String>());
                } else {
                    List<String> programNames = new ArrayList<String>();
                    for( Program p: programsForSelectedOrganisationUnit )
                        programNames.add(p.getName());
                    populateSpinner(programSpinner, programNames);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        programSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onProgramSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterEventFragment();
            }
        });
    }

    public void onProgramSelected(int position) {
        Log.d(CLASS_TAG, "onprogramselected");
        if(programsForSelectedOrganisationUnit!=null) {
            Program program = programsForSelectedOrganisationUnit.get(position);

            //get all unique TEI that have enrollment in the selected program/orgunit
            List<Event> events = DataValueController.getEvents(selectedOrganisationUnit.id, program.id);
            trackedEntityIds = new ArrayList<>();
            for(Event event: events) {
                if(event.trackedEntityInstance != null) {
                    if(!trackedEntityIds.contains(event.trackedEntityInstance))
                        trackedEntityIds.add(event.trackedEntityInstance);
                }
            }

            //get attributevalues to show in list:
            List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes
                    = program.getProgramTrackedEntityAttributes();
            List<TrackedEntityAttribute> trackedEntityAttributes = new ArrayList<>();
            for(ProgramTrackedEntityAttribute programTrackedEntityAttribute: programTrackedEntityAttributes) {
                if(programTrackedEntityAttribute.displayInList)
                    trackedEntityAttributes.add(programTrackedEntityAttribute.getTrackedEntityAttribute());
            }

            //get values and show in list
            HashMap<String, String[]> rows = new HashMap<>();
            for(String trackedEntityInstance: trackedEntityIds) {
                String[] row = new String[trackedEntityAttributes.size()];
                for(int i=0; i<trackedEntityAttributes.size(); i++) {
                    TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributes.get(i);
                    List<TrackedEntityAttributeValue> result = Select.all(TrackedEntityAttributeValue.class,
                            Condition.column(TrackedEntityAttributeValue$Table.
                                    TRACKEDENTITYATTRIBUTEID).is(trackedEntityAttribute.id),
                            Condition.column(TrackedEntityAttributeValue$Table.
                                    TRACKEDENTITYINSTANCEID).is(trackedEntityInstance));
                    if(result!=null && !result.isEmpty()) {
                        row[i] = result.get(0).value;
                    }
                    else row[i] = " ";
                }
                rows.put(trackedEntityInstance, row);
            }

            ArrayList<String[]> values = new ArrayList<>();
            for(String s: trackedEntityIds) {
                values.add(rows.get(s));


            }

            trackedEntityInstancesListView.setAdapter(new AttributeListAdapter(getActivity(), values));

        }
    }

    public void populateSpinner( Spinner spinner, List<String> list )
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( getActivity(),
                android.R.layout.simple_spinner_dropdown_item, list );
        spinner.setAdapter( adapter );
    }

    public void showRegisterEventFragment() {
        if( selectedOrganisationUnit == null || programSpinner.getSelectedItemPosition() < 0)
            return;
        MessageEvent event = new MessageEvent(BaseEvent.EventType.showRegisterEventFragment);
        Dhis2Application.bus.post(event);
    }

    public OrganisationUnit getSelectedOrganisationUnit() {
        return selectedOrganisationUnit;
    }

    public Program getSelectedProgram() {
        Program selectedProgram = programsForSelectedOrganisationUnit.
                get(programSpinner.getSelectedItemPosition());
        return selectedProgram;
    }
}
