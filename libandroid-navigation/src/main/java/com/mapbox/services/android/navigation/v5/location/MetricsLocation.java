package com.mapbox.services.android.navigation.v5.location;

import android.location.Location;

public class MetricsLocation {
  private Location location;

  public MetricsLocation(Location location) {
    this.location = location;
  }

  public Location getLocation() {
    if (location != null) {
      return location;
    }

    Location metricLocation = new Location("MetricsLocation");
    metricLocation.setLatitude(0.0);
    metricLocation.setLongitude(0.0);

    return metricLocation;
  }
}
