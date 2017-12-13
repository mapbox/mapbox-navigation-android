package com.mapbox.services.android.navigation.ui.v5;

import android.location.Location;

public interface RouteListener {
  boolean allowRerouteFrom(Location location);

  void onRerouteFrom(Location location);
}
