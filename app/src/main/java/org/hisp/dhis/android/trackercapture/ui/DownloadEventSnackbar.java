package org.hisp.dhis.android.trackercapture.ui;

import android.support.design.widget.Snackbar;
import android.view.View;

import org.hisp.dhis.android.sdk.events.OnTeiDownloadedEvent;
import org.hisp.dhis.android.trackercapture.fragments.selectprogram.SelectProgramFragment;

/**
 * Created by thomaslindsjorn on 26/07/16.
 */
public class DownloadEventSnackbar {

    private final SelectProgramFragment selectProgramFragment;
    private boolean errorHasOccured;
    private Snackbar snackbar;

    public DownloadEventSnackbar(SelectProgramFragment selectProgramFragment) {
        this.selectProgramFragment = selectProgramFragment;
    }

    public void show(OnTeiDownloadedEvent downloadEvent) {
        switch (downloadEvent.getEventType()) {
            case START: // new download cycle. reset snackbar
                snackbar.dismiss();
                snackbar = null;
                errorHasOccured = false;
            case ERROR:
                errorHasOccured = true;
            case END:
                if (errorHasOccured) {
                    downloadEvent.setErrorHasOccured(true);
                    showSnackbar(downloadEvent.getUserFriendlyMessage(), downloadEvent.getMessageDuration());
                    snackbar.setAction("Retry", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectProgramFragment.showOnlineSearchFragment();
                        }
                    });
                    return;
                }
            default:
                showSnackbar(downloadEvent.getUserFriendlyMessage(), downloadEvent.getMessageDuration());
        }
    }

    private void showSnackbar(String message, int duration) {
        if (selectProgramFragment.getView() == null) {
            return;
        }

        if (snackbar == null) {
            snackbar = Snackbar.make(selectProgramFragment.getView(), message, duration);
            snackbar.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);
                    DownloadEventSnackbar.this.snackbar = null;
                }
            });
            snackbar.show();
        } else {
            snackbar.setText(message);
            snackbar.setDuration(duration);
            if (!snackbar.isShown()) {
                snackbar.show();
            }
        }
    }
}
