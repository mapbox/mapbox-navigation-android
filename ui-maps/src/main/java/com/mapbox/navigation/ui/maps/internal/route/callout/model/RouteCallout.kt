package com.mapbox.navigation.ui.maps.internal.route.callout.model

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.route.NavigationRoute
import kotlin.time.Duration

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class RouteCallout {

    abstract val route: NavigationRoute
    abstract val maxZoom: Float?
    abstract val minZoom: Float?
    abstract val priority: Long?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class Eta internal constructor(
        override val route: NavigationRoute,
        override val maxZoom: Float?,
        override val minZoom: Float?,
        override val priority: Long?,
        val isPrimary: Boolean,
    ) : RouteCallout()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class DurationDifference internal constructor(
        override val route: NavigationRoute,
        override val maxZoom: Float?,
        override val minZoom: Float?,
        override val priority: Long?,
        val duration: Duration,
        val type: DurationDifferenceType,
    ) : RouteCallout()
}
