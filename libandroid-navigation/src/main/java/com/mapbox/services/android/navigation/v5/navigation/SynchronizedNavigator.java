package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;

import java.util.ArrayList;
import java.util.Date;

class SynchronizedNavigator {

  private final Navigator navigator;

  SynchronizedNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  Navigator retrieveNavigator() {
    return navigator;
  }

  synchronized String getRoute(ArrayList<FixLocation> waypoints) {
    return navigator.getRoute(waypoints);
  }

  synchronized NavigationStatus getStatus(Date date) {
    return navigator.getStatus(date);
  }

  synchronized void updateLocation(Location location) {
    navigator.updateLocation(buildFixLocationFrom(location));
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
