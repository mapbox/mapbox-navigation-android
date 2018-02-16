package com.mapbox.services.android.navigation.v5.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.RouteLeg;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.METERS_REMAINING_TILL_ARRIVAL;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;

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
    return (upcomingStepIsArrival(routeProgress) || currentStepIsArrival(routeProgress))
      && routeProgress.currentLegProgress().distanceRemaining() <= METERS_REMAINING_TILL_ARRIVAL;
  }

  /**
   * Looks at the current {@link RouteProgress} list of legs and
   * checks if the current leg is the last leg.
   *
   * @param routeProgress the current route progress
   * @return true if last leg, false if not
   * @since 0.8.0
   */
  public static boolean isLastLeg(RouteProgress routeProgress) {
    List<RouteLeg> legs = routeProgress.directionsRoute().legs();
    RouteLeg currentLeg = routeProgress.currentLeg();
    return currentLeg.equals(legs.get(legs.size() - 1));
  }

  /**
   * Given a {@link RouteProgress}, this method will calculate the remaining coordinates
   * along the given route based on total route coordinates and the progress remaining waypoints.
   * <p>
   * If the coordinate size is less than the remaining waypoints, this method
   * will return null.
   *
   * @param routeProgress for route coordinates and remaining waypoints
   * @return list of remaining waypoints as {@link Point}s
   * @since 0.10.0
   */
  @Nullable
  public static List<Point> calculateRemainingWaypoints(RouteProgress routeProgress) {
    if (routeProgress.directionsRoute().routeOptions() == null) {
      return null;
    }

    List<Point> coordinates = new ArrayList<>(routeProgress.directionsRoute().routeOptions().coordinates());

    if (coordinates.size() < routeProgress.remainingWaypoints()) {
      return null;
    }
    coordinates.subList(0, routeProgress.remainingWaypoints()).clear();
    return coordinates;
  }

  private static boolean upcomingStepIsArrival(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().upComingStep() != null
      && routeProgress.currentLegProgress().upComingStep().maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE);
  }

  private static boolean currentStepIsArrival(@NonNull RouteProgress routeProgress) {
    return routeProgress.currentLegProgress().currentStep().maneuver().type().contains(STEP_MANEUVER_TYPE_ARRIVE);
  }
}
