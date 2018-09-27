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
    // TODO route_index (Which route to follow) and leg_index (Which leg to follow) are hardcoded for now
    navigator.setRoute(routeJson, 0, 0);
  }

  synchronized NavigationStatus retrieveStatus(Date date) {
    return navigator.getStatus(date);
  }

  void updateLocation(Location raw) {
    FixLocation fixedLocation = buildFixLocationFromLocation(raw);
    synchronized (this) {
      navigator.updateLocation(fixedLocation);
    }
  }

  FixLocation buildFixLocationFromLocation(Location location) {
    Point rawPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Date time = new Date(location.getTime());
    Float speed = location.getSpeed();
    Float bearing = location.getBearing();
    Float altitude = (float) location.getAltitude();
    Float horizontalAccuracy = location.getAccuracy();
    String provider = location.getProvider();

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
