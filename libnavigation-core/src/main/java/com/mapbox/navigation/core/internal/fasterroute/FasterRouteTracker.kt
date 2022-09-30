package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata

class FasterRouteTracker(
    maximumAcceptedSimilarity: Double
) {

    private val rejectedRoutesTracker = RejectedRoutesTracker(
        minimumGeometrySimilarity = maximumAcceptedSimilarity
    )

    fun routesUpdated(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        val metadataMap: Map<String, AlternativeRouteMetadata> = mutableMapOf<String, AlternativeRouteMetadata>().apply {
            alternativeRoutesMetadata.forEach { this[it.navigationRoute.id] = it }
        }

        val alternatives:Map<Int, NavigationRoute> = mutableMapOf<Int, NavigationRoute>().apply {
            alternativeRoutesMetadata.forEach { this[it.alternativeId] = it.navigationRoute }
        }

        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
            rejectedRoutesTracker.trackAlternatives(alternatives)
        }
        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
            val untracked = rejectedRoutesTracker.trackAlternatives(alternatives).untracked
            val fasterRoute = untracked.minByOrNull { metadataMap[it.id]!!.infoFromStartOfPrimary.duration }
                ?: return FasterRouteResult.NoFasterRoad
            val fasterThanPrimary =  update.navigationRoutes.first().directionsRoute.duration() - metadataMap[fasterRoute.id]!!.infoFromStartOfPrimary.duration
            if (fasterThanPrimary > 0) {
                return FasterRouteResult.NewFasterRoadFound(
                    fasterRoute,
                    similarityToRejectedAlternative = 0.5,
                    fasterThanPrimary = fasterThanPrimary
                )
            }
        }
        return FasterRouteResult.NoFasterRoad
    }
}

sealed class FasterRouteResult {
    object NoFasterRoad: FasterRouteResult()
    data class NewFasterRoadFound(
        val route: NavigationRoute,
        val similarityToRejectedAlternative: Double,
        val fasterThanPrimary: Double,
    ): FasterRouteResult()
}