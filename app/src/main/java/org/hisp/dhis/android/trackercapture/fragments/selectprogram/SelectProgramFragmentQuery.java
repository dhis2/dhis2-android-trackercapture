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

package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventItemRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SelectProgramFragmentQuery implements Query<SelectProgramFragmentForm> {
    private final String mOrgUnitId;
    private final String mProgramId;

    public SelectProgramFragmentQuery(String orgUnitId, String programId) {
        mOrgUnitId = orgUnitId;
        mProgramId = programId;
    }

    @Override
    public SelectProgramFragmentForm query(Context context) {

        SelectProgramFragmentForm fragmentForm = new SelectProgramFragmentForm();
        List<EventRow> teiRows = new ArrayList<>();

        // create a list of EventItems
        Program selectedProgram = MetaDataController.getProgram(mProgramId);
        fragmentForm.setProgram(selectedProgram);
        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return fragmentForm;
        }

        // since this is single event its only 1 stage
        ProgramStage programStage = selectedProgram.getProgramStages().get(0);
        if (programStage == null || isListEmpty(programStage.getProgramStageDataElements())) {
            return fragmentForm;
        }

        List<ProgramTrackedEntityAttribute> attributes = selectedProgram.getProgramTrackedEntityAttributes();
        if (isListEmpty(attributes)) {
            return fragmentForm;
        }

        List<String> attributesToShow = new ArrayList<>();
        TrackedEntityInstanceColumnNamesRow columnNames = new TrackedEntityInstanceColumnNamesRow();

        for (ProgramTrackedEntityAttribute attribute : attributes) {
            if (attribute.getDisplayInList() && attributesToShow.size() < 3) {
                attributesToShow.add(attribute.getTrackedEntityAttributeId());
                if (attribute.getTrackedEntityAttribute() != null) {
                    String name = attribute.getTrackedEntityAttribute().getName();
                    if (attributesToShow.size() == 1) {
                        columnNames.setFirstItem(name);
                    } else if (attributesToShow.size() == 2) {
                        columnNames.setSecondItem(name);
                    } else if (attributesToShow.size() == 3) {
                        columnNames.setThirdItem(name);
                    }

                }
            }
        }
        teiRows.add(columnNames);


        if(!selectedProgram.isDisplayFrontPageList()) {
            return fragmentForm; // we don't want to show any values or any list header
        }

        List<Enrollment> enrollments = TrackerController.getEnrollments(
                mProgramId, mOrgUnitId);
        List<Event> eventsForOrgUnit = TrackerController.getEventsThatHasEnrollments(mOrgUnitId,mProgramId);
        List<Enrollment> enrollmentsToShow = new ArrayList<>();
        Map<String,Enrollment> enrollmentMap = new HashMap<>();
        for (Event event : eventsForOrgUnit) {
            if(!enrollmentMap.containsKey(event.getEnrollment())) {
                enrollmentMap.put(event.getEnrollment(), TrackerController.getEnrollment(event.getEnrollment()));
            }
        }

        List<Long> trackedEntityInstanceIds = new ArrayList<>();
        if (isListEmpty(enrollments)) {
            return fragmentForm;
        } else {
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getLocalTrackedEntityInstanceId() > 0) {
                    if (!trackedEntityInstanceIds.contains(enrollment.getLocalTrackedEntityInstanceId()))
                        trackedEntityInstanceIds.add(enrollment.getLocalTrackedEntityInstanceId());
                }
            }
        }
        List<TrackedEntityInstance> trackedEntityInstanceList = new ArrayList<>();
        if (!isListEmpty(trackedEntityInstanceIds)) {
            for (long localId : trackedEntityInstanceIds) {
                TrackedEntityInstance tei = TrackerController.getTrackedEntityInstance(localId);
                trackedEntityInstanceList.add(tei);
            }
        }

        List<TrackedEntityAttribute> trackedEntityAttributes = new Select().from(TrackedEntityAttribute.class).queryList();
        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap = new HashMap<>();
        for(TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributes) {
            trackedEntityAttributeMap.put(trackedEntityAttribute.getUid(), trackedEntityAttribute);
        }

        List<FailedItem> failedEvents = TrackerController.getFailedItems(FailedItem.TRACKEDENTITYINSTANCE);

        Set<String> failedEventIds = new HashSet<>();
        for (FailedItem failedItem : failedEvents) {
            TrackedEntityInstance tei = (TrackedEntityInstance) failedItem.getItem();
            if(tei == null) {
                failedItem.delete();
            } else {
                if(failedItem.getHttpStatusCode()>=0) {
                    failedEventIds.add(tei.getTrackedEntityInstance());
                }
            }
        }

        for (TrackedEntityInstance trackedEntityInstance : trackedEntityInstanceList) {
            if (trackedEntityInstance == null) continue;
            teiRows.add(createTrackedEntityInstanceItem(context,
                    trackedEntityInstance, attributesToShow, attributes,
                    trackedEntityAttributeMap, failedEventIds));
        }

        fragmentForm.setEventRowList(teiRows);
        fragmentForm.setColumnNames(columnNames);

        if(selectedProgram.getTrackedEntity() != null) {
            columnNames.setTrackedEntity(selectedProgram.getTrackedEntity().getName());
            columnNames.setTitle(selectedProgram.getTrackedEntity().getName() + " (" + ( teiRows.size() - 1 ) + ")") ;
        }

        return fragmentForm;
    }

    private EventRow createTrackedEntityInstanceItem(Context context, TrackedEntityInstance trackedEntityInstance,
                                                                         List<String> attributesToShow, List<ProgramTrackedEntityAttribute> attributes,
                                                                         Map<String, TrackedEntityAttribute> trackedEntityAttributeMap,
                                                                         Set<String> failedEventIds) {
        TrackedEntityInstanceItemRow trackedEntityInstanceItemRow = new TrackedEntityInstanceItemRow(context);
        trackedEntityInstanceItemRow.setTrackedEntityInstance(trackedEntityInstance);

        if (trackedEntityInstance.isFromServer()) {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.SENT);
        } else if (failedEventIds.contains(trackedEntityInstance.getTrackedEntityInstance())) {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.ERROR);
        } else {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.OFFLINE);
        }

        for (int i = 0; i < attributesToShow.size(); i++) {
            if (i > attributesToShow.size()) break;

            String attributeUid = attributesToShow.get(i);
            if (attributeUid != null) {
                TrackedEntityAttributeValue teav = TrackerController.getTrackedEntityAttributeValue(attributeUid, trackedEntityInstance.getLocalId());

                TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeMap.get(attributeUid);
                if (teav == null || trackedEntityAttribute == null) {
                    continue;
                }

                String value = teav.getValue();
                if(trackedEntityAttribute.isOptionSetValue()) {
                    if(trackedEntityAttribute.getOptionSet() == null) {
                        continue;
                    }
                    OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.getOptionSet());
                    if(optionSet == null) {
                        continue;
                    }
                    List<Option> options = MetaDataController.getOptions(optionSet.getUid());
                    if(options == null) {
                        continue;
                    }
                    for(Option option : options) {
                        if(option.getCode().equals(value)) {
                            value = option.getName();
                        }
                    }
                }

                if (i == 0) {
                    trackedEntityInstanceItemRow.setFirstItem(value);
                } else if (i == 1) {
                    trackedEntityInstanceItemRow.setSecondItem(value);
                } else if (i == 2) {
                    trackedEntityInstanceItemRow.setThirdItem(value);
                }
            }
        }
        return trackedEntityInstanceItemRow;
    }

    private static <T> boolean isListEmpty(List<T> items) {
        return items == null || items.isEmpty();
    }
}
