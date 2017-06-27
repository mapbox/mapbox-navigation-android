package com.mapbox.services.android.navigation.v5.snap;

import android.location.Location;

public abstract class Snap {

  public Snap() {
  }

  public abstract Location getSnappedLocation(Location location);

  public boolean validLocationToSnap(Location location) {
    // If users not moving, don't snap their position or bearing
    return location.getSpeed() > 0d;
  }
}
