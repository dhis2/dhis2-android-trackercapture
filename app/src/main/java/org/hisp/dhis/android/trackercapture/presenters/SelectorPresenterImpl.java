package org.hisp.dhis.android.trackercapture.presenters;


import org.hisp.dhis.android.trackercapture.SyncWrapper;
import org.hisp.dhis.android.trackercapture.views.SelectorView;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.organisationunit.UserOrganisationUnitInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageDataElementInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageInteractor;
import org.hisp.dhis.client.sdk.android.program.UserProgramInteractor;
import org.hisp.dhis.client.sdk.core.common.utils.ModelUtils;
import org.hisp.dhis.client.sdk.core.systeminfo.SystemInfoPreferences;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnit;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramStageDataElement;
import org.hisp.dhis.client.sdk.models.program.ProgramType;
import org.hisp.dhis.client.sdk.ui.bindings.commons.ApiExceptionHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SessionPreferences;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SyncDateWrapper;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;

public class SelectorPresenterImpl implements SelectorPresenter {
    private static final String TAG = SelectorPresenterImpl.class.getSimpleName();

    private final UserOrganisationUnitInteractor userOrganisationUnitInteractor;
    private final UserProgramInteractor userProgramInteractor;
    private final ProgramStageInteractor programStageInteractor;
    private final ProgramStageDataElementInteractor programStageDataElementInteractor;
    private final EventInteractor eventInteractor;

    private final SessionPreferences sessionPreferences;
    private final SyncDateWrapper syncDateWrapper;
    private final ApiExceptionHandler apiExceptionHandler;
    private final SyncWrapper syncWrapper;
    private final Logger logger;

    private CompositeSubscription subscription;
    private SelectorView selectorView;
    private boolean isSyncing;
    private boolean hasSyncedBefore;

    public SelectorPresenterImpl(UserOrganisationUnitInteractor interactor,
                                 UserProgramInteractor userProgramInteractor,
                                 ProgramStageInteractor programStageInteractor,
                                 ProgramStageDataElementInteractor stageDataElementInteractor,
                                 EventInteractor eventInteractor,
                                 SessionPreferences sessionPreferences,
                                 SyncDateWrapper syncDateWrapper,
                                 SyncWrapper syncWrapper,
                                 ApiExceptionHandler apiExceptionHandler,
                                 Logger logger) {
        this.userOrganisationUnitInteractor = interactor;
        this.userProgramInteractor = userProgramInteractor;
        this.programStageInteractor = programStageInteractor;
        this.programStageDataElementInteractor = stageDataElementInteractor;
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

        // check if metadata was synced,
        // if not, syncMetaData it
        if (!isSyncing && !hasSyncedBefore) {
            sync();
        }

        listPickers();
    }

    @Override
    public void detachView() {
        selectorView = null;

        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = new CompositeSubscription();
        }
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
    public void handleError(Throwable throwable) {

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

    @Override
    public void listEnrollments(String organisationUnitId, String programId) {

    }

    @Override
    public void createEnrollment(String organisationUnitId, String programId) {

    }

    @Override
    public void deleteEnrollment(ReportEntity reportEntity) {

    }

}
