package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Date;

class RouteRefresher {
  private final MapboxNavigation mapboxNavigation;
  private final RouteRefresh routeRefresh;
  private final long refreshIntervalInMilliseconds;
  private Date lastRefreshedDate;
  private boolean isChecking;
  private boolean isRefreshRouteEnabled;

  RouteRefresher(MapboxNavigation mapboxNavigation, RouteRefresh routeRefresh) {
    this.mapboxNavigation = mapboxNavigation;
    this.routeRefresh = routeRefresh;
    this.refreshIntervalInMilliseconds = mapboxNavigation.options().refreshIntervalInMilliseconds();
    this.lastRefreshedDate = new Date();
    this.isRefreshRouteEnabled = mapboxNavigation.options().enableRefreshRoute();
  }

  boolean check(Date currentDate) {
    if (isChecking || !isRefreshRouteEnabled) {
      return false;
    }
    long millisSinceLastRefresh = currentDate.getTime() - lastRefreshedDate.getTime();
    return millisSinceLastRefresh > refreshIntervalInMilliseconds;
  }

  void refresh(RouteProgress routeProgress) {
    updateIsChecking(true);
    routeRefresh.refresh(routeProgress, new RouteRefresherCallback(mapboxNavigation, this));
  }

  void updateLastRefresh(Date date) {
    lastRefreshedDate = date;
  }

  void updateIsChecking(boolean isChecking) {
    this.isChecking = isChecking;
  }
}
