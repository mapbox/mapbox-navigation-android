package com.mapbox.navigation.ui.route;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

/**
 * Listener for determining which current route the user has selected as their primary route for
 * navigation.
 */
public interface OnRouteSelectionChangeListener {

  /**
   * Callback when the user selects a different route.
   *
   * @param directionsRoute the route which the user has currently selected
   */
  void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute);
}
