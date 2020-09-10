package com.mapbox.navigation.ui

/**
 * Navigation constants
 *
 */
object NavigationConstants {

    /**
     * Milliseconds duration in which the AlertView is shown with the "Report Problem" text.
     */
    const val ALERT_VIEW_PROBLEM_DURATION: Long = 10000

    /**
     * Milliseconds duration in which the feedback BottomSheet is shown.
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
     * 125 seconds remaining is considered a low alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     */
    const val NAVIGATION_LOW_ALERT_DURATION = 125

    /**
     * 70 seconds remaining is considered a medium alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     */
    const val NAVIGATION_MEDIUM_ALERT_DURATION = 70

    /**
     * 15 seconds remaining is considered a high alert level when
     * navigating along a [com.mapbox.api.directions.v5.models.LegStep].
     *
     */
    const val NAVIGATION_HIGH_ALERT_DURATION = 15

    /**
     * Maximum milliseconds duration of the zoom/tilt adjustment animation while tracking.
     */
    const val NAVIGATION_MAX_CAMERA_ADJUSTMENT_ANIMATION_DURATION = 1500L

    /**
     * Minimum milliseconds duration of the zoom adjustment animation while tracking.
     */
    const val NAVIGATION_MIN_CAMERA_ZOOM_ADJUSTMENT_ANIMATION_DURATION = 300L

    /**
     * Minimum milliseconds duration of the tilt adjustment animation while tracking.
     */
    const val NAVIGATION_MIN_CAMERA_TILT_ADJUSTMENT_ANIMATION_DURATION = 750L

    /**
     * Minimum update interval (in nanoseconds) of the vanishing point when enabled.
     *
     * Represents a maximum 16 updates a second.
     */
    const val DEFAULT_VANISHING_POINT_MIN_UPDATE_INTERVAL_NANO = 62_500_000L
}
