package com.mapbox.navigation.ui.maps.route.callout.model

import androidx.annotation.IntDef
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Describes the possible callout types on the route line.
 */
@ExperimentalPreviewMapboxNavigationAPI
object RouteCalloutType {

    /**
     * Shows the route duration
     */
    const val ROUTES_OVERVIEW = 0

    /**
     * Shows the relative diff between the main route and the alternative
     *
     */
    const val NAVIGATION = 1

    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        ROUTES_OVERVIEW,
        NAVIGATION,
    )
    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.TYPE,
        AnnotationTarget.FUNCTION,
    )
    annotation class Type
}
