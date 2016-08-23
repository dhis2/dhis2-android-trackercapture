package org.hisp.dhis.android.app.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.hisp.dhis.android.app.TrackerCaptureApp;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultNotificationHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultSyncAdapter;

import javax.inject.Inject;

public class SyncAdapter extends AbstractThreadedSyncAdapter implements DefaultSyncAdapter {
    private static final String TAG = SyncAdapter.class.getSimpleName();

    @Inject
    DefaultNotificationHandler notificationHandler;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        //inject the notificationHandler:
        ((TrackerCaptureApp) context.getApplicationContext()).getUserComponent().inject(this);
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        //inject the notificationHandler:
        ((TrackerCaptureApp) context.getApplicationContext()).getUserComponent().inject(this);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        //TODO: Put your sync logic here
        Log.i(TAG, "Background sync failed. Synchronization logic not implemented.");
        notificationHandler.showSyncCompletedNotification(false);
    }
}