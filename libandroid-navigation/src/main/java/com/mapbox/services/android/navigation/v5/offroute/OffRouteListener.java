package com.mapbox.services.android.navigation.v5.offroute;

import android.location.Location;

public interface OffRouteListener {
  void userOffRoute(Location location);
}
