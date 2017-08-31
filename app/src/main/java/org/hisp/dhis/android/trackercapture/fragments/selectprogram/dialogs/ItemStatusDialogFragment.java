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

package org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs;

import android.os.Bundle;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.enrollment.EnrollmentRepository;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.event.EventRepository;
import org.hisp.dhis.android.sdk.synchronization.data.faileditem.FailedItemRepository;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceLocalDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceRemoteDataSource;
import org.hisp.dhis.android.sdk.synchronization.data.trackedentityinstance
        .TrackedEntityInstanceRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.enrollment.IEnrollmentRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.enrollment.SyncEnrollmentUseCase;
import org.hisp.dhis.android.sdk.synchronization.domain.event.IEventRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.event.SyncEventUseCase;
import org.hisp.dhis.android.sdk.synchronization.domain.trackedentityinstance
        .ITrackedEntityInstanceRepository;
import org.hisp.dhis.android.sdk.synchronization.domain.trackedentityinstance
        .SyncTrackedEntityInstanceUseCase;

/**
 * Created by erling on 9/21/15.
 */
public class ItemStatusDialogFragment extends org.hisp.dhis.android.sdk.ui.dialogs.ItemStatusDialogFragment
{

    public static ItemStatusDialogFragment newInstance(BaseSerializableModel item) {
        ItemStatusDialogFragment dialogFragment = new ItemStatusDialogFragment();
        Bundle args = new Bundle();

        args.putLong(EXTRA_ID, item.getLocalId());
        if(item instanceof TrackedEntityInstance) {
            args.putString(EXTRA_TYPE, FailedItem.TRACKEDENTITYINSTANCE);
        } else if (item instanceof Enrollment) {
            args.putString(EXTRA_TYPE, FailedItem.ENROLLMENT);
        }
        else if(item instanceof Event)
        {
            args.putString(EXTRA_TYPE, FailedItem.EVENT);
        }

        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void sendToServer(BaseSerializableModel item, org.hisp.dhis.android.sdk.ui.dialogs.ItemStatusDialogFragment fragment) {
        if(item instanceof TrackedEntityInstance) {
            TrackedEntityInstance trackedEntityInstance = (TrackedEntityInstance) item;
            sendTrackedEntityInstance(trackedEntityInstance);
        } else if(item instanceof Enrollment) {
            Enrollment enrollment = (Enrollment) item;
            sendEnrollment(enrollment);
        }
        else if(item instanceof Event)
        {
            Event event = (Event) item;
            sendEvent(event);
        }
    }
    public static void sendTrackedEntityInstance(final TrackedEntityInstance trackedEntityInstance) {
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.TRACKEDENTITYINSTANCE) {
            @Override
            public Object execute() {

                EnrollmentLocalDataSource enrollmentLocalDataSource = new EnrollmentLocalDataSource();
                EnrollmentRemoteDataSource enrollmentRemoteDataSource = new EnrollmentRemoteDataSource(DhisController.getInstance().getDhisApi());
                IEnrollmentRepository enrollmentRepository = new EnrollmentRepository(enrollmentLocalDataSource, enrollmentRemoteDataSource);

                EventLocalDataSource mLocalDataSource = new EventLocalDataSource();
                EventRemoteDataSource mRemoteDataSource = new EventRemoteDataSource(DhisController.getInstance().getDhisApi());
                EventRepository eventRepository = new EventRepository(mLocalDataSource, mRemoteDataSource);
                FailedItemRepository failedItemRepository = new FailedItemRepository();

                TrackedEntityInstanceLocalDataSource trackedEntityInstanceLocalDataSource = new TrackedEntityInstanceLocalDataSource();
                TrackedEntityInstanceRemoteDataSource trackedEntityInstanceRemoteDataSource = new TrackedEntityInstanceRemoteDataSource(DhisController.getInstance().getDhisApi());
                ITrackedEntityInstanceRepository trackedEntityInstanceRepository = new TrackedEntityInstanceRepository(trackedEntityInstanceLocalDataSource, trackedEntityInstanceRemoteDataSource);
                SyncTrackedEntityInstanceUseCase syncTrackedEntityInstanceUseCase = new SyncTrackedEntityInstanceUseCase(trackedEntityInstanceRepository, enrollmentRepository, eventRepository, failedItemRepository);
                syncTrackedEntityInstanceUseCase.execute(trackedEntityInstance);
                return new Object();
            }
        });
    }
    public static void sendEnrollment(final Enrollment enrollment) {
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.ENROLLMENT) {

            @Override
            public Object execute()  {
                EventLocalDataSource mLocalDataSource = new EventLocalDataSource();
                EventRemoteDataSource mRemoteDataSource = new EventRemoteDataSource(DhisController.getInstance().getDhisApi());
                EnrollmentLocalDataSource enrollmentLocalDataSource = new EnrollmentLocalDataSource();

                EnrollmentRemoteDataSource enrollmentRemoteDataSource = new EnrollmentRemoteDataSource(DhisController.getInstance().getDhisApi());
                IEnrollmentRepository enrollmentRepository = new EnrollmentRepository(enrollmentLocalDataSource, enrollmentRemoteDataSource);
                IEventRepository eventRepository = new EventRepository(mLocalDataSource, mRemoteDataSource);

                TrackedEntityInstanceRemoteDataSource trackedEntityInstanceRemoteDataSource = new TrackedEntityInstanceRemoteDataSource(DhisController.getInstance().getDhisApi());
                TrackedEntityInstanceLocalDataSource trackedEntityInstanceLocalDataSource = new TrackedEntityInstanceLocalDataSource();
                TrackedEntityInstanceRepository trackedEntityInstanceRepository = new TrackedEntityInstanceRepository(trackedEntityInstanceLocalDataSource, trackedEntityInstanceRemoteDataSource);

                FailedItemRepository  failedItemRepository = new FailedItemRepository ();
                SyncEnrollmentUseCase enrollmentUseCase = new SyncEnrollmentUseCase(enrollmentRepository, eventRepository, trackedEntityInstanceRepository, failedItemRepository);
                enrollmentUseCase.execute(enrollment);
                return new Object();
            }
        });
    }
}
