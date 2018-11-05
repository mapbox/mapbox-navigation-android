package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;

class NavigationLocationEngineListener implements LocationEngineListener {

  private final RouteProcessorBackgroundThread thread;
  private final LocationEngine locationEngine;

  NavigationLocationEngineListener(RouteProcessorBackgroundThread thread, LocationEngine locationEngine) {
    this.thread = thread;
    this.locationEngine = locationEngine;
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onConnected() {
    locationEngine.requestLocationUpdates();
    sendLastKnownLocation();
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
      thread.updateRawLocation(location);
    }
  }

  @SuppressLint("MissingPermission")
  private void sendLastKnownLocation() {
    Location lastLocation = locationEngine.getLastLocation();
    if (lastLocation != null) {
      onLocationChanged(lastLocation);
    }
  }
}