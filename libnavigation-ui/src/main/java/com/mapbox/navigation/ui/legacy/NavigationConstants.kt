package com.mapbox.navigation.ui.legacy

import androidx.annotation.IntDef
import androidx.annotation.StringDef

/**
 * Navigation constants
 *
 * @since 0.1.0
 */
object NavigationConstants {

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

    /**
     * The minimal lookahead value in milliseconds required to perform a lookahead animation.
     */
    const val MINIMAL_LOOKAHEAD_LOCATION_TIME_VALUE = 250L

    /**
     * Bundle key used to extract the route for launching the drop-in UI
     */
    const val NAVIGATION_VIEW_ROUTE_KEY = "route_json"

    /**
     * Bundle key to enable/disable simulation of route
     */
    const val NAVIGATION_VIEW_SIMULATE_ROUTE = "navigation_view_simulate_route"

    /**
     * Bundle key to store offline path
     */
    const val OFFLINE_PATH_KEY = "offline_path_key"

    /**
     * Bundle key to store offline version
     */
    const val OFFLINE_VERSION_KEY = "offline_version_key"

    /**
     * Bundle key to store map database path
     */
    const val MAP_DATABASE_PATH_KEY = "offline_map_database_path_key"

    /**
     * Bundle key to store map style URL
     */
    const val MAP_STYLE_URL_KEY = "offline_map_style_url_key"

    /**
     * Indicates "turn" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_TURN = "turn"

    /**
     * Indicates "new name" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_NEW_NAME = "new name"

    /**
     * Indicates "depart" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_DEPART = "depart"

    /**
     * Indicates "arrive" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_ARRIVE = "arrive"

    /**
     * Indicates "merge" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_MERGE = "merge"

    /**
     * Indicates "on ramp" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_ON_RAMP = "on ramp"

    /**
     * Indicates "off ramp" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_OFF_RAMP = "off ramp"

    /**
     * Indicates "fork" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_FORK = "fork"

    /**
     * Indicates "end of road" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_END_OF_ROAD = "end of road"

    /**
     * Indicates "continue" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_CONTINUE = "continue"

    /**
     * Indicates "roundabout" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_ROUNDABOUT = "roundabout"

    /**
     * Indicates "rotary" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_ROTARY = "rotary"

    /**
     * Indicates "roundabout turn" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_ROUNDABOUT_TURN = "roundabout turn"

    /**
     * Indicates "notification" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_NOTIFICATION = "notification"

    /**
     * Indicates "exit roundabout" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_EXIT_ROUNDABOUT = "exit roundabout"

    /**
     * Indicates "exit rotary" maneuver type to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_TYPE_EXIT_ROTARY = "exit rotary"

    /**
     * Indicates "uturn" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_UTURN = "uturn"

    /**
     * Indicates "sharp right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_SHARP_RIGHT = "sharp right"

    /**
     * Indicates "right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_RIGHT = "right"

    /**
     * Indicates "slight right" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT = "slight right"

    /**
     * Indicates "straight" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_STRAIGHT = "straight"

    /**
     * Indicates "slight left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_SLIGHT_LEFT = "slight left"

    /**
     * Indicates "left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_LEFT = "left"

    /**
     * Indicates "sharp left" maneuver modifier to be rendered in the [com.mapbox.navigation.ui.instruction.maneuver.ManeuverView]
     */
    const val STEP_MANEUVER_MODIFIER_SHARP_LEFT = "sharp left"

    /**
     * Indication to turn left
     */
    const val TURN_LANE_INDICATION_LEFT = "left"

    /**
     * Indication to turn slight left
     */
    const val TURN_LANE_INDICATION_SLIGHT_LEFT = "slight left"

    /**
     * Indication to go straight
     */
    const val TURN_LANE_INDICATION_STRAIGHT = "straight"

    /**
     * Indication to turn right
     */
    const val TURN_LANE_INDICATION_RIGHT = "right"

    /**
     * Indication to turn slight right
     */
    const val TURN_LANE_INDICATION_SLIGHT_RIGHT = "slight right"

    /**
     * Indication to take a u-turn
     */
    const val TURN_LANE_INDICATION_UTURN = "uturn"

    /**
     * Round small distances in increments of 5
     */
    const val ROUNDING_INCREMENT_FIVE = 5

    /**
     * Round small distances in increments of 10
     */
    const val ROUNDING_INCREMENT_TEN = 10

    /**
     * Round small distances in increments of 25
     */
    const val ROUNDING_INCREMENT_TWENTY_FIVE = 25

    /**
     * Round small distances in increments of 50
     */
    const val ROUNDING_INCREMENT_FIFTY = 50

    /**
     * Round small distances in increments of 100
     */
    const val ROUNDING_INCREMENT_ONE_HUNDRED = 100

    /**
     * Representation of ManeuverType in form of logical types
     */
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

    /**
     * Representation of ManeuverModifier in form of logical types
     */
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

    /**
     * Rounding increments for use in [com.mapbox.navigation.core.MapboxDistanceFormatter].
     */
    @IntDef(
        ROUNDING_INCREMENT_FIVE,
        ROUNDING_INCREMENT_TEN,
        ROUNDING_INCREMENT_TWENTY_FIVE,
        ROUNDING_INCREMENT_FIFTY,
        ROUNDING_INCREMENT_ONE_HUNDRED
    )
    annotation class RoundingIncrement
}
