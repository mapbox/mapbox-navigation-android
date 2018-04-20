package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public abstract class Snap {

  public abstract Location getSnappedLocation(Location location, RouteProgress routeProgress);
}
