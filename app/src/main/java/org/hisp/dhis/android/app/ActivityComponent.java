package org.hisp.dhis.android.app;

import org.hisp.dhis.android.app.views.DataEntryFragment;
import org.hisp.dhis.android.app.views.EnrollmentFormActivity;

import dagger.Subcomponent;

@PerActivity
@Subcomponent(
        modules = {
                ActivityModule.class
        }
)
public interface ActivityComponent {

    //------------------------------------------------------------------------
    // Injection targets
    //------------------------------------------------------------------------

    void inject(EnrollmentFormActivity enrollmentFormActivity);

    void inject(DataEntryFragment dataEntryFragment);
}
