package com.mapbox.services.android.navigation.v5;

import android.location.Location;

import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.models.RouteLegProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.RouteLeg;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.commons.models.Position;

/**
 * The {@code routeProgress} class contains all progress information of user along the route, leg and step.
 * <p>
 * You can use this together with MapboxNavigation to obtain this object from the AlertLevelChangeListener
 * or the ProgressChangeListener. This object is immutable and a new, updated routeProgress object will be provided with
 * each new location update.
 * <p>
 * This is an experimental API. Experimental APIs are quickly evolving and
 * might change or be removed in minor versions.
 *
 * @since 0.1.0
 */
@Experimental
public class RouteProgress {

  private RouteLegProgress currentLegProgress;
  private Position userSnappedPosition;
  private DirectionsRoute route;
  private int currentLegIndex;
  private int alertUserLevel;
  private double routeDistance;

  /**
   * Constructor for the route routeProgress information.
   *
   * @param route            the {@link DirectionsRoute} being used for the navigation session. When a user is
   *                         rerouted this route is updated.
   * @param location         the users location most recently used when creating this object.
   * @param currentStepIndex an {@code integer} representing the current step index the user is on.
   * @param alertUserLevel   the most recently calculated alert level.
   * @since 0.1.0
   */
  public RouteProgress(DirectionsRoute route, Location location, int currentLegIndex,
                       int currentStepIndex, int alertUserLevel) {
    this.route = route;
    this.alertUserLevel = alertUserLevel;
    this.currentLegIndex = currentLegIndex;
    userSnappedPosition = RouteUtils.getSnapToRoute(
      Position.fromCoordinates(location.getLongitude(), location.getLatitude()),
      route.getLegs().get(currentLegIndex),
      currentStepIndex
    );
    currentLegProgress = new RouteLegProgress(getCurrentLeg(), currentStepIndex, userSnappedPosition);

    // Measure route from beginning to end. This is done since the directions API gives a different distance then the
    // one we measure using turf.
    routeDistance = RouteUtils.getDistanceToEndOfRoute(
      route.getLegs().get(0).getSteps().get(0).getManeuver().asPosition(),
      route,
      TurfConstants.UNIT_METERS
    );
  }

  /**
   * Gives a {@link RouteLegProgress} object with information about the particular leg the user is currently on.
   *
   * @return a {@link RouteLegProgress} object.
   * @since 0.1.0
   */
  public RouteLegProgress getCurrentLegProgress() {
    return currentLegProgress;
  }

  /**
   * Index representing the current leg.
   *
   * @return an {@code integer} representing the current leg the user is on.
   * @since 0.1.0
   */
  public int getLegIndex() {
    return currentLegIndex;
  }

  /**
   * Provides the current {@link RouteLeg} the user is on.
   *
   * @return a {@link RouteLeg} the user is currently on.
   */
  public RouteLeg getCurrentLeg() {
    return route.getLegs().get(getLegIndex());
  }

  /**
   * Total distance traveled in meters along route.
   *
   * @return a double value representing the total distance the user has traveled along the route, using unit meters.
   * @since 0.1.0
   */
  public double getDistanceTraveled() {
    return routeDistance - getDistanceRemaining();
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return {@code long} value representing the duration remaining till end of route, in unit seconds.
   * @since 0.1.0
   */
  public long getDurationRemaining() {
    return (long) ((1 - getFractionTraveled()) * route.getDuration());
  }

  /**
   * Get the fraction traveled along the current route, this is a float value between 0 and 1 and isn't guaranteed to
   * reach 1 before the user reaches the end of the route.
   *
   * @return a float value between 0 and 1 representing the fraction the user has traveled along the route.
   * @since 0.1.0
   */
  public float getFractionTraveled() {
    return (float) (getDistanceTraveled() / routeDistance);
  }

  /**
   * Provides the duration remaining in seconds till the user reaches the end of the route.
   *
   * @return {@code long} value representing the duration remaining till end of route, in unit seconds.
   * @since 0.1.0
   */
  public double getDistanceRemaining() {
    return RouteUtils.getDistanceToEndOfRoute(userSnappedPosition, route, TurfConstants.UNIT_METERS);
  }

  /**
   * Get the most recently provided alert level, this can and will only be one of the alert constants.
   *
   * @return an {@code integer} representing the most recent user alert level.
   * @since 0.1.0
   */
  public int getAlertUserLevel() {
    return alertUserLevel;
  }

  /**
   * Get the route the navigation session is currently using.
   *
   * @return a {@link DirectionsRoute} currently being used for the navigation session.
   * @since 0.1.0
   */
  public DirectionsRoute getRoute() {
    return route;
  }

  /**
   * Provides the users location snapped to the current route they are navigating on.
   *
   * @return {@link Position} object with coordinates snapping the user to the route.
   * @since 0.1.0
   */
  public Position usersCurrentSnappedPosition() {
    return userSnappedPosition;
  }
}
