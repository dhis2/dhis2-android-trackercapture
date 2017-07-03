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

package org.hisp.dhis.android.trackercapture.fragments.trackedentityinstanceprofile;

import android.content.Context;

import org.hisp.dhis.android.sdk.controllers.GpsController;
import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.persistence.loaders.Query;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Program;
import org.hisp.dhis.android.sdk.persistence.models.ProgramTrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.DataEntryRowFactory;
import org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry.Row;
import org.hisp.dhis.android.sdk.utils.api.ValueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by erling on 5/19/15.
 */
public class TrackedEntityInstanceProfileFragmentQuery implements
        Query<TrackedEntityInstanceProfileFragmentForm> {
    private long mTrackedEntityInstanceId;
    private String mProgramId;
    private TrackedEntityInstance currentTrackedEntityInstance;
    private boolean editable;
    private long enrollmentId;
    private Enrollment currentEnrollment;

    public TrackedEntityInstanceProfileFragmentQuery(long mTrackedEntityInstanceId,
            String mProgramId, long enrollmentId) {
        this.mTrackedEntityInstanceId = mTrackedEntityInstanceId;
        this.mProgramId = mProgramId;
        this.enrollmentId = enrollmentId;
    }

    @Override
    public TrackedEntityInstanceProfileFragmentForm query(Context context) {
        TrackedEntityInstanceProfileFragmentForm mForm =
                new TrackedEntityInstanceProfileFragmentForm();
        final Program mProgram = MetaDataController.getProgram(mProgramId);
        final TrackedEntityInstance mTrackedEntityInstance =
                TrackerController.getTrackedEntityInstance(mTrackedEntityInstanceId);

        if (mProgram == null || mTrackedEntityInstance == null) {
            return mForm;
        }
        currentEnrollment = TrackerController.getEnrollment(enrollmentId);
        currentTrackedEntityInstance = mTrackedEntityInstance;
        mForm.setProgram(mProgram);
        mForm.setTrackedEntityInstance(mTrackedEntityInstance);
        mForm.setTrackedEntityAttributeValueMap(new HashMap<String, TrackedEntityAttributeValue>());

        List<TrackedEntityAttributeValue> trackedEntityAttributeValues =
                TrackerController.getProgramTrackedEntityAttributeValues(mProgram,
                        mTrackedEntityInstance);
        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes =
                MetaDataController.getProgramTrackedEntityAttributes(mProgramId);

        if (trackedEntityAttributeValues == null && programTrackedEntityAttributes == null) {
            return mForm;
        }
        currentEnrollment.setAttributes(trackedEntityAttributeValues);
        mForm.setTrackedEntityAttributeValues(trackedEntityAttributeValues);
        List<Row> dataEntryRows = new ArrayList<>();
        for (int i = 0; i < programTrackedEntityAttributes.size(); i++) {
            boolean shouldNeverBeEdited = false;
            if (programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().isGenerated()) {
                shouldNeverBeEdited = true;
            }
            if (ValueType.COORDINATE.equals(programTrackedEntityAttributes.get(
                    i).getTrackedEntityAttribute().getValueType())) {
                GpsController.activateGps(context);
            }
            Row row = DataEntryRowFactory.createDataEntryView(
                    programTrackedEntityAttributes.get(i).getMandatory(),
                    programTrackedEntityAttributes.get(i).getAllowFutureDate(),
                    programTrackedEntityAttributes.get(
                            i).getTrackedEntityAttribute().getOptionSet(),
                    programTrackedEntityAttributes.get(i).getTrackedEntityAttribute().getName(),
                    getTrackedEntityDataValue(programTrackedEntityAttributes.get(
                            i).getTrackedEntityAttribute().getUid(),
                            trackedEntityAttributeValues), programTrackedEntityAttributes.get(
                            i).getTrackedEntityAttribute().getValueType(), false,
                    shouldNeverBeEdited, mProgram.getDataEntryMethod());
            dataEntryRows.add(row);
        }
        if (trackedEntityAttributeValues != null) {
            for (TrackedEntityAttributeValue trackedEntityAttributeValue :
                    trackedEntityAttributeValues) {
                mForm.getTrackedEntityAttributeValueMap().put(
                        trackedEntityAttributeValue.getTrackedEntityAttributeId(),
                        trackedEntityAttributeValue);
            }
        }
        mForm.setDataEntryRows(dataEntryRows);
        mForm.setEnrollment(currentEnrollment);
        return mForm;
    }

    public TrackedEntityAttributeValue getTrackedEntityDataValue(String trackedEntityAttribute,
            List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
        for (TrackedEntityAttributeValue trackedEntityAttributeValue :
                trackedEntityAttributeValues) {
            if (trackedEntityAttributeValue.getTrackedEntityAttributeId().equals(
                    trackedEntityAttribute)) {
                return trackedEntityAttributeValue;
            }
        }

        //the datavalue didnt exist for some reason. Create a new one.
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setTrackedEntityAttributeId(trackedEntityAttribute);
        trackedEntityAttributeValue.setTrackedEntityInstanceId(
                currentTrackedEntityInstance.getTrackedEntityInstance());
        trackedEntityAttributeValue.setLocalTrackedEntityInstanceId(
                currentTrackedEntityInstance.getLocalId());
        trackedEntityAttributeValue.setValue("");
        trackedEntityAttributeValues.add(trackedEntityAttributeValue);
        return trackedEntityAttributeValue;
    }
}
