package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.annotation.StringDef

/**
 * This object contains pre-defined screens and experiences that are provided by Mapbox.
 * Each screen can be overridden with a screen of your choice.
 *
 * Each Mapbox defined screen is prefixed with "MAPBOX_" to prevent any future collisions.
 * For example, if you define a screen called "PICKUP", this library will never cause a
 * conflict with your screen because it will use "MAPBOX_PICKUP". Please do not prefix
 * your custom screens with "MAPBOX_" or else our library may add conflict upon upgrade.
 *
 * @see [MapboxScreenManager] for more details on customizing your experience.
 */
object MapboxScreen {

    /**
     * Provides the user instructions for enabling location permissions.
     */
    const val NEEDS_LOCATION_PERMISSION = "MAPBOX_NEEDS_LOCATION_PERMISSION"

    /**
     * Provides the user with a screen for changing app settings.
     */
    const val SETTINGS = "MAPBOX_SETTINGS"

    /**
     * Gives the user an ability to navigate without a specified route.
     */
    const val FREE_DRIVE = "MAPBOX_FREE_DRIVE"

    /**
     * Gives the user an ability to provide feedback for [FREE_DRIVE].
     */
    const val FREE_DRIVE_FEEDBACK = "MAPBOX_FREE_DRIVE_FEEDBACK"

    /**
     * Gives the user an ability to search for a destination.
     */
    const val SEARCH = "MAPBOX_SEARCH"

    /**
     * Give the user an ability to provide feedback for [SEARCH].
     */
    const val SEARCH_FEEDBACK = "MAPBOX_SEARCH_FEEDBACK"

    /**
     * Gives the user the ability to select a personally favorite destination.
     */
    const val FAVORITES = "MAPBOX_FAVORITES"

    /**
     * Give the user an ability to provide feedback for [FAVORITES].
     */
    const val FAVORITES_FEEDBACK = "MAPBOX_FAVORITES_FEEDBACK"

    /**
     * Gives the user an ability search for a destination with their voice.
     */
    const val GEO_DEEPLINK = "MAPBOX_GEO_DEEPLINK"

    /**
     * Gives the user an ability to search for a destination with their voice.
     */
    const val GEO_DEEPLINK_FEEDBACK = "MAPBOX_GEO_DEEPLINK_FEEDBACK"

    /**
     * Gives the user an ability to select a navigation route.
     */
    const val ROUTE_PREVIEW = "MAPBOX_ROUTE_PREVIEW"

    /**
     * Give the user an ability to provide feedback from [ROUTE_PREVIEW].
     */
    const val ROUTE_PREVIEW_FEEDBACK = "MAPBOX_ROUTE_PREVIEW_FEEDBACK"

    /**
     * Gives the user an ability to follow turn by turn directions to a destination.
     */
    const val ACTIVE_GUIDANCE = "MAPBOX_ACTIVE_GUIDANCE"

    /**
     * Give the user an ability to provide feedback from [ACTIVE_GUIDANCE],
     */
    const val ACTIVE_GUIDANCE_FEEDBACK = "MAPBOX_ACTIVE_GUIDANCE_FEEDBACK"

    /**
     * Shows the user a screen when they arrive at the destination.
     */
    const val ARRIVAL = "MAPBOX_ARRIVAL"

    /**
     * [MapboxScreen] keys
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        NEEDS_LOCATION_PERMISSION,
        SETTINGS,
        FREE_DRIVE,
        FREE_DRIVE_FEEDBACK,
        SEARCH,
        SEARCH_FEEDBACK,
        FAVORITES,
        FAVORITES_FEEDBACK,
        GEO_DEEPLINK,
        GEO_DEEPLINK_FEEDBACK,
        ROUTE_PREVIEW,
        ROUTE_PREVIEW_FEEDBACK,
        ACTIVE_GUIDANCE,
        ACTIVE_GUIDANCE_FEEDBACK,
        ARRIVAL,
    )
    annotation class Key
}
