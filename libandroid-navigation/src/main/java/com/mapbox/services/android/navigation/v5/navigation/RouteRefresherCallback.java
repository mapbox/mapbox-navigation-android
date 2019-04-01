package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

import java.util.Date;

import timber.log.Timber;

class RouteRefresherCallback implements RefreshCallback {
  private final MapboxNavigation mapboxNavigation;
  private final RouteRefresher routeRefresher;

  RouteRefresherCallback(MapboxNavigation mapboxNavigation, RouteRefresher routeRefresher) {
    this.mapboxNavigation = mapboxNavigation;
    this.routeRefresher = routeRefresher;
  }

  @Override
  public void onRefresh(DirectionsRoute directionsRoute) {
    mapboxNavigation.startNavigation(directionsRoute, DirectionsRouteType.FRESH_ROUTE);
    routeRefresher.updateLastRefresh(new Date());
    routeRefresher.updateIsChecking(false);
  }

  @Override
  public void onError(RefreshError error) {
    Timber.w(error.getMessage());
    routeRefresher.updateIsChecking(false);
  }
}
