package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

/**
 * Used with {@link MapboxNavigation#startNavigation(DirectionsRoute, DirectionsRouteType)}.
 */
public enum DirectionsRouteType {

  /**
   * This value means {@link MapboxNavigation} will consider the entire route with
   * {@link MapboxNavigation#startNavigation(DirectionsRoute, DirectionsRouteType)}.  This value
   * should be used in off-route scenarios or any other scenario when you need a completely new route object.
   * <p>
   * Please note, this is the default value for {@link MapboxNavigation#startNavigation(DirectionsRoute)}.
   */
  NEW_ROUTE,

  /**
   * This value means {@link MapboxNavigation} will only consider annotation data with
   * {@link MapboxNavigation#startNavigation(DirectionsRoute, DirectionsRouteType)}.  This value can be used with
   * {@link RouteRefresh} to provide up-to-date ETAs and congestion data while navigating.
   */
  FRESH_ROUTE
}
