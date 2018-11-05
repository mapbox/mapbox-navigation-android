package com.mapbox.services.android.navigation.v5.routeprogress;

import com.mapbox.api.directions.v5.models.DirectionsRoute;

/**
 * Contains the various progress states that can occur while navigating.
 */
public enum RouteProgressState {

  /**
   * The {@link com.mapbox.api.directions.v5.models.DirectionsRoute} provided
   * via {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation#startNavigation(DirectionsRoute)}
   * is not valid.
   */
  ROUTE_INVALID,

  /**
   * The {@link DirectionsRoute} is valid
   * and {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation} is waiting for
   * sufficient {@link android.location.Location} updates
   * from the {@link com.mapbox.android.core.location.LocationEngine}.
   */
  ROUTE_INITIALIZED,

  /**
   * The user has arrived at the destination of the given {@link com.mapbox.api.directions.v5.models.RouteLeg}.
   */
  ROUTE_ARRIVED,

  /**
   * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation} is now confidently tracking the
   * location updates and processing them against the route.
   */
  LOCATION_TRACKING,

  /**
   * A lack of {@link android.location.Location} updates from the phone has caused lack of confidence in the
   * progress updates being sent.
   */
  LOCATION_STALE
}
