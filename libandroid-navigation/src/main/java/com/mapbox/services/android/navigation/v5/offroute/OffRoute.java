package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public abstract class OffRoute {

  public abstract boolean isUserOffRoute(Location location, RouteProgress routeProgress,
                                         MapboxNavigationOptions options);
}
