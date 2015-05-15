/*
 * Copyright (c) 2015, dhis2
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis2.android.trackercapture.fragments.upcomingevents;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Option;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStageDataElement;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis2.android.sdk.persistence.loaders.Query;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.UpcomingEventsColumnNamesRow;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.UpcomingEventItemRow;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.EventItemStatus;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.upcomingevents.UpcomingEventRow;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class UpcomingEventsFragmentQuery implements Query<List<UpcomingEventRow>> {
    private final String mOrgUnitId;
    private final String mProgramId;
    private final String mStartDate;
    private final String mEndDate;

    private final int mNumAttributesToShow = 2;

    public UpcomingEventsFragmentQuery(String orgUnitId, String programId, String startDate,
                                       String endDate) {
        mOrgUnitId = orgUnitId;
        mProgramId = programId;
        mStartDate = startDate;
        mEndDate = endDate;
    }

    @Override
    public List<UpcomingEventRow> query(Context context) {
        List<UpcomingEventRow> eventUpcomingEventRows = new ArrayList<>();

        // create a list of EventItems
        Program selectedProgram = Select.byId(Program.class, mProgramId);
        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return eventUpcomingEventRows;
        }

        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes =
                selectedProgram.getProgramTrackedEntityAttributes();

        List<String> attributesToShow = new ArrayList<>();
        UpcomingEventsColumnNamesRow columnNames = new UpcomingEventsColumnNamesRow();
        for (ProgramTrackedEntityAttribute attribute : programTrackedEntityAttributes) {
            if (attribute.displayInList && attributesToShow.size() < mNumAttributesToShow) {
                attributesToShow.add(attribute.trackedEntityAttribute);
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

        List<Event> events = DataValueController.getScheduledEvents(
                mProgramId, mOrgUnitId, mStartDate, mEndDate
        );
        if (isListEmpty(events)) {
            return eventUpcomingEventRows;
        }

        List<Option> options = Select.all(Option.class);
        Map<String, String> codeToName = new HashMap<>();
        for (Option option : options) {
            codeToName.put(option.getCode(), option.getName());
        }

        for (Event event : events) {
            eventUpcomingEventRows.add(createEventItem(event, attributesToShow,
                    codeToName));
        }

        return eventUpcomingEventRows;
    }

    private UpcomingEventItemRow createEventItem(Event event, List<String> attributesToShow,
                                         Map<String, String> codeToName) {
        UpcomingEventItemRow eventItem = new UpcomingEventItemRow();
        eventItem.setEventId(event.getLocalId());
        eventItem.setDueDate(event.dueDate);
        eventItem.setEventName(MetaDataController.getProgramStage(event.getProgramStageId()).
                getName());

        for (int i = 0; i < mNumAttributesToShow; i++) {
            if(i>=attributesToShow.size()) break;
            String trackedEntityAttribute = attributesToShow.get(i);
            if (trackedEntityAttribute != null) {
                TrackedEntityAttributeValue trackedEntityAttributeValue = DataValueController.
                        getTrackedEntityAttributeValue(trackedEntityAttribute,
                                event.trackedEntityInstance);
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