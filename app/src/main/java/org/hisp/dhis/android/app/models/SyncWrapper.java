package org.hisp.dhis.android.app.models;

import org.hisp.dhis.client.sdk.android.api.utils.DefaultOnSubscribe;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.organisationunit.UserOrganisationUnitInteractor;
import org.hisp.dhis.client.sdk.android.program.UserProgramInteractor;
import org.hisp.dhis.client.sdk.core.common.utils.ModelUtils;
import org.hisp.dhis.client.sdk.core.program.ProgramFields;
import org.hisp.dhis.client.sdk.models.common.state.Action;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnit;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramType;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SyncDateWrapper;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class SyncWrapper {

    private final SyncDateWrapper syncDateWrapper;

    // metadata
    private final UserOrganisationUnitInteractor userOrganisationUnitInteractor;
    private final UserProgramInteractor userProgramInteractor;

    // data
    private final EventInteractor eventInteractor;

    public SyncWrapper(UserOrganisationUnitInteractor userOrganisationUnitInteractor,
                       UserProgramInteractor userProgramInteractor,
                       EventInteractor eventInteractor,
                       SyncDateWrapper syncDateWrapper
    ) {
        this.userOrganisationUnitInteractor = userOrganisationUnitInteractor;
        this.userProgramInteractor = userProgramInteractor;
        this.eventInteractor = eventInteractor;
        this.syncDateWrapper = syncDateWrapper;
    }

    public Observable<List<Program>> syncMetaData() {
        Set<ProgramType> programTypes = new HashSet<>();
        programTypes.add(ProgramType.WITH_REGISTRATION);

        return Observable.zip(userOrganisationUnitInteractor.pull(),
                userProgramInteractor.pull(ProgramFields.DESCENDANTS, programTypes),
                new Func2<List<OrganisationUnit>, List<Program>, List<Program>>() {
                    @Override
                    public List<Program> call(List<OrganisationUnit> units, List<Program> programs) {
                        if (syncDateWrapper != null) {
                            syncDateWrapper.setLastSyncedNow();
                        }
                        return programs;
                    }
                });
    }

    public Observable<List<Event>> syncData() {
        return eventInteractor.list()
                .switchMap(new Func1<List<Event>, Observable<List<Event>>>() {
                    @Override
                    public Observable<List<Event>> call(List<Event> events) {
                        Set<String> uids = ModelUtils.toUidSet(events);
                        if (uids != null && !uids.isEmpty()) {
                            if (syncDateWrapper != null) {
                                syncDateWrapper.setLastSyncedNow();
                            }
                            return eventInteractor.sync(uids);
                        }
                        return Observable.empty();
                    }
                });
    }

    public Observable<Boolean> checkIfSyncIsNeeded() {

        if (eventInteractor == null) {
            // no eventInteractor exists - return false (i.e. sync is not needed)
            return Observable.create(new DefaultOnSubscribe<Boolean>() {
                @Override
                public Boolean call() {
                    return false;
                }
            });
        }

        EnumSet<Action> updateActions = EnumSet.of(Action.TO_POST, Action.TO_UPDATE);
        return eventInteractor.listByActions(updateActions)
                .switchMap(new Func1<List<Event>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(final List<Event> events) {
                        return Observable.create(new DefaultOnSubscribe<Boolean>() {
                            @Override
                            public Boolean call() {
                                return events != null && !events.isEmpty();
                            }
                        });
                    }
                });
    }

    public Observable<List<Event>> backgroundSync() {
        return syncMetaData()
                .subscribeOn(Schedulers.io())
                .switchMap(new Func1<List<Program>, Observable<List<Event>>>() {
                    @Override
                    public Observable<List<Event>> call(List<Program> programs) {
                        if (programs != null) {
                            return syncData();
                        }

                        return Observable.empty();
                    }
                });
    }
}
