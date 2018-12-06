package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.location.Location;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

import timber.log.Timber;

class LocationUpdater implements LocationEngineCallback<LocationEngineResult> {

  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;
  private final RouteProcessorBackgroundThread thread;
  private LocationEngine locationEngine;

  @SuppressLint("MissingPermission")
  LocationUpdater(RouteProcessorBackgroundThread thread, LocationEngine locationEngine) {
    this.thread = thread;
    this.locationEngine = locationEngine;

    LocationEngineRequest request = buildEngineRequest();
    locationEngine.getLastLocation(this);
    locationEngine.requestLocationUpdates(request, this, thread.getLooper());
  }

  @Override
  public void onSuccess(LocationEngineResult result) {
    Location location = result.getLastLocation();
    onLocationChanged(location);
  }

  @Override
  public void onFailure(@NonNull Exception exception) {
    Timber.e(exception);
  }

  void removeLocationUpdates() {
    locationEngine.removeLocationUpdates(this);
  }

  @SuppressLint("MissingPermission")
  void updateLocationEngine(LocationEngine locationEngine) {
    this.locationEngine.removeLocationUpdates(this);
    LocationEngineRequest request = buildEngineRequest();
    locationEngine.requestLocationUpdates(request, this, thread.getLooper());
    this.locationEngine = locationEngine;
  }

  @NonNull
  private LocationEngineRequest buildEngineRequest() {
    return new LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
      .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
      .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
      .build();
  }

  private void onLocationChanged(Location location) {
    if (location != null) {
      thread.updateLocation(location);
      NavigationTelemetry.getInstance().updateLocation(location);
    }
  }
}