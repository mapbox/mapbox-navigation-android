package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.RouteState;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public class OffRouteDetector extends OffRoute {

  @Override
  public boolean isUserOffRoute(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    // No impl
    return false;
  }

  public boolean isUserOffRouteWith(NavigationStatus status) {
    return status.getRouteState() == RouteState.OFFROUTE;
  }
}
