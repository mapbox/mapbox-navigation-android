package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata

internal class PenaltyFasterRouteTrackerCore : FasterRouteTrackerCore {
    override fun fasterRouteDeclined(alternativeId: Int, route: NavigationRoute) {

    }

    override suspend fun findFasterRouteInUpdate(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        return FasterRouteResult.NoFasterRoute
    }
}