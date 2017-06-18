package org.hisp.dhis.android.trackercapture.fragments.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.sdk.events.LoadingMessageEvent;
import org.hisp.dhis.android.sdk.events.UiEvent;
import org.hisp.dhis.android.sdk.ui.views.FontButton;
import org.hisp.dhis.android.sdk.ui.views.FontCheckBox;
import org.hisp.dhis.android.trackercapture.R;
import org.hisp.dhis.android.trackercapture.export.ExportData;

import java.io.IOException;

public class SettingsFragment extends
        org.hisp.dhis.android.sdk.ui.fragments.settings.SettingsFragment{

    private FontButton exportDataButton;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        exportDataButton = (FontButton) view.findViewById(R.id.settings_export_data);
        exportDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onExportDataClick();
            }
        });
        FontCheckBox fontCheckBox = (FontCheckBox) view.findViewById(
                R.id.checkbox_developers_options);

        Context context = getActivity().getApplicationContext();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context);
        fontCheckBox.setChecked(sharedPreferences.getBoolean(
                getActivity().getApplicationContext().getResources().getString(
                        R.string.developer_option_key), false));
        toggleOptions(fontCheckBox.isChecked());
        fontCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
                        getActivity().getApplicationContext());
                SharedPreferences.Editor prefEditor =
                        sharedPref.edit(); // Get preference in editor mode
                prefEditor.putBoolean(
                        getActivity().getApplicationContext().getResources().getString(
                                org.hisp.dhis.android.sdk.R.string.developer_option_key), value);
                prefEditor.commit();
                toggleOptions(value);
            }
        });
    }

    private void toggleOptions(boolean value) {
        if (value) {
            exportDataButton.setVisibility(View.VISIBLE);
        } else {
            exportDataButton.setVisibility(View.INVISIBLE);
        }
    }

    public void onExportDataClick() {
        ExportData exportData = new ExportData();
        Intent emailIntent = null;
        try {
            emailIntent = exportData.dumpAndSendToAIntent(getActivity());
        } catch (IOException e) {
            Toast.makeText(getContext(), org.hisp.dhis.android.sdk.R.string.error_exporting_data, Toast.LENGTH_LONG).show();
        }
        if (emailIntent != null) {
            startActivity(emailIntent);
        }
    }


    @Subscribe
    public void onSynchronizationFinishedEvent(final UiEvent event)
    {
        super.onSynchronizationFinishedEvent(event);
    }

    @Subscribe
    public void onLoadingMessageEvent(final LoadingMessageEvent event) {
        super.onLoadingMessageEvent(event);
    }
}