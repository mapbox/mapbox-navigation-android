package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.android.core.location.LocationEngine;

class NavigationLocationEngineUpdater {

  private final NavigationLocationEngineListener listener;
  private LocationEngine locationEngine;

  NavigationLocationEngineUpdater(LocationEngine locationEngine, NavigationLocationEngineListener listener) {
    this.locationEngine = locationEngine;
    this.listener = listener;
    locationEngine.addLocationEngineListener(listener);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    removeLocationEngineListener();
    this.locationEngine = locationEngine;
    locationEngine.addLocationEngineListener(listener);
  }

  void removeLocationEngineListener() {
    locationEngine.removeLocationEngineListener(listener);
  }
}
