package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

class EnhancedLocationUpdater {

    private final LocationEngine locationEngine;
    private final LocationEngineRequest locationEngineRequest;
    private final EnhancedLocationMediator mediator;
    private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
    private ScheduledFuture future;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Location rawLocation;
    private Location enhancedLocation;
    private boolean isActive = false;

    EnhancedLocationUpdater(LocationEngine locationEngine, LocationEngineRequest locationEngineRequest, EnhancedLocationMediator mediator) {
        this.locationEngine = locationEngine;
        this.locationEngineRequest = locationEngineRequest;
        this.mediator = mediator;
    }

    @SuppressLint("MissingPermission")
    void requestLocationUpdates() {
        isActive = true;
        locationEngine.requestLocationUpdates(locationEngineRequest, callback, null);
        final Handler handler = new Handler(Looper.getMainLooper());
        future = executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (rawLocation != null) {
                    enhancedLocation = mediator.getLocation(new Date(), 1500, rawLocation);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(LocationEngineResult.create(enhancedLocation));
                        }
                    });
                }
            }
        }, 1500, 1000, TimeUnit.MILLISECONDS);
    }

    void removeLocationUpdates() {
        isActive = false;
        locationEngine.removeLocationUpdates(callback);
        future.cancel(false);
    }

    boolean isActive() {
        return isActive;
    }

    private void onLocationChanged(Location location) {
        if (location != null) {
            rawLocation = location;
            mediator.updateRawLocation(location);
        }
    }

    static class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<EnhancedLocationUpdater> updaterWeakReference;

        CurrentLocationEngineCallback(EnhancedLocationUpdater locationUpdater) {
            this.updaterWeakReference = new WeakReference<>(locationUpdater);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            EnhancedLocationUpdater locationUpdater = updaterWeakReference.get();
            if (locationUpdater != null) {
                Location location = result.getLastLocation();
                locationUpdater.onLocationChanged(location);
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            Timber.e(exception);
        }
    }
}
