package org.hisp.dhis.android.app.models;

import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleActionInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleVariableInteractor;
import org.hisp.dhis.client.sdk.core.common.utils.ModelUtils;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.models.event.Event;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnit;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramRule;
import org.hisp.dhis.client.sdk.models.program.ProgramRuleAction;
import org.hisp.dhis.client.sdk.models.program.ProgramRuleVariable;
import org.hisp.dhis.client.sdk.rules.RuleEffect;
import org.hisp.dhis.client.sdk.rules.RuleEngine;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;
import rx.subscriptions.CompositeSubscription;

public class RxRulesEngine {
    private static final String TAG = RxRulesEngine.class.getSimpleName();

    private final ProgramRuleVariableInteractor programRuleVariableInteractor;
    private final ProgramRuleInteractor programRuleInteractor;
    private final ProgramRuleActionInteractor programRuleActionInteractor;
    private final EventInteractor eventInteractor;
    private final EnrollmentInteractor enrollmentInteractor;

    private Event currentEvent;
    private Enrollment currentEnrollment;
    private final Map<String, Event> eventsMap;

    // engine
    private RuleEngine ruleEngine;
    private Subject<List<RuleEffect>, List<RuleEffect>> ruleEffectSubject;

    // utilities
    private final Logger logger;
    private CompositeSubscription subscription;

    public RxRulesEngine(ProgramRuleInteractor programRuleInteractor,
                         ProgramRuleActionInteractor programRuleActionInteractor,
                         ProgramRuleVariableInteractor programRuleVariableInteractor,
                         EventInteractor eventInteractor, EnrollmentInteractor enrollmentInteractor, Logger logger) {
        this.programRuleVariableInteractor = programRuleVariableInteractor;
        this.programRuleInteractor = programRuleInteractor;
        this.programRuleActionInteractor = programRuleActionInteractor;
        this.eventInteractor = eventInteractor;
        this.enrollmentInteractor = enrollmentInteractor;
        this.eventsMap = new HashMap<>();
        this.logger = logger;
        this.subscription = new CompositeSubscription();
    }

    public Observable<Boolean> init(String eventUid) {
        return eventInteractor.get(eventUid)
                .switchMap(new Func1<Event, Observable<? extends Boolean>>() {
                    @Override
                    public Observable<? extends Boolean> call(final Event event) {
                        final OrganisationUnit organisationUnit = new OrganisationUnit();
                        final Program program = new Program();

                        organisationUnit.setUId(event.getOrgUnit());
                        program.setUId(event.getProgram());

                        return Observable.zip(loadRulesEngine(program),
                                eventInteractor.list(organisationUnit, program),
                                new Func2<RuleEngine, List<Event>, Boolean>() {
                                    @Override
                                    public Boolean call(RuleEngine engine, List<Event> events) {
                                        // assign rules engine
                                        ruleEngine = engine;
                                        currentEvent = event;

                                        // clear events map
                                        eventsMap.clear();

                                        // put all existing events into map
                                        eventsMap.putAll(ModelUtils.toMap(eventInteractor.list(
                                                organisationUnit, program).toBlocking().first()));

                                        // ruleEffectSubject = BehaviorSubject.create();
                                        ruleEffectSubject = ReplaySubject.createWithSize(1);
                                        ruleEffectSubject.subscribeOn(Schedulers.computation());
                                        ruleEffectSubject.observeOn(AndroidSchedulers.mainThread());

                                        return true;
                                    }
                                });
                    }
                });
    }

    public Observable<Boolean> init(Enrollment enrollment) {
        return enrollmentInteractor.get(enrollment.getUId())
                .switchMap(new Func1<Enrollment, Observable<? extends Boolean>>() {
                    @Override
                    public Observable<? extends Boolean> call(final Enrollment enrollment1) {
                        final OrganisationUnit organisationUnit = new OrganisationUnit();
                        final Program program = new Program();

                        organisationUnit.setUId(enrollment1.getOrgUnit());
                        program.setUId(enrollment1.getProgram());

                        return Observable.zip(loadRulesEngine(program),
                                eventInteractor.list(organisationUnit, program),
                                new Func2<RuleEngine, List<Event>, Boolean>() {
                                    @Override
                                    public Boolean call(RuleEngine engine, List<Event> events) {
                                        // assign rules engine
                                        ruleEngine = engine;
                                        currentEnrollment = enrollment1;

                                        // clear events map
                                        eventsMap.clear();

                                        // put all existing events into map
                                        eventsMap.putAll(ModelUtils.toMap(eventInteractor.list(
                                                organisationUnit, program).toBlocking().first()));

                                        // ruleEffectSubject = BehaviorSubject.create();
                                        ruleEffectSubject = ReplaySubject.createWithSize(1);
                                        ruleEffectSubject.subscribeOn(Schedulers.computation());
                                        ruleEffectSubject.observeOn(AndroidSchedulers.mainThread());

                                        return true;
                                    }
                                });
                    }
                });
    }

    public void notifyDataSetChanged() {
        if (currentEvent == null) {
            throw new IllegalArgumentException("No events are associated with RxRulesEngine");
        }

        // first, we need to find out this event in map and replace it
        if (eventsMap.containsKey(currentEvent.getUId())) {
            eventsMap.remove(currentEvent.getUId());
        }

        if (!subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = new CompositeSubscription();
        }

        subscription.add(eventInteractor.get(currentEvent.getUId())
                .map(new Func1<Event, List<RuleEffect>>() {
                    @Override
                    public List<RuleEffect> call(Event event) {
                        logger.d(TAG, "Reloaded event: " + currentEvent.getUId());

                        currentEvent = event;
                        eventsMap.put(event.getUId(), event);

                        logger.d(TAG, "calculating rule effects");
                        return ruleEngine.execute(currentEvent,
                                new ArrayList<>(eventsMap.values()));
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<RuleEffect>>() {
                    @Override
                    public void call(List<RuleEffect> ruleEffects) {
                        logger.d(TAG, "Successfully computed new RuleEffects");
                        ruleEffectSubject.onNext(ruleEffects);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to process event", throwable);
                        ruleEffectSubject.onError(throwable);
                    }
                }));
    }

    public Observable<List<RuleEffect>> observable() {
        return ruleEffectSubject;
    }

    private Observable<RuleEngine> loadRulesEngine(Program program) {
        return Observable.zip(loadProgramRules(program), loadProgramRuleVariables(program),
                new Func2<List<ProgramRule>, List<ProgramRuleVariable>, RuleEngine>() {
                    @Override
                    public RuleEngine call(List<ProgramRule> programRules,
                                           List<ProgramRuleVariable> programRuleVariables) {
                        return new RuleEngine.Builder()
                                .programRuleVariables(programRuleVariables)
                                .programRules(programRules)
                                .build();
                    }
                });
    }

    private Observable<List<ProgramRule>> loadProgramRules(Program program) {
        return programRuleInteractor.list(program)
                .map(new Func1<List<ProgramRule>, List<ProgramRule>>() {
                    @Override
                    public List<ProgramRule> call(List<ProgramRule> programRules) {
                        if (programRules == null) {
                            programRules = new ArrayList<>();
                        }

                        for (ProgramRule programRule : programRules) {
                            List<ProgramRuleAction> programRuleActions = programRuleActionInteractor
                                    .list(programRule).toBlocking().first();
                            programRule.setProgramRuleActions(programRuleActions);
                        }

                        return programRules;
                    }
                });
    }

    private Observable<List<ProgramRuleVariable>> loadProgramRuleVariables(Program program) {
        return programRuleVariableInteractor.list(program)
                .map(new Func1<List<ProgramRuleVariable>, List<ProgramRuleVariable>>() {
                    @Override
                    public List<ProgramRuleVariable> call(List<ProgramRuleVariable> variables) {
                        if (variables == null) {
                            variables = new ArrayList<>();
                        }

                        return variables;
                    }
                });
    }
}
