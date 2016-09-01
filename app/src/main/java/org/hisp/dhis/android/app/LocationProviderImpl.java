package org.hisp.dhis.android.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.hisp.dhis.client.sdk.utils.Logger;

import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

public class LocationProviderImpl implements LocationProvider {
    private static final String TAG = LocationProviderImpl.class.getName();
    private static final String TAG_LOCATION = LocationListener.class.getName();
    private static final int LOCATION_UPDATE_INTERVAL = 313; //millisec
    private static final int BUFFER_SIZE = 1;
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private final Context context;
    private final Logger logger;
    private final LocationListener locationListener;
    private Subject<Location, Location> locationSubject;

    public LocationProviderImpl(Context context, Logger log) {
        this.context = context;
        this.logger = log;
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                logger.d(TAG_LOCATION, "onLocationChanged: new location: " + location);
                //System.out.println("Location updated: " + location);

                locationSubject.onNext(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                logger.d(TAG_LOCATION, "onStatusChanged(): " + status);
            }

            @Override
            public void onProviderEnabled(String provider) {
                logger.d(TAG_LOCATION, "onProviderEnabled()");
            }

            @Override
            public void onProviderDisabled(String provider) {
                logger.d(TAG_LOCATION, "onProviderEnabled()");
                locationSubject.onCompleted();
            }
        };
        this.locationSubject = ReplaySubject.createWithSize(BUFFER_SIZE);
    }

    /**
     * Call this method to get the observable.
     *
     * @return locationSubject.
     */
    @Override
    public Observable<Location> locations() {
        return locationSubject;
    }

    /**
     * This method expects the client to have checked weather or not the app has
     * the following permission: Manifest.permission.ACCES_FINE_LOCATION.
     */
    @SuppressWarnings("MissingPermission")
    @Override
    public void requestLocation() {
        LocationManager locationManager = (LocationManager) context.getSystemService(
                Context.LOCATION_SERVICE);
        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationSubject.onNext(lastKnown);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LOCATION_UPDATE_INTERVAL, 0, locationListener);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    public void stopUpdates() {
        Log.d(TAG, "stopUpdates()");
        ((LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE))
                .removeUpdates(locationListener);
        locationSubject.onCompleted();
        locationSubject = ReplaySubject.createWithSize(BUFFER_SIZE);
    }

    /**
     * From the google docs:
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    public boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
