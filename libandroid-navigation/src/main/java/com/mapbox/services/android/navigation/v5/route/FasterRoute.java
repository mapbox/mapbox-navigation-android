package com.mapbox.services.android.navigation.v5.route;

import android.location.Location;

public abstract class FasterRoute {

  public abstract boolean shouldCheckFasterRoute(Location location);
}
