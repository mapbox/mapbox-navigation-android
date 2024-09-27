package com.mapbox.navigation.ui.maps.internal.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Geometry
import com.mapbox.geojson.LineString
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class RouteCallout {

    abstract val route: NavigationRoute
    abstract val geometry: Geometry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class Eta internal constructor(
        override val route: NavigationRoute,
        override val geometry: LineString,
        val isPrimary: Boolean,
    ) : RouteCallout()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class DurationDifference internal constructor(
        override val route: NavigationRoute,
        override val geometry: LineString,
        val duration: Duration,
        val type: DurationDifferenceType,
    ) : RouteCallout()
}
