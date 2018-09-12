package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

import timber.log.Timber;

class NavigationLocationEngineListener implements LocationEngineListener {

  private final RouteProcessorBackgroundThread thread;
  private final LocationEngine locationEngine;
  private final LocationValidator validator;

  NavigationLocationEngineListener(RouteProcessorBackgroundThread thread, LocationEngine locationEngine,
                                   LocationValidator validator) {
    this.thread = thread;
    this.locationEngine = locationEngine;
    this.validator = validator;
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    Timber.d("NavigationLocationEngineListener#onLocationChanged: %s", location);
    thread.updateRawLocation(location);
  }

  boolean isValidLocationUpdate(Location location) {
    return location != null && validator.isValidUpdate(location);
  }
}