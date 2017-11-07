package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.List;

public abstract class Snap {

  public abstract Location getSnappedLocation(Location location, RouteProgress routeProgress);

  public boolean validLocationToSnap(Location location) {
    // If user's not moving, don't snap their position or bearing
    return location.getSpeed() > 0d;
  }
}
