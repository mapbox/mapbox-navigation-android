package com.mapbox.services.android.navigation.v5.navigation;

import android.support.annotation.IntDef;
import android.support.annotation.StringDef;

/**
 * Navigation constants
 *
 * @since 0.1.0
 */
public final class NavigationConstants {

  private NavigationConstants() {
    // Empty private constructor to prevent users creating an instance of this class.
  }

  /**
   * If default voice instructions are enabled, this identifier will be used to differentiate them
   * from custom milestones in the
   * {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener}.
   *
   * @since 0.7.0
   */
  public static final int VOICE_INSTRUCTION_MILESTONE_ID = 1;


  /**
   * String channel used to post the navigation notification (custom or default).
   * <p>
   * If &gt; Android O, a notification channel needs to be created to properly post the notification.
   *
   * @since 0.8.0
   */
  public static final String NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL";

  /**
   * This identifier will be used to
   * differentiate the {@link com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone}
   * from custom milestones in the {@link com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener}.
   *
   * @since 0.8.0
   */
  public static final int BANNER_INSTRUCTION_MILESTONE_ID = 2;

  /**
   * Random integer value used for identifying the navigation notification.
   *
   * @since 0.5.0
   */
  public static final int NAVIGATION_NOTIFICATION_ID = 5678;

  /**
   * Duration in which the AlertView is shown with the "Report Problem" text.
   */
  public static final long ALERT_VIEW_PROBLEM_DURATION = 10000;

  /**
   * Duration in which the feedback BottomSheet is shown.
   */
  public static final long FEEDBACK_BOTTOM_SHEET_DURATION = 10000;

  /**
   * If a set of light / dark themes been set in {@link android.content.SharedPreferences}
   */
  public static final String NAVIGATION_VIEW_PREFERENCE_SET_THEME = "navigation_view_theme_preference";

  /**
   * Key for the set light theme in preferences
   */
  public static final String NAVIGATION_VIEW_LIGHT_THEME = "navigation_view_light_theme";

  /**
   * Key for the set dark theme in preferences
   */
  public static final String NAVIGATION_VIEW_DARK_THEME = "navigation_view_dark_theme";

  /**
   * NavigationLauncher key for storing initial map position in Intent
   */
  public static final String NAVIGATION_VIEW_INITIAL_MAP_POSITION = "navigation_view_initial_map_position";

  /**
   * In seconds, how quickly {@link com.mapbox.services.android.navigation.v5.route.FasterRouteDetector}
   * will tell {@link RouteProcessorBackgroundThread} to check
   * for a faster {@link com.mapbox.api.directions.v5.models.DirectionsRoute}.
   *
   * @since 0.9.0
   */
  public static final int NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL = 120;

  /**
   * 125 seconds remaining is considered a low alert level when
   * navigating along a {@link com.mapbox.api.directions.v5.models.LegStep}.
   *
   * @since 0.9.0
   */
  public static final int NAVIGATION_LOW_ALERT_DURATION = 125;

  /**
   * 70 seconds remaining is considered a medium alert level when
   * navigating along a {@link com.mapbox.api.directions.v5.models.LegStep}.
   *
   * @since 0.9.0
   */
  public static final int NAVIGATION_MEDIUM_ALERT_DURATION = 70;

  /**
   * 15 seconds remaining is considered a high alert level when
   * navigating along a {@link com.mapbox.api.directions.v5.models.LegStep}.
   *
   * @since 0.10.1
   */
  public static final int NAVIGATION_HIGH_ALERT_DURATION = 15;

  /**
   * Default approximate location engine interval lag in milliseconds
   * <p>
   * This value will be used to offset the time at which the current location was calculated
   * in such a way as to project the location forward along the current trajectory so as to
   * appear more in sync with the users ground-truth location
   *
   * @since 0.20.0
   */
  static final int NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG = 1500;

  static final long ROUTE_REFRESH_INTERVAL = 5 * 60 * 1000L;

  /**
   * Defines the minimum zoom level of the displayed map.
   */
  public static final double NAVIGATION_MINIMUM_MAP_ZOOM = 7d;

  /**
   * Maximum duration of the zoom/tilt adjustment animation while tracking.
   */
  public static final long NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION = 1500L;

  /**
   * Minimum duration of the zoom adjustment animation while tracking.
   */
  public static final long NAVIGATION_MIN_CAMERA_ZOOM_ADJUSTMENT_ANIMATION_DURATION = 300L;

  /**
   * Minimum duration of the tilt adjustment animation while tracking.
   */
  public static final long NAVIGATION_MIN_CAMERA_TILT_ADJUSTMENT_ANIMATION_DURATION = 750L;

  static final String NON_NULL_APPLICATION_CONTEXT_REQUIRED = "Non-null application context required.";

  // Bundle variable keys
  public static final String NAVIGATION_VIEW_ROUTE_KEY = "route_json";
  public static final String NAVIGATION_VIEW_SIMULATE_ROUTE = "navigation_view_simulate_route";
  public static final String NAVIGATION_VIEW_ROUTE_PROFILE_KEY = "navigation_view_route_profile";
  public static final String OFFLINE_PATH_KEY = "offline_path_key";
  public static final String OFFLINE_VERSION_KEY = "offline_version_key";
  public static final String MAP_DATABASE_PATH_KEY = "offline_map_database_path_key";

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
  public static final String STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout";
  public static final String STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary";

  @StringDef( {
    STEP_MANEUVER_TYPE_TURN,
    STEP_MANEUVER_TYPE_NEW_NAME,
    STEP_MANEUVER_TYPE_DEPART,
    STEP_MANEUVER_TYPE_ARRIVE,
    STEP_MANEUVER_TYPE_MERGE,
    STEP_MANEUVER_TYPE_ON_RAMP,
    STEP_MANEUVER_TYPE_OFF_RAMP,
    STEP_MANEUVER_TYPE_FORK,
    STEP_MANEUVER_TYPE_END_OF_ROAD,
    STEP_MANEUVER_TYPE_CONTINUE,
    STEP_MANEUVER_TYPE_ROUNDABOUT,
    STEP_MANEUVER_TYPE_ROTARY,
    STEP_MANEUVER_TYPE_ROUNDABOUT_TURN,
    STEP_MANEUVER_TYPE_NOTIFICATION,
    STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT,
    STEP_MANEUVER_TYPE_EXIT_ROTARY
  })
  public @interface ManeuverType {
  }

  // Step Maneuver Modifiers
  public static final String STEP_MANEUVER_MODIFIER_UTURN = "uturn";
  public static final String STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right";
  public static final String STEP_MANEUVER_MODIFIER_RIGHT = "right";
  public static final String STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right";
  public static final String STEP_MANEUVER_MODIFIER_STRAIGHT = "straight";
  public static final String STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left";
  public static final String STEP_MANEUVER_MODIFIER_LEFT = "left";
  public static final String STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left";

  @StringDef( {
    STEP_MANEUVER_MODIFIER_UTURN,
    STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
    STEP_MANEUVER_MODIFIER_RIGHT,
    STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
    STEP_MANEUVER_MODIFIER_STRAIGHT,
    STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
    STEP_MANEUVER_MODIFIER_LEFT,
    STEP_MANEUVER_MODIFIER_SHARP_LEFT
  })
  public @interface ManeuverModifier {
  }

  // Turn Lane Indication
  public static final String TURN_LANE_INDICATION_LEFT = "left";
  public static final String TURN_LANE_INDICATION_SLIGHT_LEFT = "slight left";
  public static final String TURN_LANE_INDICATION_STRAIGHT = "straight";
  public static final String TURN_LANE_INDICATION_RIGHT = "right";
  public static final String TURN_LANE_INDICATION_SLIGHT_RIGHT = "slight right";
  public static final String TURN_LANE_INDICATION_UTURN = "uturn";

  // Distance Rounding Increments
  public static final int ROUNDING_INCREMENT_FIVE = 5;
  public static final int ROUNDING_INCREMENT_TEN = 10;
  public static final int ROUNDING_INCREMENT_TWENTY_FIVE = 25;
  public static final int ROUNDING_INCREMENT_FIFTY = 50;
  public static final int ROUNDING_INCREMENT_ONE_HUNDRED = 100;

  @IntDef( {
    ROUNDING_INCREMENT_FIVE,
    ROUNDING_INCREMENT_TEN,
    ROUNDING_INCREMENT_TWENTY_FIVE,
    ROUNDING_INCREMENT_FIFTY,
    ROUNDING_INCREMENT_ONE_HUNDRED
  })
  public @interface RoundingIncrement {
  }
}
