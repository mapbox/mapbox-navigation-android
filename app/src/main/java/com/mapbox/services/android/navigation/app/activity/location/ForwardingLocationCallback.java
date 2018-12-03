package com.mapbox.services.android.navigation.app.activity.location;

import android.location.Location;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.util.List;

class ForwardingLocationCallback extends LocationCallback {

  private static final int FIRST = 0;
  private final FusedLocationEngine locationEngine;

  ForwardingLocationCallback(FusedLocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  @Override
  public void onLocationResult(LocationResult locationResult) {
    List<Location> locations = locationResult.getLocations();
    boolean hasLocation = !locations.isEmpty();
    if (hasLocation) {
      Location newLocation = locations.get(FIRST);
      locationEngine.notifyListenersOnLocationChanged(newLocation);
    }
  }
}
