package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

public class FasterRouteDetector extends FasterRoute {

  private Location lastLocation;

  @Override
  public boolean shouldCheckFasterRoute(Location location) {
    if (lastLocation == null) {
      return false;
    }
    return false;
  }
}
