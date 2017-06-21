package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.support.annotation.FloatRange;

import com.mapbox.services.api.directions.v5.DirectionsCriteria;

public class MapboxNavigationOptions {

  private double maxTurnCompletionOffset;
  private double maneuverZoneRadius;

  private double maximumDistanceOffRoute;
  private double deadReckoningTimeInterval;
  private double maxManipulatedCourseAngle;

  private double userLocationSnapDistance;
  private int secondsBeforeReroute;

  @NavigationProfiles.Profile
  private String profile;

  /**
   * Creates a new MapboxNavigationOptions object.
   *
   * @since 0.2.0
   */
  public MapboxNavigationOptions() {
    // Set the initial variables to equal the default.
    maxTurnCompletionOffset = NavigationConstants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION;
    maneuverZoneRadius = NavigationConstants.MANEUVER_ZONE_RADIUS;
    maximumDistanceOffRoute = NavigationConstants.MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE;
    deadReckoningTimeInterval = NavigationConstants.DEAD_RECKONING_TIME_INTERVAL;
    maxManipulatedCourseAngle = NavigationConstants.MAX_MANIPULATED_COURSE_ANGLE;
    userLocationSnapDistance = NavigationConstants.USER_LOCATION_SNAPPING_DISTANCE;
    secondsBeforeReroute = NavigationConstants.SECONDS_BEFORE_REROUTE;
    profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC;
  }

  /**
   * Set the threshold user must be within to count as completing a
   * {@link com.mapbox.services.api.directions.v5.models.LegStep}. When checking if the users completed the step, the
   * users {@link Location#getBearing()} and the
   * {@link com.mapbox.services.api.directions.v5.models.StepManeuver#bearingAfter} are compared. If this number is
   * within the {@code setMaxTurnCompletionOffset} the user has completed the step. This is one of the two heuristics
   * used to know when a user completes a step, the other being {@link NavigationConstants#MANEUVER_ZONE_RADIUS}.
   *
   * @param maxTurnCompletionOffset degree double value between 0 and 359 representing the threshold used to consider if
   *                                the user has completed the step maneuver.
   * @return This.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMaxTurnCompletionOffset(
    @FloatRange(from = 0, to = 359) double maxTurnCompletionOffset) {
    this.maxTurnCompletionOffset = maxTurnCompletionOffset;
    return this;
  }

  /**
   * Radius, in meters, the user must enter to count as completing a step. This is one of the two heuristics used to
   * know when a user completes a step, the other being
   * {@link NavigationConstants#MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION}
   *
   * @param maneuverZoneRadius double radius value with unit meters representing the area the user must enter to count
   *                           as completing a step.
   * @return This.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setManeuverZoneRadius(double maneuverZoneRadius) {
    this.maneuverZoneRadius = maneuverZoneRadius;
    return this;
  }

  /**
   * Maximum distance in meters a user can travel away from the step geometry before the
   * {@link com.mapbox.services.android.navigation.v5.listeners.OffRouteListener}'s called.
   *
   * @param maximumDistanceOffRoute double value in unit meters representing the maximum distance a user can travel
   *                                before the {@code OffRouteListener} gets called.
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMaximumDistanceOffRoute(double maximumDistanceOffRoute) {
    this.maximumDistanceOffRoute = maximumDistanceOffRoute;
    return this;
  }

  /**
   * When calculating whether or not the user is on the route, we look where the user will be given their speed and
   * this {@code deadReckoningTimeInterval} variable.
   *
   * @param deadReckoningTimeInterval time interval in unit seconds we use for dead reckoning purposes.
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setDeadReckoningTimeInterval(double deadReckoningTimeInterval) {
    this.deadReckoningTimeInterval = deadReckoningTimeInterval;
    return this;
  }

  /**
   * Maximum angle the user puck will be rotated when snapping the user's course to the route line.
   *
   * @param maxManipulatedCourseAngle degree int value between 0 and 359 representing the maximum angle the user puck
   *                                  will be rotated
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMaxManipulatedCourseAngle(
    @FloatRange(from = 0, to = 359) double maxManipulatedCourseAngle) {
    this.maxManipulatedCourseAngle = maxManipulatedCourseAngle;
    return this;
  }

  /*
   * Getters
   */

  /**
   * Get the current set threshold user must be within to count as completing a
   * {@link com.mapbox.services.api.directions.v5.models.LegStep}. When checking if the user completed the step, the
   * users {@link com.mapbox.services.api.directions.v5.models.StepManeuver#bearingAfter} are compared. If this number
   * is within the {@code setMaxTurnCompletionOffset} the user has completed the step. This is one of the two heuristics
   * used to know when a user completes a step, the other being {@link NavigationConstants#MANEUVER_ZONE_RADIUS}.
   *
   * @return degree double value between 0 and 359 representing the threshold being used to consider if the user has
   * completed the step maneuver.
   * @since 0.2.0
   */
  public double getMaxTurnCompletionOffset() {
    return maxTurnCompletionOffset;
  }

  /**
   * Get the current radius, in meters, the user must enter to count as completing a step. This is one of the two
   * heuristics used to know when a user completes a step, the other being
   * {@link NavigationConstants#MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION}
   *
   * @return double radius value with unit meters representing the area the user must enter to count as completing a
   * step.
   * @since 0.2.0
   */
  public double getManeuverZoneRadius() {
    return maneuverZoneRadius;
  }

  /**
   * Get the current required maximum distance in meters a user can travel away from the step geometry before the
   * {@link com.mapbox.services.android.navigation.v5.listeners.OffRouteListener}'s called.
   *
   * @return double value in unit meters representing the maximum distance a user can travel before the
   * {@code OffRouteListener} gets called.
   * @since 0.2.0
   */
  public double getMaximumDistanceOffRoute() {
    return maximumDistanceOffRoute;
  }

  /**
   * Get the current time interval being used to calculate dead reckoning values.
   *
   * @return time interval in unit seconds being used for dead reckoning purposes.
   * @since 0.2.0
   */
  public double getDeadReckoningTimeInterval() {
    return deadReckoningTimeInterval;
  }

  /**
   * Get the maximum angle the user puck will be rotated when snapping the user's course to the route line.
   *
   * @return degree int value between 0 and 359 representing the maximum angle the user puck will be rotated.
   * @since 0.2.0
   */
  public double getMaxManipulatedCourseAngle() {
    return maxManipulatedCourseAngle;
  }

  /**
   * Determines the distance the user must stay within for snapping to route to occur.
   *
   * @return the distance in unit meters.
   * @since 0.3.0
   */
  public double getUserLocationSnapDistance() {
    return userLocationSnapDistance;
  }

  /**
   * set the distance the user must stay within for snapping to route to occur.
   *
   * @param userLocationSnapDistance distance value in unit meters
   * @since 0.3.0
   */
  public void setUserLocationSnapDistance(double userLocationSnapDistance) {
    this.userLocationSnapDistance = userLocationSnapDistance;
  }

  /**
   * The seconds before off-route happens, initially when the user goes off-route, a timer is started and waits till it
   * reaches this value and then notifies the off-route listener.
   *
   * @return seconds value before rerouting
   * @since 0.3.0
   */
  public int getSecondsBeforeReroute() {
    return secondsBeforeReroute;
  }

  /**
   * The seconds before off-route happens, initially when the user goes off-route, a timer is started and waits till it
   * reaches this value and then notifies the off-route listener.
   *
   * @param secondsBeforeReroute seconds value before rerouting
   * @since 0.3.0
   */
  public void setSecondsBeforeReroute(int secondsBeforeReroute) {
    this.secondsBeforeReroute = secondsBeforeReroute;
  }


  /**
   * Set the directions profile which will be used when requesting the route. It will also determine variables used to
   * determine alert levels.
   *
   * @param profile one of the profiles defined in {@link NavigationProfiles}
   */
  public void setDirectionsProfile(@NavigationProfiles.Profile String profile) {
    this.profile = profile;
  }

  /**
   * Get the directions profile which will be used when requesting the route. It will also determine variables used to
   * determine alert levels.
   *
   * @return one of the profiles defined in {@link NavigationProfiles}
   * @since 0.3.0
   */
  public String getDirectionsProfile() {
    return profile;
  }
}
