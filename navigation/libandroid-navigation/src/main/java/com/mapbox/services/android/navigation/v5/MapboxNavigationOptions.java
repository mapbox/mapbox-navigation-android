package com.mapbox.services.android.navigation.v5;

import android.location.Location;
import android.support.annotation.FloatRange;

public class MapboxNavigationOptions {

  private double maxTurnCompletionOffset;
  private double maneuverZoneRadius;

  private int mediumAlertInterval;
  private int highAlertInterval;

  private double minimumMediumAlertDistance;
  private double minimumHighAlertDistance;

  private double maximumDistanceOffRoute;
  private double deadReckoningTimeInterval;
  private double maxManipulatedCourseAngle;

  /**
   * Creates a new MapboxNavigationOptions object.
   *
   * @since 0.2.0
   */
  public MapboxNavigationOptions() {
    // Set the initial variables to equal the default.
    maxTurnCompletionOffset = NavigationConstants.MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION;
    maneuverZoneRadius = NavigationConstants.MANEUVER_ZONE_RADIUS;
    mediumAlertInterval = NavigationConstants.MEDIUM_ALERT_INTERVAL;
    highAlertInterval = NavigationConstants.HIGH_ALERT_INTERVAL;
    minimumMediumAlertDistance = NavigationConstants.MINIMUM_DISTANCE_FOR_MEDIUM_ALERT;
    minimumHighAlertDistance = NavigationConstants.MINIMUM_DISTANCE_FOR_HIGH_ALERT;
    maximumDistanceOffRoute = NavigationConstants.MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE;
    deadReckoningTimeInterval = NavigationConstants.DEAD_RECKONING_TIME_INTERVAL;
    maxManipulatedCourseAngle = NavigationConstants.MAX_MANIPULATED_COURSE_ANGLE;
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
   * Number of seconds left on step when a {@link NavigationConstants#MEDIUM_ALERT_LEVEL} alert occurs.
   *
   * @param mediumAlertInterval integer value in unit seconds representing the seconds left till a {@code medium} alert
   *                            level occurs.
   * @return this;
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMediumAlertInterval(int mediumAlertInterval) {
    this.mediumAlertInterval = mediumAlertInterval;
    return this;
  }

  /**
   * Number of seconds left on step when a {@link NavigationConstants#HIGH_ALERT_LEVEL} alert occurs.
   *
   * @param highAlertInterval integer value in unit seconds representing the seconds left till a {@code high} alert
   *                          level occurs.
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setHighAlertInterval(int highAlertInterval) {
    this.highAlertInterval = highAlertInterval;
    return this;
  }

  /**
   * Distance in meters representing the minimum length of a step for a {@link NavigationConstants#MEDIUM_ALERT_LEVEL}
   * to occur.
   *
   * @param minimumMediumAlertDistance double value in unit meters representing the minimum step length for a
   *                                   {@code medium} alert to occur.
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMinimumMediumAlertDistance(double minimumMediumAlertDistance) {
    this.minimumMediumAlertDistance = minimumMediumAlertDistance;
    return this;
  }

  /**
   * Distance in meters representing the minimum length of a step for a {@link NavigationConstants#HIGH_ALERT_LEVEL}
   * to occur.
   *
   * @param minimumHighAlertDistance double value in unit meters representing the minimum step length for a
   *                                 {@code high} alert to occur.
   * @return this.
   * @since 0.2.0
   */
  public MapboxNavigationOptions setMinimumHighAlertDistance(double minimumHighAlertDistance) {
    this.minimumHighAlertDistance = minimumHighAlertDistance;
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
   * Get the current number of seconds required for a {@link NavigationConstants#MEDIUM_ALERT_LEVEL} alert to occur.
   *
   * @return integer value in unit seconds representing the seconds left till a {@code medium} alert level occurs.
   * @since 0.2.0
   */
  public int getMediumAlertInterval() {
    return mediumAlertInterval;
  }

  /**
   * Get the current number of seconds required for a {@link NavigationConstants#HIGH_ALERT_LEVEL} alert to occurs.
   *
   * @return integer value in unit seconds representing the seconds left till a {@code high} alert level occurs.
   * @since 0.2.0
   */
  public int getHighAlertInterval() {
    return highAlertInterval;
  }

  /**
   * Get the current required distance in meters representing the minimum length of a step for a
   * {@link NavigationConstants#MEDIUM_ALERT_LEVEL} to occur.
   *
   * @return double value in unit meters representing the minimum step length for a {@code medium} alert to occur.
   * @since 0.2.0
   */
  public double getMinimumMediumAlertDistance() {
    return minimumMediumAlertDistance;
  }

  /**
   * Get the current required distance in meters representing the minimum length of a step for a
   * {@link NavigationConstants#HIGH_ALERT_LEVEL} to occur.
   *
   * @return double value in unit meters representing the minimum step length for a {@code high} alert to occur.
   * @since 0.2.0
   */
  public double getMinimumHighAlertDistance() {
    return minimumHighAlertDistance;
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
}
