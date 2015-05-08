package org.hisp.dhis2.android.trackercapture.fragments;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Option;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStageDataElement;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntity;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.trackercapture.fragments.loaders.Query;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events.ColumnNamesRow;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events.EventItemRow;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events.EventItemStatus;
import org.hisp.dhis2.android.trackercapture.fragments.upcomingevents.Events.EventRow;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SelectProgramFragmentQuery implements Query<List<EventRow>> {
    private final String mOrgUnitId;
    private final String mProgramId;

    public SelectProgramFragmentQuery(String orgUnitId, String programId) {
        mOrgUnitId = orgUnitId;
        mProgramId = programId;
    }

    @Override
    public List<EventRow> query(Context context) {
        List<EventRow> eventEventRows = new ArrayList<>();

        // create a list of EventItems
        Program selectedProgram = Select.byId(Program.class, mProgramId);
        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return eventEventRows;
        }

        // since this is single event its only 1 stage
        ProgramStage programStage = selectedProgram.getProgramStages().get(0);
        if (programStage == null || isListEmpty(programStage.getProgramStageDataElements())) {
            return eventEventRows;
        }

        List<ProgramStageDataElement> stageElements = programStage
                .getProgramStageDataElements();
        if (isListEmpty(stageElements)) {
            return eventEventRows;
        }

        List<String> elementsToShow = new ArrayList<>();
        ColumnNamesRow columnNames = new ColumnNamesRow();
        for (ProgramStageDataElement stageElement : stageElements) {
            if (stageElement.displayInReports && elementsToShow.size() < 3) {
                elementsToShow.add(stageElement.dataElement);
                if (stageElement.getDataElement() != null) {
                    String name = stageElement.getDataElement().getName();
                    if (elementsToShow.size() == 1) {
                        columnNames.setFirstItem(name);
                    } else if (elementsToShow.size() == 2) {
                        columnNames.setSecondItem(name);
                    } else if (elementsToShow.size() == 3) {
                        columnNames.setThirdItem(name);
                    }
                }
            }
        }
        eventEventRows.add(columnNames);

        List<Event> events = DataValueController.getEvents(
                mOrgUnitId, mProgramId
        );
        if (isListEmpty(events)) {
            return eventEventRows;
        }

        List<Option> options = Select.all(Option.class);
        Map<String, String> codeToName = new HashMap<>();
        for (Option option : options) {
            codeToName.put(option.getCode(), option.getName());
        }

        List<FailedItem> failedEvents = Select.all(
                FailedItem.class, Condition
                        .column(FailedItem$Table.ITEMTYPE)
                        .is(FailedItem.EVENT)
        );

        Set<String> failedEventIds = new HashSet<>();
        for (FailedItem failedItem : failedEvents) {
            Event event = (Event) failedItem.getItem();
            failedEventIds.add(event.getEvent());
        }

        Collections.sort(events, new EventComparator());
        for (Event event : events) {
            eventEventRows.add(createEventItem(context,
                    event, elementsToShow,
                    codeToName, failedEventIds));
        }

        return eventEventRows;
    }

    private EventItemRow createEventItem(Context context, Event event, List<String> elementsToShow,
                                         Map<String, String> codeToName,
                                         Set<String> failedEventIds) {
        EventItemRow eventItem = new EventItemRow(context);
        eventItem.setEvent(event);

        if (event.isFromServer()) {
            eventItem.setStatus(EventItemStatus.SENT);
        } else if (failedEventIds.contains(event.getEvent())) {
            eventItem.setStatus(EventItemStatus.ERROR);
        } else {
            eventItem.setStatus(EventItemStatus.OFFLINE);
        }

        for (int i = 0; i < 3; i++) {
            if(i>=elementsToShow.size()) break;
            String dataElement = elementsToShow.get(i);
            if (dataElement != null) {
                DataValue dataValue = getDataValue(event, dataElement);
                if (dataValue == null) {
                    continue;
                }

                String code = dataValue.getValue();
                String name = codeToName.get(code) == null ? code : codeToName.get(code);

                if (i == 0) {
                    eventItem.setFirstItem(name);
                } else if (i == 1) {
                    eventItem.setSecondItem(name);
                } else if (i == 2) {
                    eventItem.setThirdItem(name);
                }
            }
        }
        return eventItem;
    }

    private DataValue getDataValue(Event event, String dataElement) {
        List<DataValue> dataValues = Select.all(
                DataValue.class,
                Condition.column(DataValue$Table.EVENT).is(event.event),
                Condition.column(DataValue$Table.DATAELEMENT).is(dataElement)
        );

        if (dataValues != null && !dataValues.isEmpty()) {
            return dataValues.get(0);
        } else {
            return null;
        }
    }

    private static <T> boolean isListEmpty(List<T> items) {
        return items == null || items.isEmpty();
    }

    private static class EventComparator implements Comparator<Event> {

        @Override
        public int compare(Event first, Event second) {
            if (first.getLastUpdated() == null || second.getLastUpdated() == null) {
                return 0;
            }

            DateTime firstDateTime = DateTime.parse(first.getLastUpdated());
            DateTime secondDateTime = DateTime.parse(second.getLastUpdated());

            if (firstDateTime.isBefore(secondDateTime)) {
                return 1;
            } else if (firstDateTime.isAfter(secondDateTime)) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
