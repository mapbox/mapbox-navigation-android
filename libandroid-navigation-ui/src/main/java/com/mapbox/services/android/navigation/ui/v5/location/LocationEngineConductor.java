package com.mapbox.services.android.navigation.ui.v5.location;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;

public class LocationEngineConductor {

  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 500;

  private final NavigationViewLocationEngineCallback callback;
  private LocationEngine locationEngine;

  public LocationEngineConductor(LocationEngineConductorListener listener) {
    callback = new NavigationViewLocationEngineCallback(listener);
  }

  public void onCreate() {
    activateLocationEngine();
  }

  public void onDestroy() {
    deactivateLocationEngine();
  }

  public void initializeLocationEngine(Context context, LocationEngine locationEngine, boolean shouldReplayRoute) {
    initialize(context, locationEngine, shouldReplayRoute);
  }

  public void updateSimulatedRoute(DirectionsRoute route) {
    if (locationEngine instanceof ReplayRouteLocationEngine) {
      ((ReplayRouteLocationEngine) locationEngine).assign(route);
    }
  }

  public LocationEngine obtainLocationEngine() {
    return locationEngine;
  }

  @SuppressWarnings("MissingPermission")
  private void initialize(Context context, LocationEngine locationEngine, boolean simulateRoute) {
    if (locationEngine != null) {
      this.locationEngine = locationEngine;
    } else if (simulateRoute) {
      this.locationEngine = new ReplayRouteLocationEngine();
    } else {
      this.locationEngine = LocationEngineProvider.getBestLocationEngine(context);
    }
    updateLastLocation();
    activateLocationEngine();
  }

  @SuppressWarnings("MissingPermission")
  private void updateLastLocation() {
    if (isValidLocationEngine()) {
      locationEngine.getLastLocation(callback);
    }
  }

  @SuppressWarnings("MissingPermission")
  private void activateLocationEngine() {
    if (isValidLocationEngine()) {
      LocationEngineRequest request = buildEngineRequest();
      locationEngine.requestLocationUpdates(request, callback, null);
    }
  }

  private void deactivateLocationEngine() {
    if (isValidLocationEngine()) {
      locationEngine.removeLocationUpdates(callback);
    }
  }

  private boolean isValidLocationEngine() {
    return locationEngine != null;
  }

  @NonNull
  private LocationEngineRequest buildEngineRequest() {
    return new LocationEngineRequest.Builder(UPDATE_INTERVAL_IN_MILLISECONDS)
      .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
      .setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
      .build();
  }
}
