/*
 * Copyright (c) 2016, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.app;

import android.content.Context;

import org.hisp.dhis.android.app.models.SyncWrapper;
import org.hisp.dhis.android.app.presenters.SelectorPresenter;
import org.hisp.dhis.android.app.presenters.SelectorPresenterImpl;
import org.hisp.dhis.client.sdk.android.api.D2;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.event.EventInteractor;
import org.hisp.dhis.client.sdk.android.optionset.OptionSetInteractor;
import org.hisp.dhis.client.sdk.android.organisationunit.OrganisationUnitInteractor;
import org.hisp.dhis.client.sdk.android.organisationunit.UserOrganisationUnitInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleActionInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramRuleVariableInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageDataElementInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramStageSectionInteractor;
import org.hisp.dhis.client.sdk.android.program.ProgramTrackedEntityAttributeInteractor;
import org.hisp.dhis.client.sdk.android.program.UserProgramInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityAttributeValueInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityDataValueInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityInstanceInteractor;
import org.hisp.dhis.client.sdk.android.user.CurrentUserInteractor;
import org.hisp.dhis.client.sdk.core.common.network.Configuration;
import org.hisp.dhis.client.sdk.ui.AppPreferences;
import org.hisp.dhis.client.sdk.ui.bindings.commons.ApiExceptionHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultAppAccountManager;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultAppAccountManagerImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultNotificationHandler;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultNotificationHandlerImpl;
import org.hisp.dhis.client.sdk.ui.bindings.commons.DefaultUserModule;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SessionPreferences;
import org.hisp.dhis.client.sdk.ui.bindings.commons.SyncDateWrapper;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.HomePresenter;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.HomePresenterImpl;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.LauncherPresenter;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.LauncherPresenterImpl;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.LoginPresenter;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.LoginPresenterImpl;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.ProfilePresenter;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.ProfilePresenterImpl;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.SettingsPresenter;
import org.hisp.dhis.client.sdk.ui.bindings.presenters.SettingsPresenterImpl;
import org.hisp.dhis.client.sdk.utils.Logger;

import javax.annotation.Nullable;

import dagger.Module;
import dagger.Provides;

import static org.hisp.dhis.client.sdk.utils.StringUtils.isEmpty;


@Module
public class UserModule implements DefaultUserModule {
    private final String authority;
    private final String accountType;

    public UserModule(String authority, String accountType) {
        this(null, authority, accountType);
    }

    public UserModule(String serverUrl, String authority, String accountType) {
        this.authority = authority;
        this.accountType = accountType;

        if (!isEmpty(serverUrl)) {
            // it can throw exception in case if configuration has failed
            Configuration configuration = new Configuration(serverUrl);
            D2.configure(configuration).toBlocking().first();
        }
    }

    @Provides
    @Nullable
    @PerUser
    @Override
    public CurrentUserInteractor providesCurrentUserInteractor() {
        if (D2.isConfigured()) {
            return D2.me();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public UserOrganisationUnitInteractor providesUserOrganisationUnitInteractor() {
        if (D2.isConfigured()) {
            return D2.me().organisationUnits();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public UserProgramInteractor providesUserProgramInteractor() {
        if (D2.isConfigured()) {
            return D2.me().programs();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramStageInteractor providesProgramStageInteractor() {
        if (D2.isConfigured()) {
            return D2.programStages();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramStageSectionInteractor providesProgramStageSectionInteractor() {
        if (D2.isConfigured()) {
            return D2.programStageSections();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramStageDataElementInteractor providesProgramStageDataElementInteractor() {
        if (D2.isConfigured()) {
            return D2.programStageDataElements();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public TrackedEntityAttributeValueInteractor providesTrackedEntityAttributeValueInteractor() {
        if (D2.isConfigured()) {
            return D2.trackedEntityAttributeValues();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramTrackedEntityAttributeInteractor providesProgramTrackedEntityAttributeInteractor() {
        if (D2.isConfigured()) {
            return D2.programTrackedEntityAttributes();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public OrganisationUnitInteractor providesOrganisationUnitInteractor() {
        if (D2.isConfigured()) {
            return D2.organisationUnits();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramInteractor providesProgramInteractor() {
        if (D2.isConfigured()) {
            return D2.programs();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public OptionSetInteractor providesOptionSetInteractor() {
        if (D2.isConfigured()) {
            return D2.optionSets();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public EventInteractor providesEventInteractor() {
        if (D2.isConfigured()) {
            return D2.events();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public TrackedEntityDataValueInteractor provideTrackedEntityDataValueInteractor() {
        if (D2.isConfigured()) {
            return D2.trackedEntityDataValues();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public EnrollmentInteractor provideEnrollmentInteractor() {
        if (D2.isConfigured()) {
            return D2.enrollments();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public TrackedEntityInstanceInteractor provideTrackedEntityInstanceInteractor() {
        if (D2.isConfigured()) {
            return D2.trackedEntityInstances();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramRuleInteractor providesProgramRuleInteractor() {
        if (D2.isConfigured()) {
            return D2.programRules();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramRuleActionInteractor providesProgramRuleActionInteractor() {
        if (D2.isConfigured()) {
            return D2.programRuleActions();
        }

        return null;
    }

    @Provides
    @Nullable
    @PerUser
    public ProgramRuleVariableInteractor providesProgramRuleVariableInteractor() {
        if (D2.isConfigured()) {
            return D2.programRuleVariables();
        }

        return null;
    }

    @Provides
    @PerUser
    @Override
    public LauncherPresenter providesLauncherPresenter(
            @Nullable CurrentUserInteractor accountInteractor) {
        return new LauncherPresenterImpl(accountInteractor);
    }

    @Provides
    @PerUser
    @Override
    public LoginPresenter providesLoginPresenter(
            @Nullable CurrentUserInteractor accountInteractor,
            ApiExceptionHandler apiExceptionHandler, Logger logger) {
        return new LoginPresenterImpl(accountInteractor, apiExceptionHandler, logger);
    }


    @Provides
    @PerUser
    @Override
    public SettingsPresenter providesSettingsPresenter(AppPreferences appPreferences,
                                                       DefaultAppAccountManager appAccountManager) {
        return new SettingsPresenterImpl(appPreferences, appAccountManager);
    }

    @Provides
    @PerUser
    @Override
    public DefaultAppAccountManager providesAppAccountManager(Context context,
                                                              AppPreferences appPreferences,
                                                              CurrentUserInteractor currentUserInteractor,
                                                              Logger logger) {
        return new DefaultAppAccountManagerImpl(
                context, appPreferences, currentUserInteractor, authority, accountType, logger);
    }

    @Provides
    @PerUser
    @Override
    public DefaultNotificationHandler providesNotificationHandler(Context context) {
        return new DefaultNotificationHandlerImpl(context);
    }

    @Provides
    @PerUser
    @Override
    public HomePresenter providesHomePresenter(
            CurrentUserInteractor currentUserInteractor, SyncDateWrapper syncDateWrapper, Logger logger) {
        return new HomePresenterImpl(currentUserInteractor, syncDateWrapper, logger);
    }

    @Provides
    @Override
    @PerUser
    public ProfilePresenter providesProfilePresenter(CurrentUserInteractor currentUserInteractor,
                                                     SyncDateWrapper syncDateWrapper,
                                                     DefaultAppAccountManager appAccountManager,
                                                     DefaultNotificationHandler defaultNotificationHandler,
                                                     Logger logger) {
        return new ProfilePresenterImpl(currentUserInteractor, syncDateWrapper, appAccountManager,
                defaultNotificationHandler, logger);
    }

    @Provides
    @PerUser
    public SyncWrapper provideSyncWrapper(
            @Nullable UserOrganisationUnitInteractor userOrganisationUnitInteractor,
            @Nullable UserProgramInteractor userProgramInteractor,
            @Nullable EventInteractor eventInteractor,
            @Nullable SyncDateWrapper syncDateWrapper) {

        return new SyncWrapper(userOrganisationUnitInteractor, userProgramInteractor, eventInteractor, syncDateWrapper);
    }

    @Provides
    @PerUser
    public SelectorPresenter providesSelectorPresenter(
            @Nullable UserOrganisationUnitInteractor userOrganisationUnitInteractor,
            @Nullable UserProgramInteractor userProgramInteractor,
            @Nullable ProgramInteractor programInteractor,
            @Nullable TrackedEntityInstanceInteractor trackedEntityInstanceInteractor,
            @Nullable EnrollmentInteractor enrollmentInteractor,
            @Nullable ProgramStageInteractor programStageInteractor,
            @Nullable ProgramStageDataElementInteractor programStageDataElementInteractor,
            @Nullable EventInteractor eventInteractor,
            SessionPreferences sessionPreferences,
            SyncDateWrapper syncDateWrapper, SyncWrapper syncWrapper,
            ApiExceptionHandler apiExceptionHandler, Logger logger) {
        return new SelectorPresenterImpl(
                userOrganisationUnitInteractor, userProgramInteractor,
                programInteractor, programStageInteractor, programStageDataElementInteractor,
                enrollmentInteractor, trackedEntityInstanceInteractor, eventInteractor, sessionPreferences, syncDateWrapper, syncWrapper,
                apiExceptionHandler, logger);
    }
}