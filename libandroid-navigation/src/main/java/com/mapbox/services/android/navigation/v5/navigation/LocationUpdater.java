package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

import java.lang.ref.WeakReference;

import timber.log.Timber;

class LocationUpdater {

  private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
  private final RouteProcessorBackgroundThread thread;
  private final NavigationEventDispatcher dispatcher;
  private LocationEngine locationEngine;
  private LocationEngineRequest request;

  @SuppressLint("MissingPermission")
  LocationUpdater(@NonNull RouteProcessorBackgroundThread thread, @NonNull NavigationEventDispatcher dispatcher,
                  @NonNull LocationEngine locationEngine, @NonNull LocationEngineRequest request) {
    this.thread = thread;
    this.dispatcher = dispatcher;
    this.locationEngine = locationEngine;
    this.request = request;
    requestInitialLocationUpdates(locationEngine, request);
  }

  void updateLocationEngine(@NonNull LocationEngine locationEngine) {
    requestLocationUpdates(request, locationEngine);
    this.locationEngine = locationEngine;
  }

  void updateLocationEngineRequest(@NonNull LocationEngineRequest request) {
    requestLocationUpdates(request, locationEngine);
    this.request = request;
  }

  void onLocationChanged(@Nullable Location location) {
    if (location != null) {
      thread.updateLocation(location);
      dispatcher.onLocationUpdate(location);
      NavigationTelemetry.getInstance().updateLocation(location);
    }
  }

  void removeLocationUpdates() {
    locationEngine.removeLocationUpdates(callback);
  }

  @SuppressLint("MissingPermission")
  private void requestInitialLocationUpdates(@NonNull LocationEngine locationEngine, @NonNull LocationEngineRequest request) {
    locationEngine.requestLocationUpdates(request, callback, null);
  }

  @SuppressLint("MissingPermission")
  private void requestLocationUpdates(@NonNull LocationEngineRequest request, @NonNull LocationEngine locationEngine) {
    this.locationEngine.removeLocationUpdates(callback);
    locationEngine.requestLocationUpdates(request, callback, null);
  }

  static class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private final WeakReference<LocationUpdater> updaterWeakReference;

    CurrentLocationEngineCallback(LocationUpdater locationUpdater) {
      this.updaterWeakReference = new WeakReference<>(locationUpdater);
    }

    @Override
    public void onSuccess(@NonNull LocationEngineResult result) {
      LocationUpdater locationUpdater = updaterWeakReference.get();
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