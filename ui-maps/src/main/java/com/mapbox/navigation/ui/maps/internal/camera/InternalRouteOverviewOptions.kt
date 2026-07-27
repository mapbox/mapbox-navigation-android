package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo

/**
 * Internal options consumed by [RouteOverviewViewportDataSource] to control route-overview framing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class InternalRouteOverviewOptions(
    val overviewMode: OverviewMode,
    val overviewAlternatives: Boolean,
)
