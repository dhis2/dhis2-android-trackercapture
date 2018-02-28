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

package org.hisp.dhis.android.trackercapture.fragments.upcomingevents;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.dialogs.UpcomingEventsDialogFilter;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.UpcomingEventItemRow;
import org.hisp.dhis.android.trackercapture.ui.rows.upcomingevents.UpcomingEventsColumnNamesRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UpcomingEventsFragmentQuery implements Query<SelectProgramFragmentForm> {
    private final String mOrgUnitId;
    private final String mProgramId;
    private final String mFilterLabel;
    private final String mStartDate;
    private final String mEndDate;

    private final int mNumAttributesToShow = 2;

    public UpcomingEventsFragmentQuery(String orgUnitId, String programId, String filterLabel,
                                       String startDate,
                                       String endDate) {
        mOrgUnitId = orgUnitId;
        mProgramId = programId;
        mFilterLabel = filterLabel;
        mStartDate = startDate;
        mEndDate = endDate;
    }

    @Override
    public SelectProgramFragmentForm query(Context context) {
        List<EventRow> eventUpcomingEventRows = new ArrayList<>();
        SelectProgramFragmentForm fragmentForm = new SelectProgramFragmentForm();

        // create a list of EventItems
        Program selectedProgram = MetaDataController.getProgram(mProgramId);
        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return fragmentForm;
        }

        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes =
                selectedProgram.getProgramTrackedEntityAttributes();

        List<String> attributesToShow = new ArrayList<>();
        UpcomingEventsColumnNamesRow columnNames = new UpcomingEventsColumnNamesRow();
        String title = context.getString(R.string.events);
        if(mFilterLabel!=null){
            title = getLabelTitle(mFilterLabel, context);
        }
        columnNames.setTitle(title);
        for (ProgramTrackedEntityAttribute attribute : programTrackedEntityAttributes) {
            if (attribute.getDisplayInList() && attributesToShow.size() < mNumAttributesToShow) {
                attributesToShow.add(attribute.getTrackedEntityAttributeId());
                if (attribute.getTrackedEntityAttribute() != null) {
                    String name = attribute.getTrackedEntityAttribute().getName();
                    if (attributesToShow.size() == 1) {
                        columnNames.setFirstItem(name);
                    } else if (attributesToShow.size() == 2) {
                        columnNames.setSecondItem(name);
                    }
                }
            }
        }
        eventUpcomingEventRows.add(columnNames);
        List<Event> events = new ArrayList<>();

        if(UpcomingEventsDialogFilter.Type.UPCOMING.toString().equalsIgnoreCase(mFilterLabel)) {
             events = TrackerController.getScheduledEventsWithActiveEnrollments(
                    mProgramId, mOrgUnitId, mStartDate, mEndDate );
        }
        else if(UpcomingEventsDialogFilter.Type.OVERDUE.toString().equalsIgnoreCase(mFilterLabel)) {
            events = TrackerController.getOverdueEventsWithActiveEnrollments(
                    mProgramId, mOrgUnitId);
        }
        else if(UpcomingEventsDialogFilter.Type.ACTIVE.toString().equalsIgnoreCase(mFilterLabel)) {
            events = TrackerController.getActiveEventsWithActiveEnrollments(mProgramId, mOrgUnitId,mStartDate,mEndDate);
        }
        else {
            // if not UPCOMING, OVERDUE or ACTIVE, then it is FOLLOW_UP
            // NOT YET IMPLEMENTED
        }

        List<Option> options = new Select().from(Option.class).queryList();
        Map<String, String> codeToName = new HashMap<>();
        for (Option option : options) {
            codeToName.put(option.getCode(), option.getName());
        }

        for (Event event : events) {
            eventUpcomingEventRows.add(createEventItem(event, attributesToShow,
                    codeToName));
        }
        fragmentForm.setEventRowList(eventUpcomingEventRows);
        fragmentForm.setProgram(selectedProgram);
        return fragmentForm;
    }

    private String getLabelTitle(String filterLabel, Context context) {
        if (UpcomingEventsDialogFilter.Type.UPCOMING.toString().equalsIgnoreCase(mFilterLabel)) {
            return context.getString(R.string.upcoming_events);
        } else if (UpcomingEventsDialogFilter.Type.OVERDUE.toString().equalsIgnoreCase(
                mFilterLabel)) {
            return context.getString(R.string.overdue_events);
        } else if (UpcomingEventsDialogFilter.Type.ACTIVE.toString().equalsIgnoreCase(
                mFilterLabel)) {
            return context.getString(R.string.active_events);
        }
        return "";
    }

    private UpcomingEventItemRow createEventItem(Event event, List<String> attributesToShow,
                                                 Map<String, String> codeToName) {
        UpcomingEventItemRow eventItem = new UpcomingEventItemRow();
        eventItem.setEventId(event.getLocalId());
        eventItem.setDueDate(event.getDueDate());
        eventItem.setEventName(MetaDataController.getProgramStage(event.getProgramStageId()).
                getName());

        for (int i = 0; i < mNumAttributesToShow; i++) {
            if (i >= attributesToShow.size()) break;
            String trackedEntityAttribute = attributesToShow.get(i);
            if (trackedEntityAttribute != null) {
                TrackedEntityAttributeValue trackedEntityAttributeValue = TrackerController.
                        getTrackedEntityAttributeValue(trackedEntityAttribute,
                                event.getTrackedEntityInstance());
                if (trackedEntityAttributeValue == null) {
                    continue;
                }

                String code = trackedEntityAttributeValue.getValue();
                String name = codeToName.get(code) == null ? code : codeToName.get(code);

                if (i == 0) {
                    eventItem.setFirstItem(name);
                } else if (i == 1) {
                    eventItem.setSecondItem(name);
                }
            }
        }
        return eventItem;
    }

    private static <T> boolean isListEmpty(List<T> items) {
        return items == null || items.isEmpty();
    }
}