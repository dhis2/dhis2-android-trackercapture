package org.hisp.dhis.android.app.views;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.hisp.dhis.android.app.FormComponent;
import org.hisp.dhis.android.app.R;
import org.hisp.dhis.android.app.TrackerCaptureApp;
import org.hisp.dhis.android.app.presenters.EnrollmentFormPresenter;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.ui.fragments.DatePickerDialogFragment;
import org.hisp.dhis.client.sdk.ui.fragments.FilterableDialogFragment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;
import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

public class EnrollmentFormActivity extends AppCompatActivity implements EnrollmentFormView {
// TODO check if configuration changes are handled properly

    private static final String ARG_ENROLLMENT_UID = "arg:enrollmentUid";
    private static final String ARG_IS_ENROLLMENT_NEW = "arg:isEnrollmentNew";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final int LOCATION_REQUEST_CODE = 42;
    private static final String TAG = EnrollmentFormActivity.class.getSimpleName();

    @Inject
    EnrollmentFormPresenter enrollmentFormPresenter;

    // root layout
    CoordinatorLayout coordinatorLayout;

    // collapsing toolbar views
    TextView textViewReportDate;
    LinearLayout linearLayoutCoordinates;
    EditText editTextLatitude;
    EditText editTextLongitude;
    AppCompatImageView locationIcon;
    AppCompatImageView locationIconCancel;
    CircularProgressBar locationProgressBar;
    FrameLayout locationButtonLayout;

    // section tabs and view pager
    TabLayout tabLayout;
    ViewPager viewPager;
    FloatingActionButton fabComplete;

    FilterableDialogFragment sectionDialogFragment;
    AlertDialog alertDialog;

    public static void navigateToNewEnrollment(Activity activity, String enrollmentUid) {
        navigateTo(activity, enrollmentUid, true);
    }

    public static void navigateToExistingEnrollment(Activity activity, String enrollmentUid) {
        navigateTo(activity, enrollmentUid, false);
    }

    private static void navigateTo(Activity activity, String enrollmentUid, boolean isEnrollmentNew) {
        isNull(activity, "activity must not be null");

        Intent intent = new Intent(activity, EnrollmentFormActivity.class);
        intent.putExtra(ARG_ENROLLMENT_UID, enrollmentUid);
        intent.putExtra(ARG_IS_ENROLLMENT_NEW, isEnrollmentNew);
        activity.startActivity(intent);
    }

    private String getEnrollmentUid() {
        if (getIntent().getExtras() == null || getIntent().getExtras()
                .getString(ARG_ENROLLMENT_UID, null) == null) {
            throw new IllegalArgumentException("You must pass enrollment uid in intent extras");
        }

        return getIntent().getExtras().getString(ARG_ENROLLMENT_UID, null);
    }

    private boolean isEnrollmentNew() {
        return getIntent().getExtras().getBoolean(ARG_IS_ENROLLMENT_NEW, false);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment_form);

        setupCoordinatorLayout();
        setupToolbar();
        setupPickers();
        setupViewPager();
        setupFloatingActionButton();

        // attach listener if dialog opened (post-configuration change)
        attachListenerToExistingFragment();

        FormComponent formComponent = ((TrackerCaptureApp) getApplication()).getFormComponent();

        // first time activity is created
        if (savedInstanceState == null) {
            // it means we found old component and we have to release it
            if (formComponent != null) {
                // create new instance of component
                ((TrackerCaptureApp) getApplication()).releaseFormComponent();
            }

            formComponent = ((TrackerCaptureApp) getApplication()).createFormComponent();
        } else {
            formComponent = ((TrackerCaptureApp) getApplication()).getFormComponent();
        }

        // if it is first time when FormSectionsActivity is
        // instantiated, we need to show DatePickerDialog
        if (savedInstanceState == null && isEnrollmentNew()) {
            showDatePickerDialog();
        }

        // inject dependencies
        formComponent.inject(this);

        // start building the form
        enrollmentFormPresenter.createDataEntryForm(getEnrollmentUid());

        setupLocationPermissions();
    }

    @Override
    protected void onStart() {
        enrollmentFormPresenter.attachView(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        //don't leak the dialog
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        enrollmentFormPresenter.detachView();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (sectionDialogFragment != null) {
            getMenuInflater().inflate(R.menu.menu_form_sections, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                super.onBackPressed();
                return true;
            }
            case R.id.filter_button:
                if (sectionDialogFragment != null) {
                    sectionDialogFragment.show(getSupportFragmentManager(), FilterableDialogFragment.TAG);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setLocation(Location location) {
        setLocationButtonState(true);
        if (location != null) {
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            if (longitude != 0.0 && latitude != 0.0) {
                editTextLatitude.setText(String.format(Locale.getDefault(), "%1$,.6f", longitude));
                editTextLongitude.setText(String.format(Locale.getDefault(), "%1$,.6f", latitude));
            }
        } else {
            Toast.makeText(this, R.string.gps_no_coordinates, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void setLocationButtonState(boolean enabled) {
        if (enabled) {
            //re-enable the location fields and location button
            locationIcon.setVisibility(View.VISIBLE);
            locationIconCancel.setVisibility(View.GONE);
            locationProgressBar.setVisibility(View.GONE);
            locationButtonLayout.setClickable(true);
        } else {
            // disable it:
            locationIcon.setVisibility(View.GONE);
            locationIconCancel.setVisibility(View.VISIBLE);
            locationProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            List<String> permissionsList = Arrays.asList(permissions);
            int at = permissionsList.indexOf(Manifest.permission.ACCESS_FINE_LOCATION);
            if (at >= 0 && grantResults[at] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: permission is granged");
                // don't do anything
            } else if (at >= 0 && grantResults[at] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(EnrollmentFormActivity.this,
                        R.string.gps_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Initialize the location permissions.
     */
    public void setupLocationPermissions() {
        Log.d(TAG, "setupLocationPermissions() called with: " + "");
        if (Build.VERSION.SDK_INT > 22 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_REQUEST_CODE);
        }
    }

    @Override
    public void showFormDefaultSection(String formSectionId) {
        FormSingleSectionAdapter viewPagerAdapter =
                new FormSingleSectionAdapter(getSupportFragmentManager());
        viewPagerAdapter.swapData(getEnrollmentUid(), formSectionId);

        // in order not to loose state of ViewPager, first we
        // have to fill FormSectionsAdapter with data, and only then set it to ViewPager
        viewPager.setAdapter(viewPagerAdapter);

        // hide tab layout
        tabLayout.setVisibility(View.GONE);

        // if we don't have sections, we don't need to show navigation drawer
        // sectionsDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END);

        // we also need to hide filter icon
        supportInvalidateOptionsMenu();
    }

//    @Override
//    public void showFormSections(List<FormSection> formSections) {
//        FormSectionsAdapter viewPagerAdapter =
//                new FormSectionsAdapter(getSupportFragmentManager());
//        viewPagerAdapter.swapData(getEnrollmentUid(), formSections);
//
//        // in order not to loose state of ViewPager, first we
//        // have to fill FormSectionsAdapter with data, and only then set it to ViewPager
//        viewPager.setAdapter(viewPagerAdapter);
//
//        // hide tab layout
//        tabLayout.setVisibility(View.VISIBLE);
//
//        // TabLayout will fail on you, if ViewPager which is going to be
//        // attached does not contain ViewPagerAdapter set to it.
//        tabLayout.setupWithViewPager(viewPager);
//    }

//    @Override
//    public void setFormSectionsPicker(Picker picker) {
//        sectionDialogFragment = FilterableDialogFragment.newInstance(picker);
//        sectionDialogFragment.setOnPickerItemClickListener(new OnSearchSectionsClickListener());
//
//        supportInvalidateOptionsMenu();
//    }
    //TODO Adapt this for dateLabel to display enrollment.getDateOfEnrollmentLabel() if exists, if not fallback to "Report date"
    @Override
    public void showReportDatePicker(String hint, String value) {
        String dateLabel = isEmpty(hint) ? getString(R.string.report_date) : hint;
        textViewReportDate.setHint(dateLabel);

        if (!isEmpty(value)) {
            textViewReportDate.setText(String.format(Locale.getDefault(),
                    "%s: %s", dateLabel, value));
        }
    }

    @Override
    public void showCoordinatesPicker(String latitude, String longitude) {
        if (linearLayoutCoordinates.getVisibility() == View.INVISIBLE ||
                linearLayoutCoordinates.getVisibility() == View.GONE) {
            linearLayoutCoordinates.setVisibility(View.VISIBLE);
            setupLocationCallback();
        }
        if (!isEmpty(latitude)) {
            editTextLatitude.setText(latitude);
        }
        if (!isEmpty(longitude)) {
            editTextLongitude.setText(longitude);
        }
    }

    @Override
    public void showEnrollmentStatus(Enrollment.EnrollmentStatus enrollmentStatus) {
        if (fabComplete != null && enrollmentStatus != null) {
            fabComplete.setVisibility(View.VISIBLE);
            fabComplete.setActivated(Enrollment.EnrollmentStatus.COMPLETED.equals(enrollmentStatus));
        }
    }
    //TODO Should adapt to save Enrollment button
    private void attachListenerToExistingFragment() {
//        FilterableDialogFragment dialogFragment = (FilterableDialogFragment)
//                getSupportFragmentManager().findFragmentByTag(FilterableDialogFragment.TAG);
//
//        // if we don't have fragment attached to activity,
//        // we don't want to do anything else
//        if (dialogFragment != null) {
//            dialogFragment.setOnPickerItemClickListener(new OnSearchSectionsClickListener());
//        }
    }

    private void setupCoordinatorLayout() {
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorlayout_form);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupPickers() {
        textViewReportDate = (TextView) findViewById(R.id.textview_report_date);
        linearLayoutCoordinates = (LinearLayout) findViewById(R.id.linearlayout_coordinates);
        editTextLatitude = (EditText) findViewById(R.id.edittext_latitude);
        editTextLongitude = (EditText) findViewById(R.id.edittext_longitude);
        locationIcon = (AppCompatImageView) findViewById(R.id.imagevew_location);
        locationIconCancel = (AppCompatImageView) findViewById(R.id.imagevew_location_cancel);
        locationProgressBar = (CircularProgressBar) findViewById(R.id.progress_bar_circular_location);
        locationButtonLayout = (FrameLayout) findViewById(R.id.button_location_layout);

        // set on click listener to text view report date
        textViewReportDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePickerDialog();
            }
        });

        // since coordinates are optional, initially they should be hidden
        linearLayoutCoordinates.setVisibility(View.GONE);
    }

    private void setupLocationCallback() {
        locationButtonLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager locationManager = (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE);
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                    //we have permission ?
                    if (Build.VERSION.SDK_INT < 23 ||
                            ActivityCompat.checkSelfPermission(v.getContext(),
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                    == PackageManager.PERMISSION_GRANTED) {
                        //either if init or after cancel click:
                        if (locationIcon.getVisibility() == View.VISIBLE
                                || locationIconCancel.getVisibility() == View.GONE) {
                            // request location:
                            setLocationButtonState(false);
                            enrollmentFormPresenter.subscribeToLocations();
                        } else {
                            //cancel the location request:
                            setLocationButtonState(true);
                            enrollmentFormPresenter.stopLocationUpdates();
                        }
                    } else {
                        //don't have permissions, set them up !
                        setupLocationPermissions();
                    }
                } else {
                    showGpsDialog();
                }
            }
        });
    }

    public void showGpsDialog() {
        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_gps_disabled)
                .setMessage(R.string.gps_disabled)
                .setPositiveButton(R.string.settings_option, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel_option, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();

        alertDialog.show();
    }

    private void setupViewPager() {
        tabLayout = (TabLayout) findViewById(R.id.tablayout_data_entry);
        viewPager = (ViewPager) findViewById(R.id.viewpager_dataentry);

        // hide tab layout initially in order to prevent UI
        // jumps in cases when we don't have sections
        tabLayout.setVisibility(View.GONE);
    }

    private void setupFloatingActionButton() {
        fabComplete = (FloatingActionButton) findViewById(R.id.fab_complete_event);
        fabComplete.setVisibility(View.GONE);

        fabComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (validateForm()) {

                    Snackbar.make(coordinatorLayout, getString(R.string.enrollment_saved)
                            , Snackbar.LENGTH_LONG).show();

                     TrackedEntityInstanceDashboardActivity.navigateTo(EnrollmentFormActivity.this, getEnrollmentUid());
                }
            }
        });
    }

    private boolean validateForm() {
        return enrollmentFormPresenter.validateForm(getEnrollmentUid());
    }

    private void incompleteEvent() {
        enrollmentFormPresenter.saveEnrollmentStatus(getEnrollmentUid(), Enrollment.EnrollmentStatus.ACTIVE);
    }

    private void completeEvent() {
        enrollmentFormPresenter.saveEnrollmentStatus(getEnrollmentUid(), Enrollment.EnrollmentStatus.COMPLETED);
    }

    private void showDatePickerDialog() {
        final DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                String stringDate = (new SimpleDateFormat(DATE_FORMAT, Locale.US))
                        .format(calendar.getTime());

                String newValue = String.format(Locale.getDefault(), "%s: %s",
                        getString(R.string.report_date), stringDate);
                textViewReportDate.setText(newValue);

                DateTime currentDateTime = DateTime.now();
                DateTime selectedDateTime = DateTime.parse(stringDate);

                /*
                * in case when user selected today's date, we need to know about time as well.
                * selectedDateTime does not contain time information (only date), that's why we
                * need to create a new DateTime object by calling DateTime.now()
                */
                DateTime dateTime;
                if (DateTimeComparator.getDateOnlyInstance()
                        .compare(currentDateTime, selectedDateTime) == 0) {
                    dateTime = currentDateTime;
                } else {
                    dateTime = selectedDateTime;
                }

                enrollmentFormPresenter.saveDateOfEnrollment(getEnrollmentUid(), dateTime);
            }
        };

        DatePickerDialogFragment datePickerDialogFragment =
                DatePickerDialogFragment.newInstance(false);
        datePickerDialogFragment.setOnDateSetListener(onDateSetListener);
        datePickerDialogFragment.show(getSupportFragmentManager());
    }

    /*
    *
    * This adapter exists only in order to satisfy cases when there is no
    * sections assigned to program stage. As the result, we have to
    * use program stage itself as section
    *
    */
    private static class FormSingleSectionAdapter extends FragmentStatePagerAdapter {
        private static final int DEFAULT_STAGE_COUNT = 1;
        private static final int DEFAULT_STAGE_POSITION = 0;
        private String enrollmentUid;
        private String programId;

        public FormSingleSectionAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            if (DEFAULT_STAGE_POSITION == position && !isEmpty(programId)) {
                return DataEntryFragment.newInstanceForEnrollment(enrollmentUid, programId);
            }

            return null;
        }

        @Override
        public int getCount() {
            return isEmpty(programId) ? 0 : DEFAULT_STAGE_COUNT;
        }

        public void swapData(String enrollmentUid, String programId) {
            this.enrollmentUid = enrollmentUid;
            this.programId = programId;
            this.notifyDataSetChanged();
        }
    }
}