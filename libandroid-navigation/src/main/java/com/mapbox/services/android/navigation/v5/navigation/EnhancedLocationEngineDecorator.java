package com.mapbox.services.android.navigation.v5.navigation;

import android.app.PendingIntent;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.navigator.NavigationStatus;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

class EnhancedLocationEngineDecorator implements LocationEngine {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;
    private final LocationEngine locationEngine;
    private final EnhancedLocationMediator mediator;
    private final MapboxNavigator mapboxNavigator;
    private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
    private ScheduledFuture future;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Location rawLocation;
    private Location enhancedLocation;
    private AtomicBoolean isActive = new AtomicBoolean(false);

    EnhancedLocationEngineDecorator(LocationEngine locationEngine, final MapboxNavigator mapboxNavigator) {
        this.locationEngine = locationEngine;
        this.mapboxNavigator = mapboxNavigator;
        this.mediator = new EnhancedLocationMediator() {
            @Override
            public void updateRawLocation(Location location) {
               mapboxNavigator.updateLocation(location);
            }

            @Override
            public Location getLocation(Date date, long lagMillis, Location fallbackLocation) {
                NavigationStatus status = mapboxNavigator.retrieveStatus(date, 0);
                return isActive.get() ? mapboxNavigator.getSnappedLocation(status, fallbackLocation) : fallbackLocation;
            }
        };
    }

    void onNavigationStarted() {
        isActive.getAndSet(true);
    }

    void onNavigationStopped() {
        isActive.getAndSet(false);
    }

    private void onLocationChanged(Location location) {
        if (location != null) {
            rawLocation = location;
            mediator.updateRawLocation(location);
        }
    }

    @Override
    public void getLastLocation(@NonNull LocationEngineCallback<LocationEngineResult> callback) throws SecurityException {
        locationEngine.getLastLocation(callback);
    }

    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, @NonNull final LocationEngineCallback<LocationEngineResult> callback, @Nullable Looper looper) throws SecurityException {
        locationEngine.requestLocationUpdates(request, callback, null);
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

    @Override
    public void requestLocationUpdates(@NonNull LocationEngineRequest request, PendingIntent pendingIntent) throws SecurityException {
        locationEngine.requestLocationUpdates(request, pendingIntent);
    }

    @Override
    public void removeLocationUpdates(@NonNull LocationEngineCallback<LocationEngineResult> callback) {
        locationEngine.removeLocationUpdates(callback);
        future.cancel(false);
    }

    @Override
    public void removeLocationUpdates(PendingIntent pendingIntent) {
        locationEngine.removeLocationUpdates(pendingIntent);
    }

    void configure(File path, String version) {
        OfflineNavigator offlineNavigator = new OfflineNavigator(mapboxNavigator.getNavigator());
        String tilePath = new File(path, version).getAbsolutePath();
        offlineNavigator.configure(tilePath, new OnOfflineTilesConfiguredCallback() {
            @Override
            public void onConfigured(int numberOfTiles) {
                Timber.d("DEBUG: onConfigured %d", numberOfTiles);
                locationEngine.requestLocationUpdates(obtainLocationEngineRequest(), callback, null);
            }

            @Override
            public void onConfigurationError(@NonNull OfflineError error) {
                Timber.d("DEBUG: onConfigurationError %s", error.getMessage());
            }
        });
    }

    LocationEngineCallback<LocationEngineResult> getCallback() {
        return callback;
    }

    @NonNull
    private static LocationEngineRequest obtainLocationEngineRequest() {
        return new LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
                .build();
    }

    static class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<EnhancedLocationEngineDecorator> updaterWeakReference;

        CurrentLocationEngineCallback(EnhancedLocationEngineDecorator locationUpdater) {
            this.updaterWeakReference = new WeakReference<>(locationUpdater);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {
            EnhancedLocationEngineDecorator locationUpdater = updaterWeakReference.get();
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
