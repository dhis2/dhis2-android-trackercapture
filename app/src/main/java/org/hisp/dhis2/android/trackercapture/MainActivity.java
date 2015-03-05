package org.hisp.dhis2.android.trackercapture;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.squareup.otto.Subscribe;

import org.hisp.dhis2.android.sdk.controllers.Dhis2;
import org.hisp.dhis2.android.sdk.events.BaseEvent;
import org.hisp.dhis2.android.sdk.events.MessageEvent;
import org.hisp.dhis2.android.sdk.fragments.EditItemFragment;
import org.hisp.dhis2.android.sdk.fragments.FailedItemsFragment;
import org.hisp.dhis2.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis2.android.sdk.persistence.models.OrganisationUnit;
import org.hisp.dhis2.android.sdk.persistence.models.Program;
import org.hisp.dhis2.android.trackercapture.fragments.SelectProgramFragment;


public class MainActivity extends ActionBarActivity {

    public final static String CLASS_TAG = "MainActivity";

    private CharSequence title;

    private Fragment currentFragment = null;
    private SelectProgramFragment selectProgramFragment;
    private FailedItemsFragment failedItemsFragment;
    private EditItemFragment editItemFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Dhis2.getInstance().enableLoading(this, Dhis2.LOAD_TRACKER);
        Dhis2Application.bus.register(this);
        setContentView(R.layout.activity_main);
        if(Dhis2.hasLoadedInitialData(this))
            showSelectProgramFragment();
        else
            Dhis2.loadInitialData(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.failed_items) {
            showFailedItemsFragment();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle( final CharSequence title )
    {
        this.title = title;
        runOnUiThread(new Runnable() {
            public void run() {
                getSupportActionBar().setTitle( title );
            }
        });
    }

    @Subscribe
    public void onReceiveMessage(MessageEvent event) {
        Log.e(CLASS_TAG, "onreceivemessage");
        if(event.eventType == BaseEvent.EventType.showRegisterEventFragment) {
        //    showRegisterEventFragment();
        } else if(event.eventType == BaseEvent.EventType.showSelectProgramFragment) {
            showSelectProgramFragment();
        } else if(event.eventType == BaseEvent.EventType.showEditItemFragment) {
            showEditItemFragment();
        } else if(event.eventType == BaseEvent.EventType.showFailedItemsFragment ) {
            showFailedItemsFragment();
        } else if(event.eventType == BaseEvent.EventType.onLoadingInitialDataFinished) {
            if(Dhis2.hasLoadedInitialData(this)) {
                showSelectProgramFragment();
            } else {
                //todo: notify the user that data is missing and request to try to re-load.
            }
        }
    }

    public void showFailedItemsFragment() {
        setTitle("Failed Items");
        if(failedItemsFragment == null) failedItemsFragment = new FailedItemsFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, failedItemsFragment);
        fragmentTransaction.commit();
        currentFragment = failedItemsFragment;
    }

    public void showSelectProgramFragment() {
        setTitle("Tracker Capture");
        if(selectProgramFragment == null) selectProgramFragment = new SelectProgramFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, selectProgramFragment);
        fragmentTransaction.commit();
        currentFragment = selectProgramFragment;
    }

    public void showEditItemFragment() {
        Log.e(CLASS_TAG, "showedititemfragment");
        setTitle("Edit Item");
        editItemFragment = new EditItemFragment();
        if(failedItemsFragment == null) return;
        editItemFragment.setItem(failedItemsFragment.getSelectedFailedItem());
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, editItemFragment);
        fragmentTransaction.commit();
        currentFragment = editItemFragment;
    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if ( (keyCode == KeyEvent.KEYCODE_BACK) )
        {
            Dhis2.getInstance().showConfirmDialog(this, getString(R.string.confirm),
                    getString(R.string.exit_confirmation), getString(R.string.yes_option),
                    getString(R.string.no_option),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dialog, int which )
                        {
                            finish();
                            System.exit( 0 );
                        }
                    } );
        }

        return super.onKeyDown( keyCode, event );
    }
}
