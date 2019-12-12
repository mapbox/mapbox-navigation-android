package com.mapbox.navigation.ui.route;

import com.mapbox.navigation.base.route.model.Route;

/**
 * Listener for determining which current route the user has selected as their primary route for
 * navigation.
 *
 * @since 0.8.0
 */
public interface OnRouteSelectionChangeListener {

  /**
   * Callback when the user selects a different route.
   *
   * @param route the route which the user has currently selected
   * @since 0.8.0
   */
  void onNewPrimaryRouteSelected(Route route);
}
