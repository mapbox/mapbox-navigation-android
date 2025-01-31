package com.mapbox.navigation.base.route

import androidx.annotation.StringDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Describes which kind of router presents response.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Retention(AnnotationRetention.BINARY)
@StringDef(
    RouterOrigin.ONLINE,
    RouterOrigin.OFFLINE,
    RouterOrigin.CUSTOM_EXTERNAL,
)
annotation class RouterOrigin {

    companion object {
        /**
         * Router based on Directions API or Map Matching API
         *
         * See also [https://docs.mapbox.com/help/glossary/directions-api/] and
         * [https://docs.mapbox.com/help/glossary/map-matching-api/]
         */
        const val ONLINE = "ONLINE"

        /**
         * Router based on embedded offline library and local navigation tiles.
         */
        const val OFFLINE = "OFFLINE"

        /**
         * Customers custom router
         */
        @ExperimentalPreviewMapboxNavigationAPI
        const val CUSTOM_EXTERNAL = "CUSTOM_EXTERNAL"
    }
}
