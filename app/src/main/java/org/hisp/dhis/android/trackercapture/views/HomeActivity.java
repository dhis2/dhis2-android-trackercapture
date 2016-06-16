package org.hisp.dhis.android.trackercapture.views;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.client.sdk.ui.bindings.views.DefaultHomeActivity;
import org.hisp.dhis.client.sdk.ui.fragments.WrapperFragment;

public class HomeActivity extends DefaultHomeActivity {

    @IdRes
    private static final int DRAWER_ITEM_HOME_ID = 565765;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addMenuItem(DRAWER_ITEM_HOME_ID, R.drawable.ic_add,
                R.string.drawer_item_home);
        if (savedInstanceState == null) {
            onNavigationItemSelected(getNavigationView().getMenu()
                    .findItem(DRAWER_ITEM_HOME_ID));
        }
    }

    @Override
    protected boolean onItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case DRAWER_ITEM_HOME_ID: {
                attachFragment(WrapperFragment.newInstance(
                        SelectorFragment.class, getString(R.string.drawer_item_home)));
                break;
            }
        }
        return true;
    }
}
