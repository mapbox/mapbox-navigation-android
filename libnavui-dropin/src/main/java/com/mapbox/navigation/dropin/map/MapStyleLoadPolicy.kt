package com.mapbox.navigation.dropin.map

import androidx.annotation.IntDef
import com.mapbox.maps.MapView
import com.mapbox.navigation.dropin.NavigationView

/**
 * Map style load policy
 */
object MapStyleLoadPolicy {

    /**
     * Don't load map style. Style loading will be handled by host application.
     */
    const val NEVER = 0

    /**
     * Load style only once when [MapView] is added to [NavigationView].
     */
    const val ONCE = 1

    /**
     * Load style when [MapView] is added to [NavigationView] and reload it
     * every time device configuration changes.
     */
    const val ON_CONFIGURATION_CHANGE = 2

    /**
     * Defines map load style policy.
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        NEVER,
        ONCE,
        ON_CONFIGURATION_CHANGE,
    )
    annotation class MapLoadStylePolicy
}
