package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.Experimental;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;

/**
 * Navigation constants
 */

public class NavigationConstants {

  @Experimental
  public static final int DEPARTURE_MILESTONE = 1;
  @Experimental
  public static final int NEW_STEP_MILESTONE = 2;
  @Experimental
  public static final int IMMINENT_MILESTONE = 3;
  @Experimental
  public static final int URGENT_MILESTONE = 4;
  @Experimental
  public static final int ARRIVAL_MILESTONE = 5;

  /**
   * Threshold user must be in within to count as completing a step. One of two heuristics used to know when a user
   * completes a step, see `RouteControllerManeuverZoneRadius`. The users `heading` and the `finalHeading` are
   * compared. If this number is within `RouteControllerMaximumAllowedDegreeOffsetForTurnCompletion`, the user has
   * completed the step.
   */
  @Experimental
  static final int MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION = 30;

  /**
   * Radius in meters the user must enter to count as completing a step. One of two heuristics used to know when a user
   * completes a step, see `RouteControllerMaximumAllowedDegreeOffsetForTurnCompletion`.
   */
  @Experimental
  static final int MANEUVER_ZONE_RADIUS = 40;

  /**
   * Maximum number of meters the user can travel away from step before the
   * {@link OffRouteListener}'s called.
   */
  @Experimental
  static final double MAXIMUM_DISTANCE_BEFORE_OFF_ROUTE = 50;

  /**
   * Seconds used before a reroute occurs.
   */
  static final int SECONDS_BEFORE_REROUTE = 3;

  /**
   * Accepted deviation excluding horizontal accuracy before the user is considered to be off route.
   */
  static final double USER_LOCATION_SNAPPING_DISTANCE = 10;

  /**
   * When calculating whether or not the user is on the route, we look where the user will be given their speed and
   * this variable.
   */
  @Experimental
  static final double DEAD_RECKONING_TIME_INTERVAL = 1.0;

  /**
   * Maximum angle the user puck will be rotated when snapping the user's course to the route line.
   */
  @Experimental
  static final int MAX_MANIPULATED_COURSE_ANGLE = 25;
}
