package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.fasterroute.Log.Companion.FASTER_ROUTE_LOG_CATEGORY
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.utils.internal.logD

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
            logD(
                "New routes were set, resetting tracker state",
                FASTER_ROUTE_LOG_CATEGORY
            )
            rejectedRoutesTracker.clean()
            rejectedRoutesTracker.addRejectedRoutes(fasterAlternatives)
        }
        if (update.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
            val fasterAlternativesIdsLog = fasterAlternatives.entries
                .joinToString(separator = ", ") { "${it.value.id}(${it.key})" }
            logD(
                "considering following routes as a faster alternative: $fasterAlternativesIdsLog",
                FASTER_ROUTE_LOG_CATEGORY
            )
            val untracked = rejectedRoutesTracker.findUntrackedAlternatives(fasterAlternatives)
            logD(
                "following routes are not similar to already rejected: " +
                    untracked.joinToString(separator = ", ") {
                        "${it.id}(${metadataMap[it.id]!!.alternativeId})"
                    },
                FASTER_ROUTE_LOG_CATEGORY
            )
            val fasterAlternative = untracked.minByOrNull {
                metadataMap[it.id]!!.infoFromStartOfPrimary.duration
            } ?: return FasterRouteResult.NoFasterRoad
            val primaryRouteDuration = update.navigationRoutes.first().directionsRoute.duration()
            val fasterAlternativeRouteDuration = metadataMap[fasterAlternative.id]!!
                .infoFromStartOfPrimary.duration
            val fasterThanPrimary = primaryRouteDuration - fasterAlternativeRouteDuration
            logD(
                "route ${fasterAlternative.id} is faster then primary by $fasterThanPrimary",
                FASTER_ROUTE_LOG_CATEGORY
            )
            return FasterRouteResult.NewFasterRoadFound(
                fasterAlternative,
                fasterThanPrimary = fasterThanPrimary,
                alternativeId = metadataMap[fasterAlternative.id]!!.alternativeId
            )
        }
        return FasterRouteResult.NoFasterRoad
    }

    fun fasterRouteDeclined(alternativeId: Int, route: NavigationRoute) {
        rejectedRoutesTracker.addRejectedRoutes(mapOf(alternativeId to route))
    }
}

internal sealed class FasterRouteResult {
    object NoFasterRoad : FasterRouteResult()
    data class NewFasterRoadFound(
        val route: NavigationRoute,
        val fasterThanPrimary: Double,
        val alternativeId: Int
    ) : FasterRouteResult()
}
