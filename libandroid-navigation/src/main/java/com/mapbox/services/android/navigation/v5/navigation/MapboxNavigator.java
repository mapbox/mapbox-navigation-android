package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

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

  synchronized NavigationStatus retrieveStatus(Date date, long lagInMilliseconds) {
    // We ask for a point slightly in the future to account for lag in location services
    if (lagInMilliseconds > 0) {
      date.setTime(date.getTime() + lagInMilliseconds);
    }
    return navigator.getStatus(date);
  }

  void updateLocation(Location raw) {
    FixLocation fixedLocation = buildFixLocationFromLocation(raw);
    synchronized (this) {
      navigator.updateLocation(fixedLocation);
    }
  }

  // TODO this call should be done in the background - it's currently blocking the UI
  synchronized void configureRouter(String tileFilePath, String translationsDirPath) {
    navigator.configureRouter(tileFilePath, translationsDirPath);
  }

  /**
   * Uses libvalhalla and local tile data to generate mapbox-directions-api-like json
   *
   * @param directionsUri the uri used when hitting the http service
   * @return a RouterResult object with the json and a success/fail bool
   */
  synchronized RouterResult retrieveRouteFor(String directionsUri) {
    return navigator.getRoute(directionsUri);
  }

  /**
   * Gets the history of state changing calls to the navigator this can be used to
   * replay a sequence of events for the purpose of bug fixing.
   *
   * @return a json representing the series of events that happened since the last time
   * history was toggled on
   */
  synchronized String retrieveHistory() {
    return navigator.getHistory();
  }

  /**
   * Toggles the recording of history on or off.
   *
   * @param isEnabled set this to true to turn on history recording and false to turn it off
   *                  toggling will reset all history call getHistory first before toggling
   *                  to retain a copy
   */
  synchronized void toggleHistory(boolean isEnabled) {
    navigator.toggleHistory(isEnabled);
  }

  FixLocation buildFixLocationFromLocation(Location location) {
    Date time = new Date();
    Point rawPoint = Point.fromLngLat(location.getLongitude(), location.getLatitude());
    Float speed = checkFor(location.getSpeed());
    Float bearing = checkFor(location.getBearing());
    Float altitude = checkFor((float) location.getAltitude());
    Float horizontalAccuracy = checkFor(location.getAccuracy());
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

  private Float checkFor(Float value) {
    if (value == 0.0) {
      return null;
    }
    return value;
  }
}
