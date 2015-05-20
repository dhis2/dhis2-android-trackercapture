package org.hisp.dhis2.android.trackercapture.fragments;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis2.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis2.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis2.android.sdk.persistence.models.DataValue;
import org.hisp.dhis2.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis2.android.sdk.persistence.models.Event;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis2.android.sdk.persistence.models.FailedItem$Table;
import org.hisp.dhis2.android.sdk.persistence.models.Option;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis2.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis2.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis2.android.trackercapture.fragments.selectprogram.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis2.android.trackercapture.fragments.selectprogram.TrackedEntityInstanceItemRow;
import org.hisp.dhis2.android.trackercapture.fragments.selectprogram.TrackedEntityInstanceItemStatus;
import org.hisp.dhis2.android.sdk.persistence.loaders.Query;

import org.hisp.dhis2.android.trackercapture.fragments.selectprogram.TrackedEntityInstanceRow;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SelectProgramFragmentQuery implements Query<List<TrackedEntityInstanceRow>> {
    private final String mOrgUnitId;
    private final String mProgramId;

    public SelectProgramFragmentQuery(String orgUnitId, String programId) {
        mOrgUnitId = orgUnitId;
        mProgramId = programId;
    }

    @Override
    public List<TrackedEntityInstanceRow> query(Context context) {
        List<TrackedEntityInstanceRow> teiRows = new ArrayList<>();

        // create a list of EventItems
        Program selectedProgram = MetaDataController.getProgram(mProgramId);
        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return teiRows;
        }

        // since this is single event its only 1 stage
        ProgramStage programStage = selectedProgram.getProgramStages().get(0);
        if (programStage == null || isListEmpty(programStage.getProgramStageDataElements())) {
            return teiRows;
        }

        List<ProgramTrackedEntityAttribute> attributes = selectedProgram.getProgramTrackedEntityAttributes();
        if (isListEmpty(attributes)) {
            return teiRows;
        }

        List<String> attributesToShow = new ArrayList<>();
        TrackedEntityInstanceColumnNamesRow columnNames = new TrackedEntityInstanceColumnNamesRow();
        for (ProgramTrackedEntityAttribute attribute : attributes) {
            if (attribute.displayInList && attributesToShow.size() < 3) {
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
        teiRows.add(columnNames);

        List<Enrollment> enrollments = DataValueController.getEnrollments(
                 mProgramId, mOrgUnitId);
        List<Long> trackedEntityInstanceIds = new ArrayList<>();
        if (isListEmpty(enrollments)) {return teiRows;}
        else{
            for(Enrollment enrollment : enrollments)
            {
                if(enrollment.localTrackedEntityInstanceId > 0)
                {
                    if(!trackedEntityInstanceIds.contains(enrollment.localTrackedEntityInstanceId))
                        trackedEntityInstanceIds.add(enrollment.localTrackedEntityInstanceId);
                }
            }
        }
        List<TrackedEntityInstance> trackedEntityInstanceList = new ArrayList<>();
        if(!isListEmpty(trackedEntityInstanceIds))
        {
            for(long localId : trackedEntityInstanceIds)
            {
                TrackedEntityInstance tei = DataValueController.getTrackedEntityInstance(localId);
                trackedEntityInstanceList.add(tei);
            }
        }

        List<Option> options = new Select().from(Option.class).queryList();
        Map<String, String> codeToName = new HashMap<>();
        for (Option option : options) {
            codeToName.put(option.getCode(), option.getName());
        }

        List<FailedItem> failedEvents = DataValueController.getFailedItems(FailedItem.TRACKEDENTITYINSTANCE);

        Set<String> failedEventIds = new HashSet<>();
        for (FailedItem failedItem : failedEvents) {
            TrackedEntityInstance tei = (TrackedEntityInstance) failedItem.getItem();
            failedEventIds.add(tei.getTrackedEntityInstance());
        }

//        Collections.sort(trackedEntityInstanceList, new EventComparator()); //not necessary to sort
        for (TrackedEntityInstance trackedEntityInstance : trackedEntityInstanceList) {
            teiRows.add(createTrackedEntityInstanceItem(context,
                    trackedEntityInstance, attributesToShow, attributes,
                    codeToName, failedEventIds));
        }

        return teiRows;
    }

    private TrackedEntityInstanceItemRow createTrackedEntityInstanceItem(Context context, TrackedEntityInstance trackedEntityInstance,
                                                                         List<String> attributesToShow, List<ProgramTrackedEntityAttribute> attributes,
                                         Map<String, String> codeToName,
                                         Set<String> failedEventIds) {
        TrackedEntityInstanceItemRow trackedEntityInstanceItemRow = new TrackedEntityInstanceItemRow(context);
        trackedEntityInstanceItemRow.setTrackedEntityInstance(trackedEntityInstance);

        if (trackedEntityInstance.fromServer) {
            trackedEntityInstanceItemRow.setStatus(TrackedEntityInstanceItemStatus.SENT);
        } else if (failedEventIds.contains(trackedEntityInstance.getTrackedEntityInstance())) {
            trackedEntityInstanceItemRow.setStatus(TrackedEntityInstanceItemStatus.ERROR);
        } else {
            trackedEntityInstanceItemRow.setStatus(TrackedEntityInstanceItemStatus.OFFLINE);
        }

        for(int i=0; i<attributesToShow.size(); i++)
        {
            if(i>attributesToShow.size()) break;

            String attribute = attributesToShow.get(i);
            if(attribute != null)
            {
                TrackedEntityAttributeValue teav = DataValueController.getTrackedEntityAttributeValue(attribute, trackedEntityInstance.localId);
                String code = teav.getValue();
                String name = codeToName.get(code) == null ? code : codeToName.get(code);

                if (i == 0) {
                    trackedEntityInstanceItemRow.setFirstItem(name);
                } else if (i == 1) {
                    trackedEntityInstanceItemRow.setSecondItem(name);
                } else if (i == 2) {
                    trackedEntityInstanceItemRow.setThirdItem(name);
                }
            }
        }


        return trackedEntityInstanceItemRow;
    }

    private DataValue getDataValue(TrackedEntityInstance event, String dataElement) {
//        List<DataValue> dataValues = Select.all(
//                DataValue.class,
//                Condition.column(DataValue$Table.EVENT).is(event.event),
//                Condition.column(DataValue$Table.DATAELEMENT).is(dataElement)
//        );

//        if (dataValues != null && !dataValues.isEmpty()) {
//            return dataValues.get(0);
//        } else {
//            return null;
//        }
        return null;
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
