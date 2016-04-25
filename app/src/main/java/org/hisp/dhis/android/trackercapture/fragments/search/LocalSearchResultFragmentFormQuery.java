package org.hisp.dhis.android.trackercapture.fragments.search;

import android.content.Context;

import com.raizlabs.android.dbflow.sql.language.Select;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceColumnNamesRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalSearchResultFragmentFormQuery implements Query<LocalSearchResultFragmentForm> {
    String orgUnitId;
    String programId;
    HashMap<String, String> attributeValueMap;

    public LocalSearchResultFragmentFormQuery(String orgUnitId, String programId, HashMap<String, String> attributeValueMap) {
        this.orgUnitId = orgUnitId;
        this.programId = programId;
        this.attributeValueMap = attributeValueMap;
    }


    @Override
    public LocalSearchResultFragmentForm query(Context context) {
        LocalSearchResultFragmentForm form = new LocalSearchResultFragmentForm();

        if (orgUnitId.equals("") || programId.equals("")) {
            return form;
        }
        Program selectedProgram = MetaDataController.getProgram(programId);
        List<EventRow> eventRows = new ArrayList<>();
        List<ProgramTrackedEntityAttribute> attributes = selectedProgram.getProgramTrackedEntityAttributes();
        Collection<String> trackedEntityAttributeIds = attributeValueMap.keySet();

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

        eventRows.add(columnNames);
        List<Enrollment> enrollmentsToShow = new ArrayList<>();
        List<Enrollment> enrollments = TrackerController.getEnrollments(
                programId, orgUnitId);
        List<Long> trackedEntityInstanceIds = new ArrayList<>();
        if (isListEmpty(enrollments)) {
            return form;
        } else {
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getLocalTrackedEntityInstanceId() > 0) {
                    if (!trackedEntityInstanceIds.contains(enrollment.getLocalTrackedEntityInstanceId())) {
                        trackedEntityInstanceIds.add(enrollment.getLocalTrackedEntityInstanceId());
                    }

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

        int hitsRequired = 0;
        Collection<String> searchValues = attributeValueMap.values();
        for(String val : searchValues) {
            if(val != null && !val.equals("")) {
                hitsRequired ++;
            }
        }

        List<TrackedEntityInstance> trackedEntityInstancesToShow = new ArrayList<>();
        for(TrackedEntityInstance tei : trackedEntityInstanceList) {
            List<TrackedEntityAttributeValue> teiAttributeValues = tei.getAttributes();
            int numberOfHits = 0;

            for(TrackedEntityAttributeValue trackedEntityAttributeValue : teiAttributeValues) {

                if(attributeValueMap.get(trackedEntityAttributeValue.getTrackedEntityAttributeId()) != null &&
                        attributeValueMap.get(trackedEntityAttributeValue.getTrackedEntityAttributeId()).toLowerCase()
                        .contains(trackedEntityAttributeValue.getValue().toLowerCase())) {
                    numberOfHits++;

                    if(!trackedEntityInstancesToShow.contains(tei) && numberOfHits == hitsRequired) {
                        trackedEntityInstancesToShow.add(tei);
                    }
                }
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

        for (TrackedEntityInstance trackedEntityInstance : trackedEntityInstancesToShow) {
            if (trackedEntityInstance == null) continue;
            eventRows.add(createTrackedEntityInstanceItem(context,
                    trackedEntityInstance, attributesToShow, attributes,
                    trackedEntityAttributeMap, failedEventIds));
        }

        form.setEventRowList(eventRows);
        form.setColumnNames(columnNames);

        if(selectedProgram.getTrackedEntity() != null) {
            columnNames.setTrackedEntity(selectedProgram.getTrackedEntity().getName());
            columnNames.setTitle(selectedProgram.getTrackedEntity().getName() + " (" + ( eventRows.size() - 1 ) + ")") ;
        }

        return form;

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
                if (trackedEntityAttribute.isOptionSetValue()) {
                    if (trackedEntityAttribute.getOptionSet() == null) {
                        continue;
                    }
                    OptionSet optionSet = MetaDataController.getOptionSet(trackedEntityAttribute.getOptionSet());
                    if (optionSet == null) {
                        continue;
                    }
                    List<Option> options = MetaDataController.getOptions(optionSet.getUid());
                    if (options == null) {
                        continue;
                    }
                    for (Option option : options) {
                        if (option.getCode().equals(value)) {
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