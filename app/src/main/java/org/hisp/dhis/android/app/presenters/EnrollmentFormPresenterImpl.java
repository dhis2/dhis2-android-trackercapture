package org.hisp.dhis.android.app.presenters;

import android.location.Location;

import org.hisp.dhis.android.app.LocationProvider;
import org.hisp.dhis.android.app.models.RxRulesEngine;
import org.hisp.dhis.android.app.views.EnrollmentFormView;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageSectionInteractor;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramStage;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormSection;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.utils.Logger;
import org.joda.time.DateTime;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;

public class EnrollmentFormPresenterImpl implements EnrollmentFormPresenter {
    private static final String TAG = EnrollmentFormPresenterImpl.class.getSimpleName();
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private final ProgramInteractor programInteractor;
    private final ProgramStageInteractor programStageInteractor;
    private final ProgramStageSectionInteractor programStageSectionInteractor;

    private final EventInteractor eventInteractor;
    private final EnrollmentInteractor enrollmentInteractor;
    private final RxRulesEngine rxRuleEngine;

    private final Logger logger;

    private EnrollmentFormView enrollmentFormView;
    private CompositeSubscription subscription;

    private LocationProvider locationProvider;
    private boolean gettingLocation = false;

    public EnrollmentFormPresenterImpl(ProgramInteractor programInteractor, ProgramStageInteractor programStageInteractor,
                                       ProgramStageSectionInteractor stageSectionInteractor,
                                       EventInteractor eventInteractor, EnrollmentInteractor enrollmentInteractor, RxRulesEngine rxRuleEngine,
                                       LocationProvider locationProvider, Logger logger) {
        this.programInteractor = programInteractor;
        this.programStageInteractor = programStageInteractor;
        this.programStageSectionInteractor = stageSectionInteractor;
        this.eventInteractor = eventInteractor;
        this.enrollmentInteractor = enrollmentInteractor;
        this.rxRuleEngine = rxRuleEngine;
        this.locationProvider = locationProvider;
        this.logger = logger;
    }

    @Override
    public void attachView(View view) {
        isNull(view, "View must not be null");
        enrollmentFormView = (EnrollmentFormView) view;
        if (gettingLocation) {
            enrollmentFormView.setLocationButtonState(false);
        }
    }

    @Override
    public void detachView() {
        enrollmentFormView = null;

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void createDataEntryForm(final String enrollmentUid) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }

        subscription = new CompositeSubscription();
        subscription.add(enrollmentInteractor.get(enrollmentUid)
                .map(new Func1<Enrollment, Enrollment>() {
                    @Override
                    public Enrollment call(Enrollment enrollment) {

                        // TODO consider refactoring rules-engine logic out of map function)
                        // synchronously initializing rule engine

                        //TODO: Implement rule engine for enrollments
                        rxRuleEngine.init(enrollment).toBlocking().first();

                        // compute initial RuleEffects
//                        rxRuleEngine.notifyDataSetChanged();
                        return enrollment;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Enrollment>() {
                    @Override
                    public void call(Enrollment enrollment) {
                        isNull(enrollment, String.format("Enrollment with uid %s does not exist", enrollmentUid));

                        if (enrollmentFormView != null) {
                            enrollmentFormView.showEnrollmentStatus(enrollment.getStatus());
                        }

                        // fire next operations
                        subscription.add(showFormPickers(enrollment));
                        subscription.add(showFormSection(enrollment));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, null, throwable);
                    }
                }));
    }

    @Override
    public void saveDateOfEnrollment(final String enrollmentUid, final DateTime dateOfEnrollment) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }

        subscription = new CompositeSubscription();
        subscription.add(enrollmentInteractor.get(enrollmentUid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Enrollment>() {
                    @Override
                    public void call(Enrollment enrollment) {
                        isNull(enrollment, String.format("Enrollment with uid %s does not exist", enrollmentUid));

                        enrollment.setDateOfEnrollment(dateOfEnrollment);

                        subscription.add(saveEnrollment(enrollment));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, null, throwable);
                    }
                }));
    }

    @Override
    public void saveEnrollmentStatus(final String enrollmentUid, final Enrollment.EnrollmentStatus enrollmentStatus) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }

        subscription = new CompositeSubscription();
        subscription.add(enrollmentInteractor.get(enrollmentUid)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Enrollment>() {
                    @Override
                    public void call(Enrollment enrollment) {
                        isNull(enrollment, String.format("Enrollment with uid %s does not exist", enrollmentUid));

                        enrollment.setStatus(enrollmentStatus);

                        subscription.add(saveEnrollment(enrollment));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, null, throwable);
                    }
                }));
    }

    @Override
    public boolean validateForm(final String enrollmentUid) {
       //rxRuleEngine.validateForm(getEnrollmentUid);

        return true;
    }

    private void viewSetLocation(Location location) {
        if (enrollmentFormView != null) {
            enrollmentFormView.setLocation(location);
        }
    }

    @Override
    public void subscribeToLocations() {
        gettingLocation = true;
        locationProvider.locations()
                .timeout(31L, TimeUnit.SECONDS)
                .buffer(2L, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<List<Location>>() {
                            @Override
                            public void call(List<Location> locations) {
                                if (locations.isEmpty() || locations.get(0) == null) {
                                    return;
                                }
                                Location currentLocation = locations.get(0);
                                Location bestLocation = currentLocation;
                                float accuracyAverage = currentLocation.getAccuracy();
                                //go over the locations and find the best + keep average
                                for (int i = 1; i < locations.size(); i++) {
                                    currentLocation = locations.get(i);
                                    accuracyAverage += currentLocation.getAccuracy();
                                    if (locationProvider.isBetterLocation(currentLocation, bestLocation)) {
                                        bestLocation = currentLocation;
                                    }
                                }
                                accuracyAverage = accuracyAverage / locations.size();
                                // if accuracy doesn't improve and we have more than one, we have the best estimate.
                                if (Math.round(accuracyAverage)
                                        == Math.round(bestLocation.getAccuracy())
                                        && locations.size() > 1) {
                                    gettingLocation = false;
                                    viewSetLocation(bestLocation);
                                    locationProvider.stopUpdates();
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                if (throwable instanceof TimeoutException) {
                                    logger.d(TAG, "Rx subscribeToLocaitons() timed out.");
                                } else {
                                    logger.e(TAG, "subscribeToLocations() rx call :" + throwable);
                                }
                                gettingLocation = false;
                                viewSetLocation(null);
                                locationProvider.stopUpdates();
                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                logger.d(TAG, "onComplete");
                                gettingLocation = false;
                                viewSetLocation(null);
                                locationProvider.stopUpdates();
                            }
                        }
                );
        locationProvider.requestLocation();
    }

    @Override
    public void stopLocationUpdates() {
        locationProvider.stopUpdates();
    }

    private Subscription saveEnrollment(final Enrollment enrollment) {
        return enrollmentInteractor.save(enrollment)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSaved) {
                        if (isSaved) {
                            logger.d(TAG, "Successfully saved enrollment " + enrollment);
                        } else {
                            logger.d(TAG, "Failed to save enrollment " + enrollment);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to save enrollment " + enrollment, throwable);
                    }
                });
    }

    private Subscription showFormPickers(final Enrollment enrollment) {

        return loadProgram(enrollment.getProgram())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Program>() {
                    @Override
                    public void call(Program program) {
                        if (enrollmentFormView != null) {
                            String dateOfEnrollment = enrollment.getDateOfEnrollment() != null ?
                                    enrollment.getDateOfEnrollment().toString(DATE_FORMAT) : "";
                            enrollmentFormView.showReportDatePicker(
                                    program.getEnrollmentDateLabel(), dateOfEnrollment);

                            if(program.isDisplayIncidentDate()) {
                                String dateOfIncident = enrollment.getDateOfIncident() != null ?
                                        enrollment.getDateOfIncident().toString(DATE_FORMAT) : "";
                                enrollmentFormView.showReportDatePicker(
                                        program.getIncidentDateLabel(), dateOfIncident);
                            }
                            /**
                             * TODO If program should capture coordinates during Enrollment: implement the code below
                             */
//                            if (program.isCaptureCoordinates()) {
//                                String latitude = null;
//                                String longitude = null;
//
//                                if (enrollment.getCoordinate() != null &&
//                                        enrollment.getCoordinate().getLatitude() != null) {
//                                    latitude = String.valueOf(enrollment.getCoordinate().getLatitude());
//                                }
//
//                                if (enrollment.getCoordinate() != null &&
//                                        enrollment.getCoordinate().getLongitude() != null) {
//                                    longitude = String.valueOf(enrollment.getCoordinate().getLongitude());
//                                }
//
//                                enrollmentFormView.showCoordinatesPicker(latitude, longitude);
//                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to fetch program", throwable);
                    }
                });
    }

    private Subscription showFormSection(final Enrollment currentEnrollment) {

        return loadProgram(currentEnrollment.getProgram())
                .map(new Func1<Program, AbstractMap.SimpleEntry<Picker, List<FormSection>>>() {
                    @Override
                    public AbstractMap.SimpleEntry<Picker, List<FormSection>> call(Program program) {

                        // TODO remove hardcoded prompt
                        Picker picker = new Picker.Builder()
                                .id(program.getUId())
                                .name("Choose section")
                                .build();

                        // transform sections
                        List<FormSection> formSections = new ArrayList<>();

                        formSections.add(new FormSection(
                                program.getUId(), program.getDisplayName()));
                        picker.addChild(
                                new Picker.Builder()
                                        .id(program.getUId())
                                        .name(program.getDisplayName())
                                        .parent(picker)
                                        .build());



                        return new AbstractMap.SimpleEntry<>(picker, formSections);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AbstractMap.SimpleEntry<Picker, List<FormSection>>>() {
                    @Override
                    public void call(AbstractMap.SimpleEntry<Picker, List<FormSection>> results) {
                        if (results != null && enrollmentFormView != null) {
                            if (results.getValue() != null) {
                                enrollmentFormView.showFormDefaultSection(results.getKey().getId());
                            }
//                            else {
//                                enrollmentFormView.showFormSections(results.getValue());
//                                enrollmentFormView.setFormSectionsPicker(results.getKey());
//                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Form construction failed", throwable);
                    }
                });
    }

    private Observable<ProgramStage> loadProgramStage(final Program program) {
        return programStageInteractor.list(program)
                .map(new Func1<List<ProgramStage>, ProgramStage>() {
                    @Override
                    public ProgramStage call(List<ProgramStage> stages) {
                        // since this form is intended to be used in event capture
                        // and programs for event capture apps consist only from one
                        // and only one program stage, we can just retrieve it from the list
                        if (stages == null || stages.isEmpty()) {
                            logger.e(TAG, "Form construction failed. No program " +
                                    "stages are assigned to given program: " + program.getUId());
                            return null;
                        }

                        return stages.get(0);
                    }
                });
    }

    private Observable<Program> loadProgram(final String programUid) {
        return programInteractor.get(programUid).asObservable();
    }
}
