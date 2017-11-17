package com.mapbox.services.android.navigation.v5.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.METERS_REMAINING_TILL_ARRIVAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_DEPART;

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

  /**
   * Looks at the current {@link RouteProgress} maneuverType for type "arrival", then
   * checks if the arrival meter threshold has been hit.
   *
   * @param routeProgress the current route progress
   * @return true if in arrival state, false if not
   * @since 0.8.0
   */
  public static boolean isArrivalEvent(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().currentStep().maneuver() != null
      && routeProgress.currentLegProgress().currentStep().maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE)
      && routeProgress.distanceRemaining() <= METERS_REMAINING_TILL_ARRIVAL;
  }

  /**
   * Looks at the current {@link RouteProgress} maneuverType for type "departure".
   *
   * @param routeProgress the current route progress
   * @return true if in departure state, false if not
   * @since 0.8.0
   */
  public static boolean isDepartureEvent(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().currentStep().maneuver() != null
      && routeProgress.currentLegProgress().currentStep().maneuver().type().contains(STEP_MANEUVER_TYPE_DEPART);
  }
}
