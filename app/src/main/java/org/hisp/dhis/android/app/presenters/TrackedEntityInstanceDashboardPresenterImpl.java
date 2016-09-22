package org.hisp.dhis.android.app.presenters;

import org.hisp.dhis.android.app.views.TrackedEntityInstanceDashboardView;
import org.hisp.dhis.client.sdk.android.enrollment.EnrollmentInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityAttributeValueInteractor;
import org.hisp.dhis.client.sdk.android.trackedentity.TrackedEntityInstanceInteractor;
import org.hisp.dhis.client.sdk.models.enrollment.Enrollment;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityAttributeValue;
import org.hisp.dhis.client.sdk.models.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.client.sdk.ui.bindings.views.View;
import org.hisp.dhis.client.sdk.ui.models.FormEntity;
import org.hisp.dhis.client.sdk.ui.models.FormEntityEditText;
import org.hisp.dhis.client.sdk.ui.models.FormEntityText;
import org.hisp.dhis.client.sdk.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static org.hisp.dhis.client.sdk.utils.Preconditions.isNull;

public class TrackedEntityInstanceDashboardPresenterImpl implements TrackedEntityInstanceDashboardPresenter {
    TrackedEntityInstanceDashboardView trackedEntityInstanceDashboardView;
    private final String TAG = TrackedEntityInstanceDashboardPresenterImpl.class.getSimpleName();
    private CompositeSubscription subscription;
    private final EnrollmentInteractor enrollmentInteractor;
    private final TrackedEntityInstanceInteractor trackedEntityInstanceInteractor;
    private final TrackedEntityAttributeValueInteractor trackedEntityAttributeValueInteractor;
    private final Logger logger;

    public TrackedEntityInstanceDashboardPresenterImpl(EnrollmentInteractor enrollmentInteractor,
                                                       TrackedEntityInstanceInteractor trackedEntityInstanceInteractor,
                                                       TrackedEntityAttributeValueInteractor trackedEntityAttributeValueInteractor,
                                                       Logger logger) {

        this.enrollmentInteractor = enrollmentInteractor;
        this.trackedEntityInstanceInteractor = trackedEntityInstanceInteractor;
        this.logger = logger;
        this.trackedEntityAttributeValueInteractor = trackedEntityAttributeValueInteractor;
    }

    @Override
    public void attachView(View view) {
        isNull(view, "View must not be null");
        trackedEntityInstanceDashboardView = (TrackedEntityInstanceDashboardView) view;
    }

    @Override
    public void detachView() {
        trackedEntityInstanceDashboardView = null;

        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    @Override
    public void createDashboard(String enrollmentUid) {
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
            subscription = null;
        }
        subscription = new CompositeSubscription();

        subscription.add(enrollmentInteractor.get(enrollmentUid)
                .map(new Func1<Enrollment, List<TrackedEntityAttributeValue>>() {
                    @Override
                    public List<TrackedEntityAttributeValue> call(Enrollment enrollment) {
                        return trackedEntityAttributeValueInteractor.list(enrollment).toBlocking().first();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<TrackedEntityAttributeValue>>() {
                    @Override
                    public void call(List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {
                        isNull(trackedEntityAttributeValues, String.format(
                                "TrackedEntityAttributes with enrollment does not exist"));


                        List<FormEntity> formEntities = transformTrackedEntityAttributeValues(trackedEntityAttributeValues);

                        if (trackedEntityInstanceDashboardView != null) {
//                            trackedEntityInstanceDashboardView.setTextToDummyTextView(builder.toString());
                            trackedEntityInstanceDashboardView.showProfileRows(formEntities);
                        }

                        // fire next operations

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logger.e(TAG, null, throwable);
                    }
                }));
    }

    private List<FormEntity> transformTrackedEntityAttributeValues(
             List<TrackedEntityAttributeValue> trackedEntityAttributeValues) {

        if(trackedEntityAttributeValues == null || trackedEntityAttributeValues.isEmpty()) {
            return new ArrayList<>();
        }

        List<FormEntity> formEntities = new ArrayList<>();

        for (TrackedEntityAttributeValue trackedEntityAttributeValue : trackedEntityAttributeValues) {
            formEntities.add(transformTrackedEntityAttribute(trackedEntityAttributeValue));
        }

        return formEntities;
    }

    private FormEntity transformTrackedEntityAttribute(TrackedEntityAttributeValue trackedEntityAttributeValue) {
        FormEntityText formEntityText = new FormEntityText(trackedEntityAttributeValue.getTrackedEntityAttributeUId(), "");
        formEntityText.setValue(trackedEntityAttributeValue.getValue());
        return formEntityText;

    }
}
