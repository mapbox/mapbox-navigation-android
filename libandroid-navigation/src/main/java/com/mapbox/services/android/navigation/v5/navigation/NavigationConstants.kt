package com.mapbox.services.android.navigation.v5.navigation

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.mapbox.services.android.navigation.v5.internal.navigation.RouteProcessorBackgroundThread

/**
 * Navigation constants
 *
 * @since 0.1.0
 */
object NavigationConstants {

    /**
     * Mapbox shared preferences file name
     */
    const val MAPBOX_SHARED_PREFERENCES = "MapboxSharedPreferences"

    /**
     * If default voice instructions are enabled, this identifier will be used to differentiate them
     * from custom milestones in the
     * [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener].
     *
     * @since 0.7.0
     */
    const val VOICE_INSTRUCTION_MILESTONE_ID = 1

    /**
     * String channel used to post the navigation notification (custom or default).
     *
     *
     * If &gt; Android O, a notification channel needs to be created to properly post the notification.
     *
     * @since 0.8.0
     */
    const val NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL"

    /**
     * This identifier will be used to
     * differentiate the [com.mapbox.services.android.navigation.v5.milestone.BannerInstructionMilestone]
     * from custom milestones in the [com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener].
     *
     * @since 0.8.0
     */
    const val BANNER_INSTRUCTION_MILESTONE_ID = 2

    /**
     * Random integer value used for identifying the navigation notification.
     *
     * @since 0.5.0
     */
    const val NAVIGATION_NOTIFICATION_ID = 5678

    /**
     * Duration in which the AlertView is shown with the "Report Problem" text.
     */
    const val ALERT_VIEW_PROBLEM_DURATION: Long = 10000

    /**
     * Duration in which the feedback BottomSheet is shown.
     */
    const val FEEDBACK_BOTTOM_SHEET_DURATION: Long = 10000

    /**
     * If a set of light / dark themes been set in [android.content.SharedPreferences]
     */
    const val NAVIGATION_VIEW_PREFERENCE_SET_THEME = "navigation_view_theme_preference"

    /**
     * Key for the set light theme in preferences
     */
    const val NAVIGATION_VIEW_LIGHT_THEME = "navigation_view_light_theme"

    /**
     * Key for the set dark theme in preferences
     */
    const val NAVIGATION_VIEW_DARK_THEME = "navigation_view_dark_theme"

    /**
     * NavigationLauncher key for storing initial map position in Intent
     */
    const val NAVIGATION_VIEW_INITIAL_MAP_POSITION = "navigation_view_initial_map_position"

    /**
     * In seconds, how quickly [com.mapbox.services.android.navigation.v5.route.FasterRouteDetector]
     * will tell [RouteProcessorBackgroundThread] to check
     * for a faster [com.mapbox.api.directions.v5.models.DirectionsRoute].
     *
     * @since 0.9.0
     */
    const val NAVIGATION_CHECK_FASTER_ROUTE_INTERVAL = 120

    /**
     * 125 seconds remaining is considered a low alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     * @since 0.9.0
     */
    const val NAVIGATION_LOW_ALERT_DURATION = 125

    /**
     * 70 seconds remaining is considered a medium alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     * @since 0.9.0
     */
    const val NAVIGATION_MEDIUM_ALERT_DURATION = 70

    /**
     * 15 seconds remaining is considered a high alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     * @since 0.10.1
     */
    const val NAVIGATION_HIGH_ALERT_DURATION = 15

    /**
     * Default approximate location engine interval lag in milliseconds
     *
     *
     * This value will be used to offset the time at which the current location was calculated
     * in such a way as to project the location forward along the current trajectory so as to
     * appear more in sync with the users ground-truth location
     *
     * @since 0.20.0
     */
    internal const val NAVIGATION_LOCATION_ENGINE_INTERVAL_LAG = 1500

    /**
     * Default off route threshold in meters
     */
    internal const val NAVIGATION_OFF_ROUTE_THRESHOLD = 50.0f

    /**
     * Default off route threshold in meters when near an intersection which is more prone
     * to inaccurate gps fixes
     */
    internal const val NAVIGATION_OFF_ROUTE_THRESHOLD_WHEN_NEAR_INTERSECTION = 25.0f

    /**
     * Default radius in meters for off route detection near intersection
     */
    internal const val NAVIGATION_INTERSECTION_RADIUS_FOR_OFF_ROUTE_DETECTION = 40.0f

    internal const val ROUTE_REFRESH_INTERVAL = 5 * 60 * 1000L

    /**
     * Defines the minimum zoom level of the displayed map.
     */
    const val NAVIGATION_MINIMUM_MAP_ZOOM = 7.0

    /**
     * Maximum duration of the zoom/tilt adjustment animation while tracking.
     */
    const val NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION = 1500L

    /**
     * Minimum duration of the zoom adjustment animation while tracking.
     */
    const val NAVIGATION_MIN_CAMERA_ZOOM_ADJUSTMENT_ANIMATION_DURATION = 300L

    /**
     * Minimum duration of the tilt adjustment animation while tracking.
     */
    const val NAVIGATION_MIN_CAMERA_TILT_ADJUSTMENT_ANIMATION_DURATION = 750L

    internal const val NON_NULL_APPLICATION_CONTEXT_REQUIRED =
        "Non-null application context required."

    // Bundle variable keys
    const val NAVIGATION_VIEW_ROUTE_KEY = "route_json"
    const val NAVIGATION_VIEW_SIMULATE_ROUTE = "navigation_view_simulate_route"
    const val OFFLINE_PATH_KEY = "offline_path_key"
    const val OFFLINE_VERSION_KEY = "offline_version_key"
    const val MAP_DATABASE_PATH_KEY = "offline_map_database_path_key"
    const val MAP_STYLE_URL_KEY = "offline_map_style_url_key"

    // Step Maneuver Types
    const val STEP_MANEUVER_TYPE_TURN = "turn"
    const val STEP_MANEUVER_TYPE_NEW_NAME = "new name"
    const val STEP_MANEUVER_TYPE_DEPART = "depart"
    const val STEP_MANEUVER_TYPE_ARRIVE = "arrive"
    const val STEP_MANEUVER_TYPE_MERGE = "merge"
    const val STEP_MANEUVER_TYPE_ON_RAMP = "on ramp"
    const val STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp"
    const val STEP_MANEUVER_TYPE_FORK = "fork"
    const val STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road"
    const val STEP_MANEUVER_TYPE_CONTINUE = "continue"
    const val STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout"
    const val STEP_MANEUVER_TYPE_ROTARY = "rotary"
    const val STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn"
    const val STEP_MANEUVER_TYPE_NOTIFICATION = "notification"
    const val STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout"
    const val STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary"

    // Step Maneuver Modifiers
    const val STEP_MANEUVER_MODIFIER_UTURN = "uturn"
    const val STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right"
    const val STEP_MANEUVER_MODIFIER_RIGHT = "right"
    const val STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right"
    const val STEP_MANEUVER_MODIFIER_STRAIGHT = "straight"
    const val STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left"
    const val STEP_MANEUVER_MODIFIER_LEFT = "left"
    const val STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left"

    // Turn Lane Indication
    const val TURN_LANE_INDICATION_LEFT = "left"
    const val TURN_LANE_INDICATION_SLIGHT_LEFT = "slight left"
    const val TURN_LANE_INDICATION_STRAIGHT = "straight"
    const val TURN_LANE_INDICATION_RIGHT = "right"
    const val TURN_LANE_INDICATION_SLIGHT_RIGHT = "slight right"
    const val TURN_LANE_INDICATION_UTURN = "uturn"

    // Distance Rounding Increments
    const val ROUNDING_INCREMENT_FIVE = 5
    const val ROUNDING_INCREMENT_TEN = 10
    const val ROUNDING_INCREMENT_TWENTY_FIVE = 25
    const val ROUNDING_INCREMENT_FIFTY = 50
    const val ROUNDING_INCREMENT_ONE_HUNDRED = 100

    @StringDef(
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
    )
    annotation class ManeuverType

    @StringDef(
        STEP_MANEUVER_MODIFIER_UTURN,
        STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
        STEP_MANEUVER_MODIFIER_RIGHT,
        STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
        STEP_MANEUVER_MODIFIER_STRAIGHT,
        STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
        STEP_MANEUVER_MODIFIER_LEFT,
        STEP_MANEUVER_MODIFIER_SHARP_LEFT
    )
    annotation class ManeuverModifier

    @IntDef(
        ROUNDING_INCREMENT_FIVE,
        ROUNDING_INCREMENT_TEN,
        ROUNDING_INCREMENT_TWENTY_FIVE,
        ROUNDING_INCREMENT_FIFTY,
        ROUNDING_INCREMENT_ONE_HUNDRED
    )
    annotation class RoundingIncrement
} // Empty private constructor to prevent users creating an instance of this class.
