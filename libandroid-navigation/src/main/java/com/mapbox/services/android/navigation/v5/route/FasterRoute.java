package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public abstract class FasterRoute {

  public abstract boolean shouldCheckFasterRoute(Location location, RouteProgress routeProgress);
}
