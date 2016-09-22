package org.hisp.dhis.android.app.presenters;

import org.hisp.dhis.android.app.models.RxRulesEngine;
import org.hisp.dhis.android.app.views.DataEntryView;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.optionset.OptionSetInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramTrackedEntityAttributeInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityAttributeValueInteractor;
import org.hisp.dhis.client.sdk.android.user.CurrentUserInteractor;
import org.hisp.dhis.client.sdk.core.common.network.UserCredentials;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.models.optionset.Option;
import org.hisp.dhis.client.sdk.models.optionset.OptionSet;
import org.hisp.dhis.client.sdk.models.program.Program;
import org.hisp.dhis.client.sdk.models.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.client.sdk.rules.RuleEffect;
import org.hisp.dhis.client.sdk.ui.bindings.commons.RxOnValueChangedListener;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;
import org.hisp.dhis.client.sdk.ui.models.FormEntityAction;
import org.hisp.dhis.client.sdk.ui.models.FormEntityCharSequence;
import org.hisp.dhis.client.sdk.ui.models.FormEntityCheckBox;
import org.hisp.dhis.client.sdk.ui.models.FormEntityDate;
import org.hisp.dhis.client.sdk.ui.models.FormEntityEditText;
import org.hisp.dhis.client.sdk.ui.models.FormEntityFilter;
import org.hisp.dhis.client.sdk.ui.models.FormEntityRadioButtons;
import org.hisp.dhis.client.sdk.ui.models.FormEntityText;
import org.hisp.dhis.client.sdk.ui.models.Picker;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;
import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;

public class DataEntryPresenterImpl implements DataEntryPresenter {
    private static final String TAG = DataEntryPresenterImpl.class.getSimpleName();

    private final CurrentUserInteractor currentUserInteractor;

    private final OptionSetInteractor optionSetInteractor;
    private final EnrollmentInteractor enrollmentInteractor;
    private final ProgramInteractor programInteractor;
    private final ProgramTrackedEntityAttributeInteractor programTrackedEntityAttributeInteractor;
    private final TrackedEntityAttributeValueInteractor trackedEntityAttributeValueInteractor;

    private final RxRulesEngine rxRulesEngine;

    private final Logger logger;
    private final RxOnValueChangedListener onValueChangedListener;

    private DataEntryView dataEntryView;
    private CompositeSubscription subscription;


    public DataEntryPresenterImpl(CurrentUserInteractor currentUserInteractor,
                                  OptionSetInteractor optionSetInteractor,
                                  EnrollmentInteractor enrollmentInteractor,
                                  ProgramInteractor programInteractor,
                                  ProgramTrackedEntityAttributeInteractor programTrackedEntityAttributeInteractor,
                                  TrackedEntityAttributeValueInteractor trackedEntityAttributeValueInteractor,
                                  RxRulesEngine rxRulesEngine, Logger logger) {
        this.currentUserInteractor = currentUserInteractor;
        this.optionSetInteractor = optionSetInteractor;
        this.enrollmentInteractor = enrollmentInteractor;
        this.programInteractor = programInteractor;
        this.programTrackedEntityAttributeInteractor = programTrackedEntityAttributeInteractor;

        this.trackedEntityAttributeValueInteractor = trackedEntityAttributeValueInteractor;
        this.rxRulesEngine = rxRulesEngine;

        this.logger = logger;
        this.onValueChangedListener = new RxOnValueChangedListener();
    }

    @Override
    public void attachView(View view) {
        isNull(view, "view must not be null");
        dataEntryView = (DataEntryView) view;
    }

    @Override
    public void detachView() {
        dataEntryView = null;

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void createDataEntryFormEnrollment(String enrollmentId, String programId) {
        logger.d(TAG, "ProgramId: " + programId);

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }

        subscription = new CompositeSubscription();
        subscription.add(saveTrackedEntityAttributeValues());
        subscription.add(initRulesEngine());

        subscription.add(Observable.zip(
                enrollmentInteractor.get(enrollmentId),
                programInteractor.get(programId),
                currentUserInteractor.userCredentials(),
//                rxRulesEngine.observable(),
                new Func3<Enrollment, Program, UserCredentials, //List<ProgramRuleEffects> effects,
                                        AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>>>() {

                    @Override
                    public AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>> call(
                            Enrollment enrollment, Program program, UserCredentials creds) { //List<RuleEffects> effects

                        List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes =
                                programTrackedEntityAttributeInteractor.list(program).toBlocking().first();
                        String username = creds.getUsername();

                        Collections.sort(programTrackedEntityAttributes,
                                ProgramTrackedEntityAttribute.SORT_ORDER_COMPARATOR);

                        List<FormEntity> formEntities = transformTrackedEntityAttributes(
                                username, enrollment, programTrackedEntityAttributes);
                        List<FormEntityAction> formEntityActions = new ArrayList(); //transformRuleEffects(effects);

                        return new AbstractMap.SimpleEntry<>(formEntities, formEntityActions);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>>>() {
                    @Override
                    public void call(AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>> result) {
                        if (dataEntryView != null) {
                            dataEntryView.showDataEntryForm(result.getKey(), result.getValue());
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Something went wrong during form construction", throwable);
                    }
                }));
    }

    @Override
    public void createDataEntryFormSection(String eventId, String programStageSectionId) {
//        logger.d(TAG, "ProgramStageSectionId: " + programStageSectionId);
//
//        if (subscription != null && !subscription.isUnsubscribed()) {
//            subscription.unsubscribe();
//            subscription = null;
//        }
//
//        subscription = new CompositeSubscription();
//        subscription.add(saveTrackedEntityAttributeValues());
//        subscription.add(initRulesEngine());
//
//        subscription.add(Observable.zip(
//                eventInteractor.get(eventId), sectionInteractor.get(programStageSectionId),
//                currentUserInteractor.userCredentials(), rxRulesEngine.observable(),
//                new Func4<Event, ProgramStageSection, UserCredentials, List<RuleEffect>,
//                        AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>>>() {
//
//                    @Override
//                    public AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>> call(
//                            Event event, ProgramStageSection stageSection, UserCredentials creds,
//                            List<RuleEffect> effects) {
//                        List<ProgramStageDataElement> dataElements = dataElementInteractor
//                                .list(stageSection).toBlocking().first();
//
//                        // sort ProgramStageDataElements by sortOrder
//                        if (dataElements != null) {
//                            Collections.sort(dataElements,
//                                    ProgramStageDataElement.SORT_ORDER_COMPARATOR);
//                        }
//
//                        String username = creds.getUsername();
//
//                        List<FormEntity> formEntities = transformTrackedEntityAttributes(
//                                username, event, dataElements);
//                        List<FormEntityAction> formEntityActions = transformRuleEffects(effects);
//
//                        return new AbstractMap.SimpleEntry<>(formEntities, formEntityActions);
//                    }
//                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Action1<AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>>>() {
//                    @Override
//                    public void call(AbstractMap.SimpleEntry<List<FormEntity>, List<FormEntityAction>> entry) {
//                        if (dataEntryView != null) {
//                            dataEntryView.showDataEntryForm(entry.getKey(), entry.getValue());
//                        }
//                    }
//                }, new Action1<Throwable>() {
//                    @Override
//                    public void call(Throwable throwable) {
//                        logger.e(TAG, "Something went wrong during form construction", throwable);
//                    }
//                }));
    }

    private Subscription initRulesEngine() {
        return rxRulesEngine.observable()
                .map(new Func1<List<RuleEffect>, List<FormEntityAction>>() {
                    @Override
                    public List<FormEntityAction> call(List<RuleEffect> ruleEffects) {
                        logger.d(TAG, "RuleEffects are emitted: " +
                                System.identityHashCode(ruleEffects));
                        return transformRuleEffects(ruleEffects);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<FormEntityAction>>() {
                    @Override
                    public void call(List<FormEntityAction> actions) {
                        if (dataEntryView != null) {
                            dataEntryView.updateDataEntryForm(actions);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to calculate rules", throwable);
                    }
                });
    }

    private Subscription saveTrackedEntityAttributeValues() {
        return Observable.create(onValueChangedListener)
                .debounce(512, TimeUnit.MILLISECONDS)
                .switchMap(new Func1<FormEntity, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(FormEntity formEntity) {
                        return onFormEntityChanged(formEntity);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSaved) {
                        if (isSaved) {
                            logger.d(TAG, "data value is saved successfully");

                            // fire rule engine execution
//                            rxRulesEngine.notifyDataSetChanged();
                        } else {
                            logger.d(TAG, "Failed to save value");
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, "Failed to save value", throwable);
                    }
                });
    }

    private List<FormEntity> transformTrackedEntityAttributes(
            String username, Enrollment enrollment, List<ProgramTrackedEntityAttribute> programTrackedEntityAttributes) {
        if (programTrackedEntityAttributes == null || programTrackedEntityAttributes.isEmpty()) {
            return new ArrayList<>();
        }

        // List<DataElement> dataElements = new ArrayList<>();
        for (ProgramTrackedEntityAttribute programTrackedEntityAttribute : programTrackedEntityAttributes) {
            TrackedEntityAttribute trackedEntityAttribute = programTrackedEntityAttribute.getTrackedEntityAttribute();
            if (trackedEntityAttribute == null) {
                throw new RuntimeException("Malformed metadata: Program" +
                        "TrackedEntityAttribute " + programTrackedEntityAttribute.getUId() +
                        " does not have reference to TrackedEntityAttribute");
            }

            OptionSet optionSet = trackedEntityAttribute.getOptionSet();
            if (optionSet != null) {
                List<Option> options = optionSetInteractor.list(
                        trackedEntityAttribute.getOptionSet()).toBlocking().first();
                optionSet.setOptions(options);
            }
        }

        Map<String, TrackedEntityAttributeValue> attributeValueMap = new HashMap<>();
        if (enrollment.getTrackedEntityAttributeValues() != null && !enrollment.getTrackedEntityAttributeValues().isEmpty()) {
            for (TrackedEntityAttributeValue attributeValue : enrollment.getTrackedEntityAttributeValues()) {
                attributeValueMap.put(attributeValue.getTrackedEntityAttributeUId(), attributeValue);
            }
        }

        List<FormEntity> formEntities = new ArrayList<>();
        for (ProgramTrackedEntityAttribute programTrackedEntityAttribute : programTrackedEntityAttributes) {
            TrackedEntityAttribute trackedEntityAttribute = programTrackedEntityAttribute.getTrackedEntityAttribute();
            formEntities.add(transformTrackedEntityAttribute(
                    username, enrollment, attributeValueMap.get(trackedEntityAttribute.getUId()), programTrackedEntityAttribute));
        }

        return formEntities;
    }

    private FormEntity transformTrackedEntityAttribute(String username, Enrollment enrollment,
                                                       TrackedEntityAttributeValue trackedEntityAttributeValue,
                                                       ProgramTrackedEntityAttribute programTrackedEntityAttribute) {
        TrackedEntityAttribute trackedEntityAttribute = programTrackedEntityAttribute.getTrackedEntityAttribute();

        // logger.d(TAG, "DataElement: " + trackedEntityAttribute.getDisplayName());
        // logger.d(TAG, "ValueType: " + trackedEntityAttribute.getValueType());

        // create TrackedEntityDataValue upfront
        if (trackedEntityAttributeValue == null) {
            trackedEntityAttributeValue = new TrackedEntityAttributeValue();
            trackedEntityAttributeValue.setTrackedEntityInstance(enrollment.getTrackedEntityInstance());
            trackedEntityAttributeValue.setTrackedEntityAttributeUId(trackedEntityAttribute.getUId());
        }

        // logger.d(TAG, "transformTrackedEntityAttribute() -> TrackedEntityDataValue: " +
        //        trackedEntityAttributeValue + " localId: " + trackedEntityAttributeValue.getId());

        // in case if we have option set linked to data-element, we
        // need to process it regardless of data-element value type
        if (trackedEntityAttribute.getOptionSet() != null) {
            List<Option> options = trackedEntityAttribute.getOptionSet().getOptions();

            Picker picker = new Picker.Builder()
                    .hint(trackedEntityAttribute.getDisplayName())
                    .build();
            if (options != null && !options.isEmpty()) {
                for (Option option : options) {
                    Picker childPicker = new Picker.Builder()
                            .id(option.getCode())
                            .name(option.getDisplayName())
                            .parent(picker)
                            .build();
                    picker.addChild(childPicker);

                    if (option.getCode().equals(trackedEntityAttributeValue.getValue())) {
                        picker.setSelectedChild(childPicker);
                    }
                }
            }

            FormEntityFilter formEntityFilter = new FormEntityFilter(trackedEntityAttribute.getUId(),
                    getFormEntityLabel(programTrackedEntityAttribute), trackedEntityAttributeValue);
            formEntityFilter.setPicker(picker);
            formEntityFilter.setOnFormEntityChangeListener(onValueChangedListener);

            return formEntityFilter;
        }

        switch (trackedEntityAttribute.getValueType()) {
            case TEXT: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.TEXT, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case LONG_TEXT: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.LONG_TEXT, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case PHONE_NUMBER: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.TEXT, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case EMAIL: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.TEXT, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case NUMBER: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.NUMBER, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case INTEGER: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.INTEGER, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case INTEGER_POSITIVE: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.INTEGER_POSITIVE, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case INTEGER_NEGATIVE: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.INTEGER_NEGATIVE, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case INTEGER_ZERO_OR_POSITIVE: {
                FormEntityEditText formEntityEditText = new FormEntityEditText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), FormEntityEditText.InputType.INTEGER_ZERO_OR_POSITIVE, trackedEntityAttributeValue);
                formEntityEditText.setValue(trackedEntityAttributeValue.getValue());
                formEntityEditText.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityEditText;
            }
            case DATE: {
                FormEntityDate formEntityDate = new FormEntityDate(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute), trackedEntityAttributeValue);
                formEntityDate.setValue(trackedEntityAttributeValue.getValue());
                formEntityDate.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityDate;
            }
            case BOOLEAN: {
                FormEntityRadioButtons formEntityRadioButtons = new FormEntityRadioButtons(
                        trackedEntityAttribute.getUId(), getFormEntityLabel(programTrackedEntityAttribute), trackedEntityAttributeValue);
                formEntityRadioButtons.setValue(trackedEntityAttributeValue.getValue());
                formEntityRadioButtons.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityRadioButtons;
            }
            case TRUE_ONLY: {
                FormEntityCheckBox formEntityCheckBox = new FormEntityCheckBox(
                        trackedEntityAttribute.getUId(), getFormEntityLabel(programTrackedEntityAttribute), trackedEntityAttributeValue);
                formEntityCheckBox.setValue(trackedEntityAttributeValue.getValue());
                formEntityCheckBox.setOnFormEntityChangeListener(onValueChangedListener);
                return formEntityCheckBox;
            }
            default:
                logger.d(TAG, "Unsupported FormEntity type: " + trackedEntityAttribute.getValueType());

                FormEntityText formEntityText = new FormEntityText(trackedEntityAttribute.getUId(),
                        getFormEntityLabel(programTrackedEntityAttribute));
                formEntityText.setValue("Unsupported value type: " + trackedEntityAttribute.getValueType());

                return formEntityText;
        }
    }

    private List<FormEntityAction> transformRuleEffects(List<RuleEffect> ruleEffects) {
        List<FormEntityAction> entityActions = new ArrayList<>();
        if (ruleEffects == null || ruleEffects.isEmpty()) {
            return entityActions;
        }

        for (RuleEffect ruleEffect : ruleEffects) {
            if (ruleEffect == null || ruleEffect.getProgramRuleActionType() == null) {
                logger.d(TAG, "failed processing broken RuleEffect");
                continue;
            }

            switch (ruleEffect.getProgramRuleActionType()) {
                case HIDEFIELD: {
                    if (ruleEffect.getTrackedEntityAttribute() != null) {
                        String trackedEntityAttributeUid = ruleEffect.getTrackedEntityAttribute().getUId();
                        FormEntityAction formEntityAction = new FormEntityAction(
                                trackedEntityAttributeUid, null, FormEntityAction.FormEntityActionType.HIDE);
                        entityActions.add(formEntityAction);
                    }
                    break;
                }
                case ASSIGN: {
                    if (ruleEffect.getTrackedEntityAttribute() != null) {
                        String trackedEntityAttributeUid = ruleEffect.getTrackedEntityAttribute().getUId();
                        FormEntityAction formEntityAction = new FormEntityAction(
                                trackedEntityAttributeUid, ruleEffect.getData(), FormEntityAction.FormEntityActionType.ASSIGN);
                        entityActions.add(formEntityAction);
                    }
                    break;
                }
            }
        }

        return entityActions;
    }

    private String getFormEntityLabel(ProgramTrackedEntityAttribute programTrackedEntityAttribute) {
        TrackedEntityAttribute trackedEntityAttribute = programTrackedEntityAttribute.getTrackedEntityAttribute();
        String label = isEmpty(trackedEntityAttribute.getDisplayName()) ?
                trackedEntityAttribute.getDisplayName() : trackedEntityAttribute.getName();

        if (programTrackedEntityAttribute.isMandatory()) {
            label = label + " (*)";
        }

        return label;
    }

    private Observable<Boolean> onFormEntityChanged(FormEntity formEntity) {
        return trackedEntityAttributeValueInteractor.save(mapFormEntityToDataValue(formEntity));
    }

    private TrackedEntityAttributeValue mapFormEntityToDataValue(FormEntity entity) {
        if (entity instanceof FormEntityFilter) {
            Picker picker = ((FormEntityFilter) entity).getPicker();

            String value = "";
            if (picker != null && picker.getSelectedChild() != null) {
                value = picker.getSelectedChild().getId();
            }

            TrackedEntityAttributeValue trackedEntityAttributeValue;
            if (entity.getTag() != null) {
                trackedEntityAttributeValue = (TrackedEntityAttributeValue) entity.getTag();
            } else {
                throw new IllegalArgumentException("TrackedEntityAttributeValue must be " +
                        "assigned to FormEntity upfront");
            }

            trackedEntityAttributeValue.setValue(value);

            logger.d(TAG, "New value " + value + " is emitted for " + entity.getLabel());

            return trackedEntityAttributeValue;
        } else if (entity instanceof FormEntityCharSequence) {
            String value = ((FormEntityCharSequence) entity).getValue().toString();

            TrackedEntityAttributeValue trackedEntityAttributeValue;
            if (entity.getTag() != null) {
                trackedEntityAttributeValue = (TrackedEntityAttributeValue) entity.getTag();
            } else {
                throw new IllegalArgumentException("TrackedEntityAttributeValue must be " +
                        "assigned to FormEntity upfront");
            }

            trackedEntityAttributeValue.setValue(value);

            logger.d(TAG, "New value " + value + " is emitted for " + entity.getLabel());

            return trackedEntityAttributeValue;
        }

        return null;
    }
}
