package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal class FasterRouteTracker(
    options: FasterRouteOptions
) {

    private val rejectedRoutesTracker = RejectedRoutesTracker(
        maximumGeometrySimilarity = options.maxSimilarityToExistingRoute
    )

    fun routesUpdated(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        val metadataMap: Map<String, AlternativeRouteMetadata> = mutableMapOf<String, AlternativeRouteMetadata>().apply {
            alternativeRoutesMetadata.forEach { this[it.navigationRoute.id] = it }
        }

        val alternatives:Map<Int, NavigationRoute> = mutableMapOf<Int, NavigationRoute>().apply {
            alternativeRoutesMetadata
                .filter { it.infoFromStartOfPrimary.duration < update.navigationRoutes.first().directionsRoute.duration() }
                .forEach { this[it.alternativeId] = it.navigationRoute }
        }

        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
            rejectedRoutesTracker.clean()
            rejectedRoutesTracker.addRejectedRoutes(alternatives)
        }
        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
            val untracked = rejectedRoutesTracker.checkAlternatives(alternatives).untracked
            val fasterRoute = untracked.minByOrNull { metadataMap[it.id]!!.infoFromStartOfPrimary.duration }
                ?: return FasterRouteResult.NoFasterRoad
            val fasterThanPrimary =  update.navigationRoutes.first().directionsRoute.duration() - metadataMap[fasterRoute.id]!!.infoFromStartOfPrimary.duration
            if (fasterThanPrimary > 0) {
                return FasterRouteResult.NewFasterRoadFound(
                    fasterRoute,
                    fasterThanPrimary = fasterThanPrimary
                )
            }
        }
        return FasterRouteResult.NoFasterRoad
    }
}

internal sealed class FasterRouteResult {
    object NoFasterRoad: FasterRouteResult()
    data class NewFasterRoadFound(
        val route: NavigationRoute,
        val fasterThanPrimary: Double,
    ): FasterRouteResult()
}