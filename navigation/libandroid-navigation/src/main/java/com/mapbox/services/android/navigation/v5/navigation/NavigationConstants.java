package com.mapbox.services.android.navigation.v5.navigation;

import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;

/**
 * Navigation constants
 */
public class NavigationConstants {

  public static final int DEPARTURE_MILESTONE = 1;
  public static final int NEW_STEP_MILESTONE = 2;
  public static final int IMMINENT_MILESTONE = 3;
  public static final int URGENT_MILESTONE = 4;
  public static final int ARRIVAL_MILESTONE = 5;

  /**
   * Random integer value used for identifying the navigation notification.
   */
  static final int NAVIGATION_NOTIFICATION_ID = 5678;

  /**
   * Threshold user must be in within to count as completing a step. One of two heuristics used to know when a user
   * completes a step, see `RouteControllerManeuverZoneRadius`. The users `heading` and the `finalHeading` are
   * compared. If this number is within `RouteControllerMaximumAllowedDegreeOffsetForTurnCompletion`, the user has
   * completed the step.
   */
  static final int MAXIMUM_ALLOWED_DEGREE_OFFSET_FOR_TURN_COMPLETION = 30;

  /**
   * Radius in meters the user must enter to count as completing a step. One of two heuristics used to know when a user
   * completes a step, see `RouteControllerMaximumAllowedDegreeOffsetForTurnCompletion`.
   */
  static final int MANEUVER_ZONE_RADIUS = 40;

  /**
   * Maximum number of meters the user can travel away from step before the
   * {@link OffRouteListener}'s called.
   */
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
  static final double DEAD_RECKONING_TIME_INTERVAL = 1.0;

  /**
   * Maximum angle the user puck will be rotated when snapping the user's course to the route line.
   */
  static final int MAX_MANIPULATED_COURSE_ANGLE = 25;

  // Bundle variable keys
  public static final String NAVIGATION_VIEW_ORIGIN_LAT_KEY = "origin_lat";
  public static final String NAVIGATION_VIEW_ORIGIN_LNG_KEY = "origin_long";
  public static final String NAVIGATION_VIEW_DESTINATION_LAT_KEY = "destination_lat";
  public static final String NAVIGATION_VIEW_DESTINATION_LNG_KEY = "destination_long";
  public static final String ROUTE_BELOW_LAYER = "admin-3-4-boundaries-bg";

  // Step Maneuver Types
  public static final String STEP_MANEUVER_TYPE_TURN = "turn";
  public static final String STEP_MANEUVER_TYPE_NEW_NAME = "new name";
  public static final String STEP_MANEUVER_TYPE_DEPART = "depart";
  public static final String STEP_MANEUVER_TYPE_ARRIVE = "arrive";
  public static final String STEP_MANEUVER_TYPE_MERGE = "merge";
  public static final String STEP_MANEUVER_TYPE_ON_RAMP = "on ramp";
  public static final String STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp";
  public static final String STEP_MANEUVER_TYPE_FORK = "fork";
  public static final String STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road";
  public static final String STEP_MANEUVER_TYPE_CONTINUE = "continue";
  public static final String STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout";
  public static final String STEP_MANEUVER_TYPE_ROTARY = "rotary";
  public static final String STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn";
  public static final String STEP_MANEUVER_TYPE_NOTIFICATION = "notification";

  // Step Maneuver Modifiers
  public static final String STEP_MANEUVER_MODIFIER_UTURN = "uturn";
  public static final String STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right";
  public static final String STEP_MANEUVER_MODIFIER_RIGHT = "right";
  public static final String STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right";
  public static final String STEP_MANEUVER_MODIFIER_STRAIGHT = "straight";
  public static final String STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left";
  public static final String STEP_MANEUVER_MODIFIER_LEFT = "left";
  public static final String STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left";

  // Turn Lane Indication
  public static final String TURN_LANE_INDICATION_LEFT = "left";
  public static final String TURN_LANE_INDICATION_SHARP_LEFT = "sharp left";
  public static final String TURN_LANE_INDICATION_SLIGHT_LEFT = "slight left";
  public static final String TURN_LANE_INDICATION_STRAIGHT = "straight";
  public static final String TURN_LANE_INDICATION_NONE = "none";
  public static final String TURN_LANE_INDICATION_RIGHT = "right";
  public static final String TURN_LANE_INDICATION_SHARP_RIGHT = "sharp right";
  public static final String TURN_LANE_INDICATION_SLIGHT_RIGHT = "slight right";
  public static final String TURN_LANE_INDICATION_UTURN = "uturn";
}
