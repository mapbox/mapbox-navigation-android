package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

class NavigationLocationEngineUpdater {

  private final NavigationLocationEngineListener listener;
  private RouteUtils routeUtils;
  private LocationEngine locationEngine;

  NavigationLocationEngineUpdater(LocationEngine locationEngine, NavigationLocationEngineListener listener) {
    this.locationEngine = locationEngine;
    this.listener = listener;
    locationEngine.addLocationEngineListener(listener);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
    locationEngine.addLocationEngineListener(listener);
  }

  @SuppressWarnings("MissingPermission")
  void forceLocationUpdate(DirectionsRoute route) {
    Location location = locationEngine.getLastLocation();
    if (!listener.isValidLocationUpdate(location)) {
      routeUtils = obtainRouteUtils();
      location = routeUtils.createFirstLocationFromRoute(route);
    }
    listener.queueLocationUpdate(location);
  }

  void removeLocationEngineListener() {
    locationEngine.removeLocationEngineListener(listener);
  }

  private RouteUtils obtainRouteUtils() {
    if (routeUtils == null) {
      return new RouteUtils();
    }
    return routeUtils;
  }
}
