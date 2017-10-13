package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

import com.mapbox.services.Experimental;

public interface OffRouteListener {
  void userOffRoute(Location location);
}
