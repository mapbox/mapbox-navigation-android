package com.mapbox.navigation.ui.legacy

/**
 * Navigation constants
 *
 * @since 0.1.0
 */
object NavigationConstants {

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
}
