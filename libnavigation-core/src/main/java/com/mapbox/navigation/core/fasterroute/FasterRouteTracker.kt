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

    suspend fun routesUpdated(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        val metadataMap: Map<String, AlternativeRouteMetadata> =
            mutableMapOf<String, AlternativeRouteMetadata>().apply {
                alternativeRoutesMetadata.forEach { this[it.navigationRoute.id] = it }
            }

        val fasterAlternatives: Map<Int, NavigationRoute> = mutableMapOf<Int, NavigationRoute>()
            .apply {
                alternativeRoutesMetadata
                    .filter {
                        it.infoFromStartOfPrimary.duration < update.navigationRoutes.first()
                            .directionsRoute.duration()
                    }
                    .forEach { this[it.alternativeId] = it.navigationRoute }
            }

        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
            rejectedRoutesTracker.clean()
            rejectedRoutesTracker.addRejectedRoutes(fasterAlternatives)
        }
        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
            val untracked = rejectedRoutesTracker.checkAlternatives(fasterAlternatives).untracked
            val fasterAlternative = untracked.minByOrNull {
                metadataMap[it.id]!!.infoFromStartOfPrimary.duration
            } ?: return FasterRouteResult.NoFasterRoad
            val primaryRouteDuration = update.navigationRoutes.first().directionsRoute.duration()
            val fasterAlternativeRouteDuration = metadataMap[fasterAlternative.id]!!
                .infoFromStartOfPrimary.duration
            val fasterThanPrimary = primaryRouteDuration - fasterAlternativeRouteDuration
            if (fasterThanPrimary > 0) {
                return FasterRouteResult.NewFasterRoadFound(
                    fasterAlternative,
                    fasterThanPrimary = fasterThanPrimary
                )
            }
        }
        return FasterRouteResult.NoFasterRoad
    }
}

internal sealed class FasterRouteResult {
    object NoFasterRoad : FasterRouteResult()
    data class NewFasterRoadFound(
        val route: NavigationRoute,
        val fasterThanPrimary: Double,
    ) : FasterRouteResult()
}
