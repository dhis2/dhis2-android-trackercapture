package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.controllers.datavalues.DataValueController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceItemStatus;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;

import org.hisp.dhis.android.trackercapture.ui.rows.selectprogram.TrackedEntityInstanceRow;

import java.util.ArrayList;
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
            if (attribute.getDisplayInList() && attributesToShow.size() < 3) {
                attributesToShow.add(attribute.trackedEntityAttribute);
                if (attribute.getTrackedEntityAttribute() != null) {
                    String name = attribute.getTrackedEntityAttribute().getName();
                    if (attributesToShow.size() == 1) {
                        columnNames.setFirstItem(name);
                    } else if (attributesToShow.size() == 2) {
                        columnNames.setSecondItem(name);
                    }
                    else if(attributesToShow.size() == 3){
                        columnNames.setThirdItem(name);
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
                if(enrollment.getLocalTrackedEntityInstanceId() > 0)
                {
                    if(!trackedEntityInstanceIds.contains(enrollment.getLocalTrackedEntityInstanceId()))
                        trackedEntityInstanceIds.add(enrollment.getLocalTrackedEntityInstanceId());
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

        for (TrackedEntityInstance trackedEntityInstance : trackedEntityInstanceList) {
            if(trackedEntityInstance==null) continue;
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

        if (trackedEntityInstance.getFromServer()) {
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

    private static <T> boolean isListEmpty(List<T> items) {
        return items == null || items.isEmpty();
    }

}
