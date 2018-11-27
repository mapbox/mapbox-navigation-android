package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

import java.util.List;

/**
 * Callback used for getting routes
 */
public interface RouteFoundCallback {

  /**
   * Called when route is found.
   *
   * @param routes offline route
   */
  void routesFound(List<DirectionsRoute> routes);

  void onError(OfflineData offlineData);
}
