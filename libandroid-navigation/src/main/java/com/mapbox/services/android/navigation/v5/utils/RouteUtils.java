package com.mapbox.services.android.navigation.v5.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

public final class RouteUtils {

  private RouteUtils() {
    // Utils class therefore, shouldn't be initialized.
  }

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param routeProgress         the route progress with the directions route included
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public static boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull RouteProgress routeProgress) {
    return isNewRoute(previousRouteProgress, routeProgress.directionsRoute());
  }

  /**
   * Compares a new routeProgress geometry to a previousRouteProgress geometry to determine if the
   * user is traversing along a new route. If the route geometries do not match, this returns true.
   *
   * @param previousRouteProgress the past route progress with the directions route included
   * @param directionsRoute       the current directions route
   * @return true if the direction route geometries do not match up, otherwise, false
   * @since 0.7.0
   */
  public static boolean isNewRoute(@Nullable RouteProgress previousRouteProgress,
                                   @NonNull DirectionsRoute directionsRoute) {
    return previousRouteProgress == null || !previousRouteProgress.directionsRoute().geometry()
      .equals(directionsRoute.geometry());
  }
}
