package org.hisp.dhis.android.trackercapture.fragments.selectprogram.dialogs;

import android.os.Bundle;

import org.hisp.dhis.android.sdk.controllers.DhisController;
import org.hisp.dhis.android.sdk.controllers.tracker.TrackerController;
import org.hisp.dhis.android.sdk.job.JobExecutor;
import org.hisp.dhis.android.sdk.job.NetworkJob;
import org.hisp.dhis.android.sdk.network.APIException;
import org.hisp.dhis.android.sdk.persistence.models.BaseSerializableModel;
import org.hisp.dhis.android.sdk.persistence.models.Enrollment;
import org.hisp.dhis.android.sdk.persistence.models.Event;
import org.hisp.dhis.android.sdk.persistence.models.FailedItem;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityInstance;
import org.hisp.dhis.android.sdk.persistence.preferences.ResourceType;

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
                TrackerController.sendTrackedEntityInstanceChanges(DhisController.getInstance().getDhisApi(), trackedEntityInstance, true);
                return new Object();
            }
        });
    }
    public static void sendEnrollment(final Enrollment enrollment) {
        JobExecutor.enqueueJob(new NetworkJob<Object>(0,
                ResourceType.ENROLLMENT) {

            @Override
            public Object execute()  {
                TrackerController.sendEnrollmentChanges(DhisController.getInstance().getDhisApi(), enrollment, true);
                return new Object();
            }
        });
    }
}
