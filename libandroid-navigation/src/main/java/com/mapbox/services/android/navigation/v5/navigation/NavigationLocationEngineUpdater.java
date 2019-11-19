package com.mapbox.services.android.navigation.v5.navigation;

import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.utils.RouteUtils;

class NavigationLocationEngineUpdater {

  private final NavigationLocationEngine listener;
  private RouteUtils routeUtils;
  private LocationEngine locationEngine;

  NavigationLocationEngineUpdater(LocationEngine locationEngine, NavigationLocationEngine listener) {
    this.locationEngine = locationEngine;
    this.listener = listener;
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    this.locationEngine = locationEngine;
    listener.setLocationEngine(locationEngine);
  }

  @SuppressWarnings("MissingPermission")
  void forceLocationUpdate(DirectionsRoute route) {
    Location location = listener.getLastLocation();
    if (!listener.isValidLocationUpdate(location)) {
      routeUtils = obtainRouteUtils();
      location = routeUtils.createFirstLocationFromRoute(route);
    }
    listener.queueLocationUpdate(location);
  }

  void removeLocationEngineListener() {
    listener.onRemove();
  }

  private RouteUtils obtainRouteUtils() {
    if (routeUtils == null) {
      return new RouteUtils();
    }
    return routeUtils;
  }
}
