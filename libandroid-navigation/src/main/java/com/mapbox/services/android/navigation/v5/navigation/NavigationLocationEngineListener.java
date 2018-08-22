package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.Navigator;
import com.mapbox.services.android.navigation.v5.location.LocationValidator;

import java.util.Date;

class NavigationLocationEngineListener implements LocationEngineListener {

  private final RouteProcessorBackgroundThread thread;
  private final Navigator navigator;
  private final LocationEngine locationEngine;
  private final LocationValidator validator;

  NavigationLocationEngineListener(RouteProcessorBackgroundThread thread, Navigator navigator,
                                   LocationEngine locationEngine, LocationValidator validator) {
    this.thread = thread;
    this.locationEngine = locationEngine;
    this.navigator = navigator;
    this.validator = validator;
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onConnected() {
    locationEngine.requestLocationUpdates();
  }

  @Override
  public void onLocationChanged(Location location) {
    navigator.updateLocation(buildFixLocationFrom(location));
    thread.updateLocation(location);
    if (!thread.isAlive()) {
      thread.start();
    }
  }

  boolean isValidLocationUpdate(Location location) {
    return location != null && validator.isValidUpdate(location);
  }

  private FixLocation buildFixLocationFrom(Location rawLocation) {
    Point rawPoint = Point.fromLngLat(rawLocation.getLongitude(), rawLocation.getLatitude());
    Date time = new Date(rawLocation.getTime());
    Float speed = rawLocation.getSpeed();
    Float bearing = rawLocation.getBearing();
    Float altitude = (float) rawLocation.getAltitude();
    Float horizontalAccuracy = rawLocation.getAccuracy();
    String provider = rawLocation.getProvider();

    return new FixLocation(
      rawPoint,
      time,
      speed,
      bearing,
      altitude,
      horizontalAccuracy,
      provider
    );
  }
}