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
import com.raizlabs.android.dbflow.sql.queriable.StringQuery;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment$Table;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem$Table;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.OptionSet;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStage;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue$Table;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance$Table;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.EventRow;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceDynamicColumnRows;
import org.hisp.dhis.android.sdk.ui.adapters.rows.events.TrackedEntityInstanceItemRow;
import org.hisp.dhis.android.sdk.ui.fragments.selectprogram.SelectProgramFragmentForm;
import org.hisp.dhis.android.sdk.utils.ScreenSizeConfigurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectProgramFragmentQuery implements Query<SelectProgramFragmentForm> {
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

        if (selectedProgram == null || isListEmpty(selectedProgram.getProgramStages())) {
            return fragmentForm;
        }

        fragmentForm.setProgram(selectedProgram);

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
        Map<String, TrackedEntityAttribute> attributesToShowMap = new HashMap<>();
        TrackedEntityInstanceDynamicColumnRows columnNames = new TrackedEntityInstanceDynamicColumnRows();
        TrackedEntityInstanceDynamicColumnRows attributeNames = new TrackedEntityInstanceDynamicColumnRows();
        for (ProgramTrackedEntityAttribute attribute : attributes) {
            if (attribute.getDisplayInList() && attributesToShow.size() < ScreenSizeConfigurator.getInstance().getFields()) {
                attributesToShow.add(attribute.getTrackedEntityAttributeId());
                if (attribute.getTrackedEntityAttribute() != null) {
                    String name = attribute.getTrackedEntityAttribute().getName();
                    if (attributesToShow.size() <= ScreenSizeConfigurator.getInstance().getFields()) {
                        columnNames.addColumn(name);
                        attributeNames.addColumn(attribute.getTrackedEntityAttribute().getShortName());
                    }
                    attributesToShowMap.put(attribute.getTrackedEntityAttributeId(), attribute.getTrackedEntityAttribute());
                }
            }
        }
        teiRows.add(columnNames);

        if(!selectedProgram.isDisplayFrontPageList()) {
            return fragmentForm; // we don't want to show any values or any list header
        }

        String query = getTrackedEntityInstancesWithEnrollmentQuery(mOrgUnitId, mProgramId);
        if(query == null) {
            return fragmentForm;
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
        Map<String, Map<String, Option>> optionsForOptionSetMap = getCachedOptionsForOptionSets(attributesToShowMap);

        //caching TrackedEntityAttributeValues to avoid looped db queries
        Map<Long, Map<String, TrackedEntityAttributeValue>> cachedTrackedEntityAttributeValuesForTrackedEntityInstances = getCachedTrackedEntityAttributeValuesForTrackedEntityInstances(attributesToShow, resultTrackedEntityInstances);

        for (TrackedEntityInstance trackedEntityInstance : resultTrackedEntityInstances) {
            if (trackedEntityInstance == null) {
                continue;
            }
            teiRows.add(createTrackedEntityInstanceItem(context,
                    trackedEntityInstance, attributesToShow, attributesToShowMap,
                    failedItemsForTrackedEntityInstances,
                    cachedTrackedEntityAttributeValuesForTrackedEntityInstances,
                    optionsForOptionSetMap));
        }

        fragmentForm.setEventRowList(teiRows);
        fragmentForm.setColumnNames(columnNames);

        fragmentForm.setColumnNames(attributeNames);
        if(selectedProgram.getTrackedEntity() != null) {
            columnNames.setTrackedEntity(selectedProgram.getTrackedEntity().getName());
            columnNames.setTitle(selectedProgram.getTrackedEntity().getName() + " (" + ( teiRows.size() - 1 ) + ")") ;
        }

        return fragmentForm;
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
            if (i > attributesToShow.size()) {
                break;
            }


            String attributeUid = attributesToShow.get(i);
            if (attributeUid != null) {
                TrackedEntityAttributeValue teav = null;

                if(trackedEntityAttributeValueMapForTrackedEntityInstance != null) {
                    teav = trackedEntityAttributeValueMapForTrackedEntityInstance.get(attributeUid);
                }

                String value;
                TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeMap.get(attributeUid);
                if (teav == null || trackedEntityAttribute == null) {
                    trackedEntityInstanceItemRow.addColumn("");
                    continue;
                }

                value = teav.getValue();

                if (trackedEntityAttribute.isOptionSetValue()) {
                    if (trackedEntityAttribute.getOptionSet() == null) {
                        trackedEntityInstanceItemRow.addColumn("");
                        continue;
                    }

                    String optionSetId = trackedEntityAttribute.getOptionSet();
                    Map<String, Option> optionsMap = optionsForOptionSetMap.get(optionSetId);
                    if(optionsMap == null) {
                        trackedEntityInstanceItemRow.addColumn("");
                        continue;
                    }
                    Option optionWithMatchingValue = optionsMap.get(value);
                    if(optionWithMatchingValue != null) {
                        value = optionWithMatchingValue.getName();
                    }

                }
                trackedEntityInstanceItemRow.addColumn(value);
            }
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
     * Returns a SQL query for getting TEI with enrollment for Organisation Unit and Program
     * @param organisationUnitId
     * @param programId
     * @return
     */
    private String getTrackedEntityInstancesWithEnrollmentQuery(String organisationUnitId, String programId) {
        String query = "SELECT * FROM " + TrackedEntityInstance.class.getSimpleName() + " t1 " +
                "JOIN "
                +"( " +
                    "SELECT DISTINCT " + Enrollment$Table.LOCALTRACKEDENTITYINSTANCEID + ", " + Enrollment$Table.LASTUPDATED + " FROM " +
                Enrollment.class.getSimpleName() + " WHERE " + Enrollment$Table.PROGRAM +
                " IS '" + programId + "' AND " + Enrollment$Table.ORGUNIT + " IS '" + organisationUnitId + "'" +
                ") t2 " +
                "ON t1." + TrackedEntityInstance$Table.LOCALID + "=t2." + Enrollment$Table.LOCALTRACKEDENTITYINSTANCEID +
                " GROUP BY t1."+TrackedEntityInstance$Table.LOCALID+" "+
                " ORDER BY t2." + Enrollment$Table.LASTUPDATED + " ASC";

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

    private static <T> boolean isListEmpty(List<T> items) {
        return items == null || items.isEmpty();
    }
}
