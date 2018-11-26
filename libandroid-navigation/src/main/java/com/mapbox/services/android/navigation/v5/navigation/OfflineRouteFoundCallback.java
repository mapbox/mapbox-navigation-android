package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

/**
 * Callback used for getting offline routes
 */
public interface OfflineRouteFoundCallback {

  /**
   * Called when offline route is found.
   *
   * @param directionsRoute offline route
   */
  void onOfflineRouteFound(DirectionsRoute directionsRoute);
}
