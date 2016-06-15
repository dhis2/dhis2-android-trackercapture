package org.hisp.dhis.android.app.views;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.hisp.dhis.android.app.R;
import org.hisp.dhis.client.sdk.ui.bindings.views.DefaultHomeActivity;
import org.hisp.dhis.client.sdk.ui.fragments.WrapperFragment;

public class HomeActivity extends DefaultHomeActivity {

    @IdRes
    private static final int DRAWER_ITEM_PLACEHOLDER_ID = 324342;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addMenuItem(DRAWER_ITEM_PLACEHOLDER_ID, R.drawable.ic_add,
                R.string.drawer_item_placeholder);
        if (savedInstanceState == null) {
            onNavigationItemSelected(getNavigationView().getMenu()
                    .findItem(DRAWER_ITEM_PLACEHOLDER_ID));
        }
    }

    @Override
    protected boolean onItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case DRAWER_ITEM_PLACEHOLDER_ID: {
                attachFragment(WrapperFragment.newInstance(
                        PlaceholderFragment.class, getString(R.string.drawer_item_placeholder)));
                break;
            }
        }
        return true;
    }
}
