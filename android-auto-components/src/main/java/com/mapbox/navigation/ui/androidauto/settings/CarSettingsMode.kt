package com.mapbox.navigation.ui.androidauto.settings

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * The navigation mode for which inline settings are requested.
 */
@ExperimentalPreviewMapboxNavigationAPI
enum class CarSettingsMode {
    /**
     * Navigation without an active or previewed route.
     */
    FREE_DRIVE,

    /**
     * A route is being previewed before starting guidance.
     */
    ROUTE_PREVIEW,

    /**
     * Turn-by-turn guidance is active.
     */
    ACTIVE_GUIDANCE,
}
