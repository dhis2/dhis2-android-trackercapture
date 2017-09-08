package org.hisp.dhis.android.trackercapture.fragments.search;

import android.content.Context;
import android.content.res.Configuration;

import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.sql.queriable.StringQuery;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem$Table;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue$Table;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance$Table;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceDynamicColumnRows;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.sdk.utils.ScreenSizeConfigurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

        List<String> attributesToShow = new ArrayList<>();
        Map<String, TrackedEntityAttribute> attributesToShowMap = new HashMap<>();
        TrackedEntityInstanceDynamicColumnRows attributeNames = new TrackedEntityInstanceDynamicColumnRows();
        TrackedEntityInstanceDynamicColumnRows
                row = new TrackedEntityInstanceDynamicColumnRows();
        int numberOfColumns = ScreenSizeConfigurator.getInstance().getFields();
         for (ProgramTrackedEntityAttribute attribute : attributes) {
            if (attribute.getDisplayInList() && attributesToShow.size() < numberOfColumns) {
                attributesToShow.add(attribute.getTrackedEntityAttributeId());
                attributesToShowMap.put(attribute.getTrackedEntityAttributeId(), attribute.getTrackedEntityAttribute());
                if (attribute.getTrackedEntityAttribute() != null) {
                    String name = attribute.getTrackedEntityAttribute().getName();
                    row.addColumn(name);
                    attributeNames.addColumn(attribute.getTrackedEntityAttribute().getShortName());
                }
            }
        }

        eventRows.add(row);

        HashMap<String, String> attributesWithValuesMap = new HashMap<>();

        //map of Tracked Entity Attributes used in this query
        Map<String, TrackedEntityAttribute> trackedEntityAttributesUsedInQueryMap = new HashMap();

        for(String key : attributeValueMap.keySet()) {
            String val = attributeValueMap.get(key);
            if(val != null && !val.equals("")) {
                attributesWithValuesMap.put(key, val);
            }
            trackedEntityAttributesUsedInQueryMap.put(key, MetaDataController.getTrackedEntityAttribute(key));
        }

        String query = getTrackedEntityInstancesQuery(attributesWithValuesMap, trackedEntityAttributesUsedInQueryMap);
        if(query == null) {
            return form;
        }

        List<TrackedEntityInstance> resultTrackedEntityInstances = new StringQuery<>(TrackedEntityInstance.class, query).queryList();

        //caching tracked entity attributes
        List<TrackedEntityAttribute> trackedEntityAttributes = new Select().from(TrackedEntityAttribute.class).queryList();
        Map<String, TrackedEntityAttribute> allTrackedEntityAttributesMap = new HashMap<>();
        for(TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributes) {
            allTrackedEntityAttributesMap.put(trackedEntityAttribute.getUid(), trackedEntityAttribute);
        }

        //putting teis in map indexed by localid
        Map<Long, TrackedEntityInstance> trackedEntityInstanceLocalIdToTeiMap = new HashMap<>();
        for(TrackedEntityInstance trackedEntityInstance : resultTrackedEntityInstances) {
            trackedEntityInstanceLocalIdToTeiMap.put(trackedEntityInstance.getLocalId(), trackedEntityInstance);
        }

        //searching for Failed Items for any of the resulting TEI
        Set<String> failedItemsForTrackedEntityInstances = getFailedItemsForTrackedEntityInstances(trackedEntityInstanceLocalIdToTeiMap);

        //Caching Option Sets for further use to avoid repeated db calls
        Map<String, Map<String, Option>> optionsForOptionSetsDisplayedInListMap = getCachedOptionsForOptionSets(attributesToShowMap);

        //caching TrackedEntityAttributeValues to avoid looped db queries
        Map<Long, Map<String, TrackedEntityAttributeValue>> cachedTrackedEntityAttributeValuesForTrackedEntityInstances = getCachedTrackedEntityAttributeValuesForTrackedEntityInstances(attributesToShow, resultTrackedEntityInstances);

        //creating rows to show in list
        for (TrackedEntityInstance trackedEntityInstance : resultTrackedEntityInstances) {
            if (trackedEntityInstance == null) {
                continue;
            }
            eventRows.add(createTrackedEntityInstanceItem(context,
                    trackedEntityInstance, attributesToShow,
                    allTrackedEntityAttributesMap, failedItemsForTrackedEntityInstances,
                    cachedTrackedEntityAttributeValuesForTrackedEntityInstances,
                    optionsForOptionSetsDisplayedInListMap));
        }

        form.setEventRowList(eventRows);

        form.setColumnNames(attributeNames);

        if(selectedProgram.getTrackedEntity() != null) {
            row.setTrackedEntity(selectedProgram.getTrackedEntity().getName());
            row.setTitle(selectedProgram.getTrackedEntity().getName() + " (" + ( eventRows.size() - 1 ) + ")") ;
        }

        return form;

    }

    private EventRow createTrackedEntityInstanceItem(Context context, TrackedEntityInstance trackedEntityInstance,
                                                     List<String> attributesToShow,
                                                     Map<String, TrackedEntityAttribute> trackedEntityAttributeMap,
                                                     Set<String> failedEventIds, Map<Long, Map<String, TrackedEntityAttributeValue>> cachedTrackedEntityAttributeValuesForTrackedEntityInstances, Map<String, Map<String, Option>> optionsForOptionSetMap) {
        TrackedEntityInstanceItemRow trackedEntityInstanceItemRow = new TrackedEntityInstanceItemRow(context);
        trackedEntityInstanceItemRow.setTrackedEntityInstance(trackedEntityInstance);

        if (trackedEntityInstance.isFromServer()) {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.SENT);
        } else if (failedEventIds.contains(trackedEntityInstance.getTrackedEntityInstance())) {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.ERROR);
        } else {
            trackedEntityInstanceItemRow.setStatus(OnRowClick.ITEM_STATUS.OFFLINE);
        }

        Map<String, TrackedEntityAttributeValue> trackedEntityAttributeValueMapForTrackedEntityInstance = cachedTrackedEntityAttributeValuesForTrackedEntityInstances.get(trackedEntityInstance.getLocalId());
        for (int i = 0; i < attributesToShow.size(); i++) {
            String value = " ";

            String attributeUid = attributesToShow.get(i);
            if (attributeUid != null) {
                TrackedEntityAttributeValue teav = null;

                if(trackedEntityAttributeValueMapForTrackedEntityInstance != null) {
                    teav = trackedEntityAttributeValueMapForTrackedEntityInstance.get(attributeUid);
                }

                TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeMap.get(attributeUid);
                if (teav == null || trackedEntityAttribute == null) {
                    trackedEntityInstanceItemRow.addColumn(value);
                    continue;
                }

                value = teav.getValue();

                if (trackedEntityAttribute.isOptionSetValue()) {
                    if (trackedEntityAttribute.getOptionSet() == null) {
                        continue;
                    }

                    String optionSetId = trackedEntityAttribute.getOptionSet();
                    Map<String, Option> optionsMap = optionsForOptionSetMap.get(optionSetId);
                    if(optionsMap == null) {
                        trackedEntityInstanceItemRow.addColumn(value);
                        continue;
                    }
                    Option optionWithMatchingValue = optionsMap.get(value);
                    if(optionWithMatchingValue != null) {
                        value = optionWithMatchingValue.getName();
                    }

                }
            }
            trackedEntityInstanceItemRow.addColumn(value);
        }
        return trackedEntityInstanceItemRow;
    }

    /**
     * Returns a map of Tracked Entity Attribute Values for the given List of Tracked Entity Instances
     * Indexed by local id of TEI
     * @param attributesToShow
     * @param resultTrackedEntityInstances
     * @return
     */
    private Map<Long, Map<String, TrackedEntityAttributeValue>> getCachedTrackedEntityAttributeValuesForTrackedEntityInstances(List<String> attributesToShow, List<TrackedEntityInstance> resultTrackedEntityInstances) {
        List<Long> trackedEntityInstanceIds = new ArrayList<>();
        for(TrackedEntityInstance trackedEntityInstance : resultTrackedEntityInstances) {
            trackedEntityInstanceIds.add(trackedEntityInstance.getLocalId());
        }

        //making tei localids string to add to query separated by comma
        String trackedEntityInstanceIdsString = "";
        for(int i = 0; i<trackedEntityInstanceIds.size(); i++) {
            trackedEntityInstanceIdsString += "" + trackedEntityInstanceIds.get(i);
            if(i<trackedEntityInstanceIds.size() -1 ) {
                trackedEntityInstanceIdsString += ',';
            }
        }

        //making attributes to show string to add to query separated by comma
        String attributesToShowIdString = "";
        for(int i = 0; i<attributesToShow.size(); i++) {
            attributesToShowIdString += "'" + attributesToShow.get(i)+"'";
            if(i<attributesToShow.size() -1 ) {
                attributesToShowIdString += ',';
            }
        }

        String attributeValuesQuery = "SELECT * FROM " + TrackedEntityAttributeValue.class.getSimpleName() +
                " WHERE " + TrackedEntityAttributeValue$Table.LOCALTRACKEDENTITYINSTANCEID + " IN ( " + trackedEntityInstanceIdsString
                + ") AND " + TrackedEntityAttributeValue$Table.TRACKEDENTITYATTRIBUTEID + " IN (" + attributesToShowIdString + ");";

        List<TrackedEntityAttributeValue> cachedAttributeValuesToShow = new StringQuery<>(TrackedEntityAttributeValue.class, attributeValuesQuery).queryList();

        //making a map for each tracked entity instances containing tracked entity attributes
        //each map is added to the main map indexed by localid of tei
        Map<Long, Map<String, TrackedEntityAttributeValue>> cachedTrackedEntityAttributeValuesForTrackedEntityInstances = new HashMap<>();
        for(TrackedEntityAttributeValue trackedEntityAttributeValue : cachedAttributeValuesToShow) {
            Map<String, TrackedEntityAttributeValue> trackedEntityAttributeValueMapForTrackedEntityInstance = cachedTrackedEntityAttributeValuesForTrackedEntityInstances.get(trackedEntityAttributeValue.getLocalTrackedEntityInstanceId());
            if(trackedEntityAttributeValueMapForTrackedEntityInstance == null) {
                trackedEntityAttributeValueMapForTrackedEntityInstance = new HashMap<>();
                cachedTrackedEntityAttributeValuesForTrackedEntityInstances.put(trackedEntityAttributeValue.getLocalTrackedEntityInstanceId(), trackedEntityAttributeValueMapForTrackedEntityInstance);
            }
            trackedEntityAttributeValueMapForTrackedEntityInstance.put(trackedEntityAttributeValue.getTrackedEntityAttributeId(), trackedEntityAttributeValue);
        }
        return cachedTrackedEntityAttributeValuesForTrackedEntityInstances;
    }

    /**
     * Returns a map of map of options for each option set used in tracked entity attributes
     * @param trackedEntityAttributeMap
     * @return
     */
    private Map<String, Map<String, Option>> getCachedOptionsForOptionSets(Map<String, TrackedEntityAttribute> trackedEntityAttributeMap) {
        Map<String, Map<String, Option>> optionsForOptionSetMap = new HashMap<>();
        for(TrackedEntityAttribute trackedEntityAttribute : trackedEntityAttributeMap.values()) {
            if(trackedEntityAttribute.isOptionSetValue()) {
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
                HashMap<String, Option> optionsHashMap = new HashMap<>();
                optionsForOptionSetMap.put(optionSet.getUid(), optionsHashMap);
                for (Option option : options) {
                    optionsHashMap.put(option.getCode(), option);
                }
            }
        }
        return optionsForOptionSetMap;
    }

    /**
     * Returns a SQL query to fetch Tracked Entity Instances based on attribute values
     * @param attributesWithValuesMap
     * @param trackedEntityAttributeMap
     * @return
     */
    private String getTrackedEntityInstancesQuery(HashMap<String, String> attributesWithValuesMap,
                                                  Map<String, TrackedEntityAttribute> trackedEntityAttributeMap) {
        Set<String> attributesIdsUsedInQuery = attributesWithValuesMap.keySet();
        Iterator<String> attributesIdsUsedInQueryIterator = attributesIdsUsedInQuery.iterator();
        String firstId;
        if(attributesIdsUsedInQueryIterator.hasNext()) {
            firstId = attributesIdsUsedInQueryIterator.next();
        } else {
            //no values have been used in the query show with no filter
            return "SELECT * FROM " + TrackedEntityInstance.class.getSimpleName() + " WHERE "
                    + TrackedEntityInstance$Table.TRACKEDENTITYINSTANCE + " IN (SELECT " +
                    TrackedEntityAttributeValue$Table.TRACKEDENTITYINSTANCEID + " FROM " +
                    TrackedEntityAttributeValue.class.getSimpleName() + ")";
        }
        String firstValue;
        TrackedEntityAttribute firstTrackedEntityAttribute = trackedEntityAttributeMap.get(firstId);
        String firstCompareOperator;
        if(firstTrackedEntityAttribute.getOptionSet() != null) {
            firstCompareOperator = "IS";
            firstValue = attributesWithValuesMap.get(firstId);
        } else {
            firstCompareOperator = "LIKE";
            firstValue = '%' + attributesWithValuesMap.get(firstId) + '%';
        }

        String query = "SELECT * FROM " + TrackedEntityInstance.class.getSimpleName() + " WHERE "
                + TrackedEntityInstance$Table.TRACKEDENTITYINSTANCE + " IN (SELECT " +
                TrackedEntityAttributeValue$Table.TRACKEDENTITYINSTANCEID + " FROM " +
                TrackedEntityAttributeValue.class.getSimpleName() + " WHERE " + TrackedEntityAttributeValue$Table.TRACKEDENTITYATTRIBUTEID +
                " IS '" + firstId + "' AND " + TrackedEntityAttributeValue$Table.VALUE + ' ' + firstCompareOperator +' ' + "'" + firstValue + "'";

        int closingParenthesis = 1;

        while (attributesIdsUsedInQueryIterator.hasNext()) {
            String attributeId = attributesIdsUsedInQueryIterator.next();
            String attributeValue;
            TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeMap.get(attributeId);
            String compareOperator;
            if(trackedEntityAttribute.getOptionSet() != null) {
                compareOperator = "IS";
                attributeValue = attributesWithValuesMap.get(attributeId);
            } else {
                compareOperator = "LIKE";
                attributeValue = '%' + attributesWithValuesMap.get(attributeId) + '%';
            }

            String queryToAppend = " AND " + TrackedEntityAttributeValue$Table.TRACKEDENTITYINSTANCEID +
                    " IN ( SELECT " + TrackedEntityAttributeValue$Table.TRACKEDENTITYINSTANCEID +
                    " FROM " + TrackedEntityAttributeValue.class.getSimpleName() + " WHERE " + TrackedEntityAttributeValue$Table.TRACKEDENTITYATTRIBUTEID +
                    " IS '" + attributeId + "' AND " + TrackedEntityAttributeValue$Table.VALUE + ' ' + compareOperator +' ' + "'" + attributeValue + "'";
            query += queryToAppend;
            closingParenthesis++;
        }

        for(int i = 0; i<closingParenthesis; i++) {
            query += ')';
        }
        query += ';';
        return query;
    }

    private Set<String> getFailedItemsForTrackedEntityInstances(Map<Long, TrackedEntityInstance> trackedEntityInstanceLocalIdToTeiMap) {
        //making tei localids string to add to query separated by comma
        Set<Long> trackedEntityLocalIdSet = trackedEntityInstanceLocalIdToTeiMap.keySet();
        Iterator<Long> idIterator = trackedEntityLocalIdSet.iterator();
        String trackedEntityInstanceLocalIdsString = "";
        for(int i = 0; i<trackedEntityLocalIdSet.size(); i++) {
            if(idIterator.hasNext()) {
                trackedEntityInstanceLocalIdsString += "" + idIterator.next();
                if (i < trackedEntityLocalIdSet.size() - 1) {
                    trackedEntityInstanceLocalIdsString += ',';
                }
            }
        }

        String failedItemsQuery = "SELECT * FROM " + FailedItem.class.getSimpleName() + " WHERE " + FailedItem$Table.ITEMTYPE + " IS '" + FailedItem.TRACKEDENTITYINSTANCE
                + "' AND " + FailedItem$Table.ITEMID + " IN (" + trackedEntityInstanceLocalIdsString + ");";
        List<FailedItem> newFailedItems = new StringQuery<>(FailedItem.class, failedItemsQuery).queryList();

        Set<String> failedItemsForTrackedEntityInstances = new HashSet<>();
        for (FailedItem failedItem : newFailedItems) {
            TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceLocalIdToTeiMap.get(failedItem.getItemId());
            if(trackedEntityInstance == null) {
                failedItem.delete();
            } else {
                if(failedItem.getHttpStatusCode()>=0) {
                    if(trackedEntityInstance.getTrackedEntityInstance() != null) {
                        failedItemsForTrackedEntityInstances.add(trackedEntityInstance.getTrackedEntityInstance());
                    }
                }
            }
        }
        return failedItemsForTrackedEntityInstances;
    }
}