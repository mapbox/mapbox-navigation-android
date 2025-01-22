package com.mapbox.navigation.core.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Interface definition for an observer that gets notified whenever route preview state changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface RoutesPreviewObserver {
    /***
     * Called when route preview state changes. Emits current state on subscription.
     *
     * Register the observer using [MapboxNavigation.registerRoutesPreviewObserver].
     */
    fun routesPreviewUpdated(update: RoutesPreviewUpdate)
}

/**
 * Routes preview update is provided via [RoutesPreviewObserver] whenever route
 * preview changes state.
 *
 * @param reason why route preview has been updated
 * @param routesPreview current state of route preview, null if routes preview isn't set
 */
@ExperimentalPreviewMapboxNavigationAPI
class RoutesPreviewUpdate internal constructor(
    @RoutesPreviewExtra.RoutePreviewUpdateReason val reason: String,
    val routesPreview: RoutesPreview?,
) {

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoutesPreviewUpdate
        if (reason != other.reason) return false
        if (routesPreview != other.routesPreview) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = reason.hashCode()
        result = 31 * result + routesPreview.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "RoutesPreviewUpdate(" +
            "reason='$reason', " +
            "routesPreview=$routesPreview" +
            ")"
    }
}
