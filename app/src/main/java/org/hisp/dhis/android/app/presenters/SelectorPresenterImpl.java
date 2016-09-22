package org.hisp.dhis.android.app.presenters;

import org.hisp.dhis.android.app.models.SyncWrapper;
import org.hisp.dhis.android.app.views.SelectorView;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.organisationunit.UserOrganisationUnitInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageDataElementInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageInteractor;
import org.hisp.dhis.client.sdk.android.program.UserProgramInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityInstanceInteractor;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.core.common.utils.ModelUtils;
import org.hisp.dhis.client.sdk.models.common.state.State;
import org.hisp.dhis.client.sdk.models.dataelement.DataElement;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnit;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramStage;
import org.hisp.dhis.client.sdk.models.program.ProgramStageDataElement;
import org.hisp.dhis.client.sdk.models.program.ProgramType;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityDataValue;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.client.sdk.ui.bindings.commons.ApiExceptionHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.AppError;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SessionPreferences;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SyncDateWrapper;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;
import org.hisp.dhis.client.sdk.utils.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;
import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

public class SelectorPresenterImpl implements SelectorPresenter {
    private static final String TAG = SelectorPresenterImpl.class.getSimpleName();
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private final UserOrganisationUnitInteractor userOrganisationUnitInteractor;
    private final UserProgramInteractor userProgramInteractor;
    private final ProgramInteractor programInteractor;
    private final ProgramStageInteractor programStageInteractor;
    private final ProgramStageDataElementInteractor programStageDataElementInteractor;
    private final EnrollmentInteractor enrollmentInteractor;
    private final TrackedEntityInstanceInteractor trackedEntityInstanceInteractor;
    private final EventInteractor eventInteractor;

    private final SessionPreferences sessionPreferences;
    private final SyncDateWrapper syncDateWrapper;
    private final ApiExceptionHandler apiExceptionHandler;
    private final SyncWrapper syncWrapper;
    private final Logger logger;

    private CompositeSubscription subscription;
    private boolean hasSyncedBefore;
    private SelectorView selectorView;
    private boolean isSyncing;

    public SelectorPresenterImpl(UserOrganisationUnitInteractor interactor,
                                 UserProgramInteractor userProgramInteractor,
                                 ProgramInteractor programInteractor, ProgramStageInteractor programStageInteractor,
                                 ProgramStageDataElementInteractor stageDataElementInteractor,
                                 EnrollmentInteractor enrollmentInteractor, TrackedEntityInstanceInteractor trackedEntityInstanceInteractor, EventInteractor eventInteractor,
                                 SessionPreferences sessionPreferences,
                                 SyncDateWrapper syncDateWrapper,
                                 SyncWrapper syncWrapper,
                                 ApiExceptionHandler apiExceptionHandler,
                                 Logger logger) {
        this.userOrganisationUnitInteractor = interactor;
        this.userProgramInteractor = userProgramInteractor;
        this.programInteractor = programInteractor;
        this.programStageInteractor = programStageInteractor;
        this.programStageDataElementInteractor = stageDataElementInteractor;
        this.enrollmentInteractor = enrollmentInteractor;
        this.trackedEntityInstanceInteractor = trackedEntityInstanceInteractor;
        this.eventInteractor = eventInteractor;
        this.sessionPreferences = sessionPreferences;
        this.syncDateWrapper = syncDateWrapper;
        this.syncWrapper = syncWrapper;
        this.apiExceptionHandler = apiExceptionHandler;
        this.logger = logger;

        this.subscription = new CompositeSubscription();
        this.hasSyncedBefore = false;
    }

    private static void traverseAndSetDefaultSelection(Picker tree) {
        if (tree != null) {

            Picker node = tree;
            do {
                if (node.getChildren().size() == 1) {
                    // get the only child node and set it as selected
                    Picker singleChild = node.getChildren().get(0);
                    node.setSelectedChild(singleChild);
                }
            } while ((node = node.getSelectedChild()) != null);
        }
    }

    public void attachView(View view) {
        isNull(view, "SelectorView must not be null");

        selectorView = (SelectorView) view;

        if (isSyncing) {
            selectorView.showProgressBar();
        } else {
            selectorView.hideProgressBar();
        }

        // check if metadata was synced,
        // if not, syncMetaData it
        if (!isSyncing && !hasSyncedBefore) {
            sync();
        }

        listPickers();
    }

    @Override
    public void detachView() {
        selectorView.hideProgressBar();
        selectorView = null;
    }

    @Override
    public void onPickersSelectionsChanged(List<Picker> pickerList) {
        if (pickerList != null) {
            sessionPreferences.clearSelectedPickers();
            for (int index = 0; index < pickerList.size(); index++) {
                Picker current = pickerList.get(index);
                Picker child = current.getSelectedChild();
                if (child == null) { //done with pickers. exit.
                    return;
                }
                String pickerId = child.getId();
                sessionPreferences.setSelectedPickerUid(index, pickerId);
            }
        }
    }

    @Override
    public void sync() {
        selectorView.showProgressBar();
        isSyncing = true;
        subscription.add(syncWrapper.syncMetaData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Program>>() {
                    @Override
                    public void call(List<Program> programs) {
                        isSyncing = false;
                        hasSyncedBefore = true;

                        if (selectorView != null) {
                            selectorView.hideProgressBar();
                        }
                        listPickers();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        isSyncing = false;
                        hasSyncedBefore = true;
                        if (selectorView != null) {
                            selectorView.hideProgressBar();
                        }
                        handleError(throwable);
                    }
                }));
        subscription.add(syncWrapper.syncData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Event>>() {
                    @Override
                    public void call(List<Event> events) {
                        listPickers();

                        logger.d(TAG, "Synced events successfully");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to sync events", throwable);
                    }
                }));
    }

    @Override
    public void listPickers() {
        logger.d(TAG, "listPickers()");
        subscription.add(Observable.zip(
                userOrganisationUnitInteractor.list(),
                userProgramInteractor.list(),
                new Func2<List<OrganisationUnit>, List<Program>, Picker>() {
                    @Override
                    public Picker call(List<OrganisationUnit> units, List<Program> programs) {
                        return createPickerTree(units, programs);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Picker>() {
                    @Override
                    public void call(Picker picker) {
                        if (selectorView != null) {
                            selectorView.showPickers(picker);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed listing pickers.", throwable);
                    }
                }));
    }

    @Override
    public void listEnrollments(String organisationUnitId, String programId) {
        final OrganisationUnit orgUnit = new OrganisationUnit();
        final Program program = new Program();

        orgUnit.setUId(organisationUnitId);
        program.setUId(programId);

        subscription.add(programStageInteractor.list(program)
                .switchMap(new Func1<List<ProgramStage>, Observable<List<ReportEntity>>>() {
                    @Override
                    public Observable<List<ReportEntity>> call(List<ProgramStage> stages) {
                        if (stages == null || stages.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Program should contain at least one program stage");
                        }

                        Observable<List<ProgramStageDataElement>> stageDataElements =
                                programStageDataElementInteractor.list(stages.get(0));

                        return Observable.zip(stageDataElements, eventInteractor.list(orgUnit, program),
                                new Func2<List<ProgramStageDataElement>, List<Event>, List<ReportEntity>>() {

                                    @Override
                                    public List<ReportEntity> call(List<ProgramStageDataElement> stageDataElements,
                                                                   List<Event> events) {
                                        return transformEvents(stageDataElements, events);
                                    }
                                });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<ReportEntity>>() {
                    @Override
                    public void call(List<ReportEntity> reportEntities) {
                        if (selectorView != null) {
                            selectorView.showReportEntities(reportEntities);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed loading enrollments", throwable);
                    }
                }));
    }

    @Override
    public void createEnrollment(final String orgUnitId, final String programId) {
        final OrganisationUnit orgUnit = new OrganisationUnit();
        final Program program = new Program();
        orgUnit.setUId(orgUnitId);
        program.setUId(programId);

        subscription.add(programInteractor.get(programId)
                .map(new Func1<Program, TrackedEntityInstance>() {
                    @Override
                    public TrackedEntityInstance call(Program program) {
                        if (program == null || program.getTrackedEntity() == null) {
                            throw new IllegalArgumentException("In order to create enrollment, " +
                                    "we need program and tracked entity to be in place");
                        }
                        TrackedEntityInstance trackedEntityInstance = trackedEntityInstanceInteractor.create(
                                orgUnit,
                                program.getTrackedEntity());


                        return trackedEntityInstance;
                    }
                })
                .map(new Func1<TrackedEntityInstance, Enrollment>() {
                    @Override
                    public Enrollment call(TrackedEntityInstance trackedEntityInstance) {
                        if (trackedEntityInstance == null) {
                            throw new IllegalArgumentException("In order to create enrollment, " +
                                    "we need a tracked entity instance to be in place");
                        }

                        DateTime dateOfEnrollment = DateTime.now();
                        //TODO Consider having popup dialog in your face asking for dateOfEnrollment and dateOfIncident
                        DateTime dateOfIncident = null;
                        if(program.isDisplayIncidentDate()) {
                            dateOfIncident = DateTime.now();
                        }
                        Enrollment enrollment = enrollmentInteractor.create(
                                orgUnit,
                                trackedEntityInstance,
                                program,
                                true,
                                dateOfEnrollment,
                                dateOfIncident).toBlocking().first();

                        enrollmentInteractor.save(enrollment).toBlocking().first();
                        return enrollment;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Enrollment>() {
                    @Override
                    public void call(Enrollment enrollment) {
                        if (selectorView != null) {
                            selectorView.navigateToFormSectionActivity(enrollment);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed creating enrollment", throwable);
                    }
                })
        );
    }

    @Override
    public void deleteEnrollment(final ReportEntity reportEntity) {
        subscription.add(enrollmentInteractor.get(reportEntity.getId())
                .switchMap(new Func1<Enrollment, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Enrollment enrollment) {
                        return enrollmentInteractor.remove(enrollment);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        logger.d(TAG, "Enrollment deleted");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Error deleting enrollment: " + reportEntity, throwable);
                        if (selectorView != null) {
                            selectorView.onReportEntityDeletionError(reportEntity);
                        }
                    }
                }));
    }

    @Override
    public void handleError(final Throwable throwable) {
        AppError error = apiExceptionHandler.handleException(TAG, throwable);

        if (throwable instanceof ApiException) {
            ApiException exception = (ApiException) throwable;

            if (exception.getResponse() != null) {
                switch (exception.getResponse().getStatus()) {
                    case HttpURLConnection.HTTP_UNAUTHORIZED: {
                        selectorView.showError(error.getDescription());
                        break;
                    }
                    case HttpURLConnection.HTTP_NOT_FOUND: {
                        selectorView.showError(error.getDescription());
                        break;
                    }
                    default: {
                        selectorView.showUnexpectedError(error.getDescription());
                        break;
                    }
                }
            }
        } else {
            logger.e(TAG, "handleError", throwable);
        }
    }

    private List<ReportEntity> transformEvents(List<ProgramStageDataElement> dataElements,
                                               List<Event> events) {

        // preventing additional work
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }

        // sort events by eventDate
        Collections.sort(events, Event.DATE_COMPARATOR);
        Collections.reverse(events);

        // retrieve state map for given events
        // it is done synchronously
        Map<Long, State> stateMap = eventInteractor.map(events)
                .toBlocking().first();
        List<ProgramStageDataElement> filteredElements =
                filterProgramStageDataElements(dataElements);
        List<ReportEntity> reportEntities = new ArrayList<>();

        for (Event event : events) {
            // status of event
            ReportEntity.Status status;
            // get state of event from database
            State state = stateMap.get(event.getId());
            // State state = eventInteractor.get(event).toBlocking().first();

            logger.d(TAG, "State action for event " + event + " is " + state.getAction());
            switch (state.getAction()) {
                case SYNCED: {
                    status = ReportEntity.Status.SENT;
                    break;
                }
                case TO_POST: {
                    status = ReportEntity.Status.TO_POST;
                    break;
                }
                case TO_UPDATE: {
                    status = ReportEntity.Status.TO_UPDATE;
                    break;
                }
                case ERROR: {
                    status = ReportEntity.Status.ERROR;
                    break;
                }
                default: {
                    throw new IllegalArgumentException(
                            "Unsupported event state: " + state.getAction());
                }
            }

            Map<String, String> dataElementToValueMap =
                    mapDataElementToValue(event.getDataValues());

            ArrayList<String> dataElementLabels = new ArrayList<>();

            dataElementToValueMap.put(Event.EVENT_DATE_KEY,
                    event.getEventDate().toString(DateTimeFormat.forPattern(DATE_FORMAT)));
            dataElementToValueMap.put(Event.STATUS_KEY, event.getStatus().toString());

            reportEntities.add(
                    new ReportEntity(
                            event.getUId(),
                            status,
                            dataElementToValueMap));

        }

        return reportEntities;
    }

    private List<ProgramStageDataElement> filterProgramStageDataElements(
            List<ProgramStageDataElement> dataElements) {

        List<ProgramStageDataElement> filteredElements = new ArrayList<>();
        if (dataElements != null && !dataElements.isEmpty()) {
            for (ProgramStageDataElement dataElement : dataElements) {
                if (dataElement.isDisplayInReports()) {
                    filteredElements.add(dataElement);
                }
            }
        }
        return filteredElements;
    }

    private Map<String, String> mapDataElementToValue(List<TrackedEntityDataValue> dataValues) {
        Map<String, String> dataElementToValueMap = new HashMap<>();

        if (dataValues != null && !dataValues.isEmpty()) {
            for (TrackedEntityDataValue dataValue : dataValues) {
                String value = !isEmpty(dataValue.getValue()) ? dataValue.getValue() : "";
                dataElementToValueMap.put(dataValue.getDataElement(), value);
            }
        }
        return dataElementToValueMap;
    }

    /*
     * Goes through given organisation units and programs and builds Picker tree
     */
    private Picker createPickerTree(List<OrganisationUnit> units, List<Program> programs) {
        Map<String, OrganisationUnit> organisationUnitMap = ModelUtils.toMap(units);
        Map<String, Program> assignedProgramsMap = ModelUtils.toMap(programs);

        String chooseOrganisationUnit = selectorView != null ? selectorView
                .getPickerLabel(SelectorView.ID_CHOOSE_ORGANISATION_UNIT) : "";
        String chooseProgram = selectorView != null ? selectorView
                .getPickerLabel(SelectorView.ID_CHOOSE_PROGRAM) : "";

        if (selectorView != null &&
                (organisationUnitMap == null || organisationUnitMap.isEmpty())) {
            selectorView.showNoOrganisationUnitsError();
        }

        Picker rootPicker = new Picker.Builder()
                .hint(chooseOrganisationUnit)
                .build();
        for (String unitKey : organisationUnitMap.keySet()) {
            // creating organisation unit picker items
            OrganisationUnit organisationUnit = organisationUnitMap.get(unitKey);
            Picker organisationUnitPicker = new Picker.Builder()
                    .id(organisationUnit.getUId())
                    .name(organisationUnit.getDisplayName())
                    .hint(chooseProgram)
                    .parent(rootPicker)
                    .build();

            if (organisationUnit.getPrograms() != null && !organisationUnit.getPrograms().isEmpty()) {
                for (Program program : organisationUnit.getPrograms()) {
                    Program assignedProgram = assignedProgramsMap.get(program.getUId());

                    if (assignedProgram != null && ProgramType.WITH_REGISTRATION
                            .equals(assignedProgram.getProgramType())) {
                        Picker programPicker = new Picker.Builder()
                                .id(assignedProgram.getUId())
                                .name(assignedProgram.getDisplayName())
                                .parent(organisationUnitPicker)
                                .build();
                        organisationUnitPicker.addChild(programPicker);
                    }
                }
            }
            rootPicker.addChild(organisationUnitPicker);
        }

        // set saved selections or default ones:
        if (sessionPreferences.getSelectedPickerUid(0) != null) {
            traverseAndSetSavedSelection(rootPicker);
        } else {
            // Traverse the tree. If there is a path with nodes
            // which have only one child, set default selection
            traverseAndSetDefaultSelection(rootPicker);
        }
        return rootPicker;
    }

    private void traverseAndSetSavedSelection(Picker node) {
        int treeLevel = 0;
        while (node != null) {
            String pickerId = sessionPreferences.getSelectedPickerUid(treeLevel);
            if (pickerId != null) {
                for (Picker child : node.getChildren()) {

                    if (child.getId().equals(pickerId)) {
                        node.setSelectedChild(child);
                        break;
                    }
                }
            }
            treeLevel++;
            node = node.getSelectedChild();
        }
    }
}

