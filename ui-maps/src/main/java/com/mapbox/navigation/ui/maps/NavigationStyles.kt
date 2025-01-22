package com.mapbox.navigation.ui.maps

import com.mapbox.api.directions.v5.DirectionsCriteria

/**
 * An object that links to default navigation styles.
 * We recommend using these map styles for an optimized map appearance for navigation use-cases.
 */
object NavigationStyles {
    /**
     * User ID for day mode
     */
    const val NAVIGATION_DAY_STYLE_USER_ID = DirectionsCriteria.PROFILE_DEFAULT_USER

    /**
     * Style ID for day mode
     */
    const val NAVIGATION_DAY_STYLE_ID = "navigation-day-v1"

    /**
     * Default navigation style for day mode
     */
    const val NAVIGATION_DAY_STYLE =
        "mapbox://styles/$NAVIGATION_DAY_STYLE_USER_ID/$NAVIGATION_DAY_STYLE_ID"

    /**
     * User ID for night mode
     */
    const val NAVIGATION_NIGHT_STYLE_USER_ID = DirectionsCriteria.PROFILE_DEFAULT_USER

    /**
     * Style ID for night mode
     */
    const val NAVIGATION_NIGHT_STYLE_ID = "navigation-night-v1"

    /**
     * Default navigation style for night mode
     */
    const val NAVIGATION_NIGHT_STYLE =
        "mapbox://styles/$NAVIGATION_NIGHT_STYLE_USER_ID/$NAVIGATION_NIGHT_STYLE_ID"
}
