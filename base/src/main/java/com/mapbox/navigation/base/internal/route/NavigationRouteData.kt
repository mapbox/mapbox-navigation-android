package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.navigation.base.route.NavigationRoute

/**
 * Represents data which is preserved in [NavigationRoute] object.
 * In ideal world it should not exist as it's lost during transition via
 * native layer.
 * TODO: https://mapbox.atlassian.net/browse/NAVAND-6774
 */
internal data class NavigationRouteData(
    val unavoidableClosures: List<List<Closure>>,
    val expirationTimeElapsedSeconds: Long?,
)
