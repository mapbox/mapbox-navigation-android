package com.mapbox.services.android.navigation.ui.v5;

import android.content.Context;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;

class LocationEngineConductor {

  private LocationEngine locationEngine;

  void initializeLocationEngine(Context context, LocationEngine locationEngine, boolean shouldReplayRoute) {
    initialize(context, locationEngine, shouldReplayRoute);
  }

  boolean updateSimulatedRoute(DirectionsRoute route) {
    if (locationEngine instanceof ReplayRouteLocationEngine) {
      ((ReplayRouteLocationEngine) locationEngine).assign(route);
      return true;
    }
    return false;
  }

  LocationEngine obtainLocationEngine() {
    return locationEngine;
  }

  @SuppressWarnings("MissingPermission")
  private void initialize(Context context, LocationEngine locationEngine, boolean simulateRoute) {
    if (locationEngine != null) {
      this.locationEngine = locationEngine;
    } else if (simulateRoute) {
      this.locationEngine = new ReplayRouteLocationEngine();
    } else {
      this.locationEngine = LocationEngineProvider.getBestLocationEngine(context);
    }
  }
}
