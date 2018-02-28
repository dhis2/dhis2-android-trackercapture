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

package org.hisp.dhis.android.trackercapture.fragments.programoverview.registerrelationshipdialogfragment;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.trackercapture.ui.rows.programoverview.SearchRelativeTrackedEntityInstanceItemRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Simen S. Russnes on 7/9/15.
 */
public class RegisterRelationshipDialogFragmentQuery implements Query<RegisterRelationshipDialogFragmentForm>
{
    public static final String TAG = RegisterRelationshipDialogFragmentQuery.class.getSimpleName();
    private long trackedEntityInstanceId;
    private String activeProgramUid;
    private final int NUMBER_OF_ATTRIBUTES = 4;

    public RegisterRelationshipDialogFragmentQuery(long trackedEntityInstanceId, String activeProgramUid)
    {
        this.trackedEntityInstanceId = trackedEntityInstanceId;
        this.activeProgramUid = activeProgramUid;
    }

    @Override
    public RegisterRelationshipDialogFragmentForm query(Context context)
    {
        RegisterRelationshipDialogFragmentForm form = new RegisterRelationshipDialogFragmentForm();
        TrackedEntityInstance trackedEntityInstance = TrackerController.getTrackedEntityInstance(trackedEntityInstanceId);
        if(trackedEntityInstance==null) {
            return form;
        }
        form.setTrackedEntityInstance(trackedEntityInstance);

        List<TrackedEntityInstance> trackedEntityInstances = new Select().from(TrackedEntityInstance.class).queryList();
        if(trackedEntityInstances == null)
            return form;

        List<EventRow> teiRows = new ArrayList<>();

        for (TrackedEntityInstance tei : trackedEntityInstances) {
            if(trackedEntityInstance==null ||
                    tei.getLocalId() == trackedEntityInstanceId) {
                //avoid adding the current TEI to the list of TEIs to form relationship with
                continue;
            }
            teiRows.add(createTrackedEntityInstanceItem(context,
                    tei, NUMBER_OF_ATTRIBUTES, activeProgramUid));
        }

        form.setRows(teiRows);
        return form;
    }

    private SearchRelativeTrackedEntityInstanceItemRow createTrackedEntityInstanceItem(Context context, TrackedEntityInstance trackedEntityInstance, int numberOfAttributes, String activeProgramUid) {
        SearchRelativeTrackedEntityInstanceItemRow trackedEntityInstanceItemRow = new SearchRelativeTrackedEntityInstanceItemRow(context);
        trackedEntityInstanceItemRow.setTrackedEntityInstance(trackedEntityInstance);
        if(trackedEntityInstance.getAttributes()==null) {
            return trackedEntityInstanceItemRow;
        }

        //checking if the tei has an enrollment so that we can order the displayed attributes
        //in some logical fashion
        List<Enrollment> enrollments = TrackerController.getEnrollments(trackedEntityInstance);
        Program activeProgram = MetaDataController.getProgram(activeProgramUid);

        List<TrackedEntityAttribute> attributesToShow = new ArrayList<>();
        if(enrollments!=null && !enrollments.isEmpty()) {
            Program program = null;
            for(Enrollment e: enrollments) {
                if(e!=null && e.getProgram()!=null && e.getProgram().getProgramTrackedEntityAttributes()!=null) {
                    program = e.getProgram();
                    if(program.getUid().equals(activeProgram.getUid())) {
                        break;
                    }
                }
            }
            List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes = program.getProgramTrackedEntityAttributes();
            for(int i = 0; i<programTrackedEntityAttributes.size() && i<numberOfAttributes; i++) {
                attributesToShow.add(programTrackedEntityAttributes.get(i).getTrackedEntityAttribute());
            }
        }

        List<TrackedEntityAttributeValue> attributes = new ArrayList<>();
        for(int i=0; i<NUMBER_OF_ATTRIBUTES; i++)
        {
            String value = "";
            if(attributesToShow==null || attributesToShow.size()<=i) {
                if(trackedEntityInstance.getAttributes().size()>i && trackedEntityInstance.getAttributes().get(i) != null && trackedEntityInstance.getAttributes().get(i).getValue()!=null) {
                    attributes.add(trackedEntityInstance.getAttributes().get(i));
                }
            } else {
                TrackedEntityAttributeValue av = TrackerController.getTrackedEntityAttributeValue(attributesToShow.get(i).getUid(), trackedEntityInstance.getLocalId());
                if(av!=null && av.getValue()!=null) {
                    value = av.getValue();
                    trackedEntityInstanceItemRow.addColumn(value);
                }
            }
        }
        List<ProgramTrackedEntityAttribute> programAttributesOrdered = MetaDataController.getProgramTrackedEntityAttributes(activeProgram.getUid());
        for(ProgramTrackedEntityAttribute programTrackedEntityAttribute : programAttributesOrdered){
            boolean hasAttribute = false;
            for(TrackedEntityAttributeValue attributeValue:attributes){
                if(programTrackedEntityAttribute.getTrackedEntityAttributeId().equals(attributeValue.getTrackedEntityAttributeId())){
                    String value="";
                    if(attributeValue.getValue()!=null) {
                        value = attributeValue.getValue();
                        hasAttribute=true;
                    }
                    trackedEntityInstanceItemRow.addColumn(value);
                }
            }
            if(!hasAttribute){
                trackedEntityInstanceItemRow.addColumn("");
            }
        }
        return trackedEntityInstanceItemRow;
    }


}
