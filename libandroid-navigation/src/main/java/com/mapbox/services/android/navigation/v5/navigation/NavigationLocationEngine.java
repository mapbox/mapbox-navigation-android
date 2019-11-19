package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

class NavigationLocationEngine {

    private final RouteProcessorBackgroundThread thread;
    private final LocationValidator validator;
    private LocationEngine locationEngine;
    private MapboxNavigation mapboxNavigation;
    private Location lastLocation;
    LocationEngineCallback<LocationEngineResult> callback = new LocationEngineCallback<LocationEngineResult>() {
        @Override
        public void onSuccess(LocationEngineResult result) {
            if (isValidLocationUpdate(result.getLastLocation())) {
                queueLocationUpdate(result.getLastLocation());
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {}
    };

    NavigationLocationEngine(RouteProcessorBackgroundThread thread, MapboxNavigation mapboxNavigation,
                             LocationEngine locationEngine, LocationValidator validator) {
        this.thread = thread;
        this.mapboxNavigation = mapboxNavigation;
        this.validator = validator;
        setLocationEngine(locationEngine);
    }

    boolean isValidLocationUpdate(Location location) {
        return location != null && validator.isValidUpdate(location);
    }

    /**
     * Queues a new task created from a location update to be sent
     * to {@link RouteProcessorBackgroundThread} for processing.
     *
     * @param location to be processed
     */
    void queueLocationUpdate(Location location) {
        lastLocation = location;
        thread.queueUpdate(NavigationLocationUpdate.create(location, mapboxNavigation));
    }

    private void onAdded() {
        if (locationEngine != null)
            locationEngine.requestLocationUpdates(new LocationEngineRequest.Builder(1000).build(), callback, Looper.getMainLooper());
    }

    void setLocationEngine(LocationEngine locationEngine) {
        onRemove();
        this.locationEngine = locationEngine;
        onAdded();
    }

    void onRemove() {
        if (locationEngine != null)
            locationEngine.removeLocationUpdates(callback);
    }

    Location getLastLocation() {
        return lastLocation;
    }
}
