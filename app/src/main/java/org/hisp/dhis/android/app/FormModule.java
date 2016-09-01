package org.hisp.dhis.android.app;

import org.hisp.dhis.android.app.models.RxRulesEngine;
import org.hisp.dhis.android.app.presenters.DataEntryPresenter;
import org.hisp.dhis.android.app.presenters.DataEntryPresenterImpl;
import org.hisp.dhis.android.app.presenters.EnrollmentFormPresenter;
import org.hisp.dhis.android.app.presenters.EnrollmentFormPresenterImpl;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.optionset.OptionSetInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleActionInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleVariableInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageSectionInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramTrackedEntityAttributeInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityAttributeValueInteractor;
import org.hisp.dhis.client.sdk.android.user.CurrentUserInteractor;
import org.hisp.dhis.client.sdk.utils.Logger;

import javax.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

@Module
public class FormModule {

    public FormModule() {
        // explicit empty constructor
    }

    @Provides
    @PerActivity
    public RxRulesEngine providesRuleEngine(
            @Nullable ProgramRuleInteractor programRuleInteractor,
            @Nullable ProgramRuleActionInteractor programRuleActionInteractor,
            @Nullable ProgramRuleVariableInteractor programRuleVariableInteractor,
            @Nullable EnrollmentInteractor enrollmentInteractor,
            @Nullable EventInteractor eventInteractor, Logger logger) {
        return new RxRulesEngine(programRuleInteractor, programRuleActionInteractor,
                programRuleVariableInteractor, eventInteractor, enrollmentInteractor, logger);
    }

    @Provides
    @PerActivity
    public EnrollmentFormPresenter providesEnrollmentFormPresenter(
            @Nullable ProgramInteractor programInteractor,
            @Nullable ProgramStageInteractor programStageInteractor,
            @Nullable ProgramStageSectionInteractor stageSectionInteractor,
            @Nullable EventInteractor eventInteractor,
            @Nullable EnrollmentInteractor enrollmentInteractor,
            RxRulesEngine rxRulesEngine,
            LocationProvider locationProvider, Logger logger) {
        return new EnrollmentFormPresenterImpl(programInteractor, programStageInteractor,
                stageSectionInteractor, eventInteractor, enrollmentInteractor, rxRulesEngine, locationProvider, logger);
    }

    @Provides
    public DataEntryPresenter providesDataEntryPresenter(
            @Nullable CurrentUserInteractor currentUserInteractor,
            @Nullable EnrollmentInteractor enrollmentInteractor,
            @Nullable ProgramInteractor programInteractor,
            @Nullable TrackedEntityAttributeValueInteractor trackedEntityAttributeValueInteractor,
            @Nullable ProgramTrackedEntityAttributeInteractor programTrackedEntityAttributeInteractor,
            @Nullable OptionSetInteractor optionSetInteractor,
            RxRulesEngine rxRulesEngine, Logger logger) {
        return new DataEntryPresenterImpl(currentUserInteractor,optionSetInteractor,
                enrollmentInteractor,programInteractor,
                programTrackedEntityAttributeInteractor,trackedEntityAttributeValueInteractor,
                rxRulesEngine,logger);
    }
}
