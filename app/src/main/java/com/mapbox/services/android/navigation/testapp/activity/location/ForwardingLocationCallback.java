package com.mapbox.services.android.navigation.testapp.activity.location;

import android.location.Location;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

class ForwardingLocationCallback extends LocationCallback {

  private final FusedLocationEngine locationEngine;

  ForwardingLocationCallback(FusedLocationEngine locationEngine) {
    this.locationEngine = locationEngine;
  }

  @Override
  public void onLocationResult(LocationResult locationResult) {
    Location location = locationResult.getLastLocation();
    locationEngine.notifyListenersOnLocationChanged(location);
  }
}
