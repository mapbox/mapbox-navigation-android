package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;

import java.util.Date;

class MapboxNavigator {

  private final Navigator navigator;

  MapboxNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  synchronized void updateRoute(String routeJson) {
    navigator.setDirections(routeJson);
  }

  synchronized NavigationStatus retrieveStatus(Date date) {
    return navigator.getStatus(date);
  }

  void updateLocation(Location location) {
    FixLocation fixedLocation = buildFixLocationFrom(location);
    synchronized (this) {
      navigator.updateLocation(fixedLocation);
    }
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
