package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.navigator.NavigationStatus;
import com.mapbox.navigator.Navigator;
import com.mapbox.navigator.RouteState;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.Date;

public class OffRouteDetector extends OffRoute {

  private final Navigator navigator;

  public OffRouteDetector(Navigator navigator) {
    this.navigator = navigator;
  }

  @Override
  public boolean isUserOffRoute(Location location, RouteProgress routeProgress, MapboxNavigationOptions options) {
    return determineIsUserOffRoute(location);
  }

  private boolean determineIsUserOffRoute(Location location) {
    Date locationDate = new Date(location.getTime());
    NavigationStatus status = navigator.getStatus(locationDate);
    return status.getRouteState() == RouteState.OFFROUTE;
  }
}
