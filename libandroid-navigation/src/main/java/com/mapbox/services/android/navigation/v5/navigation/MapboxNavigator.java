package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.navigator.FixLocation;
import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouterResult;

import java.util.ArrayList;
import java.util.Date;

class MapboxNavigator {

  private static final String MAPBOX_OFFLINE_NAVIGATION_PROVIDER = "mapbox_offline_navigation";
  private final Navigator navigator;

  MapboxNavigator(Navigator navigator) {
    this.navigator = navigator;
  }

  synchronized void updateRoute(String routeJson) {
    // TODO route_index (Which route to follow) and leg_index (Which leg to follow) are hardcoded for now
    navigator.setRoute(routeJson, 0, 0);
  }

  // TODO this call should be done in the background - it's currently blocking the UI
  synchronized void configureRouter(String tileFilePath, String translationsDirPath) {
    navigator.configureRouter(tileFilePath, translationsDirPath);
  }

  synchronized RouterResult retrieveRouteFor(ArrayList<FixLocation> waypoints) {
    return navigator.getRoute(waypoints);
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

  ArrayList<FixLocation> buildFixLocationListFrom(FixLocation origin, Point destination, Point[] waypoints) {
    return buildFixLocationList(origin, destination, waypoints);
  }

  FixLocation buildFixLocationFromPoint(Point point) {
    return new FixLocation(
      point,
      new Date(),
      0f, 0f, 0f, 0f,
      MAPBOX_OFFLINE_NAVIGATION_PROVIDER
    );
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

  private ArrayList<FixLocation> buildFixLocationList(FixLocation origin, Point destination, Point[] waypoints) {
    ArrayList<FixLocation> fixLocations = new ArrayList<>();
    fixLocations.add(origin);
    if (waypoints != null) {
      for (Point point : waypoints) {
        fixLocations.add(buildFixLocationFromPoint(point));
      }
    }
    fixLocations.add(buildFixLocationFromPoint(destination));
    return fixLocations;
  }
}
