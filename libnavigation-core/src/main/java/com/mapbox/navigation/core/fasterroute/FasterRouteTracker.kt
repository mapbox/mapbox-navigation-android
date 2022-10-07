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
        maximumGeometrySimilarity = options.maxGeometrySimilarityToRejectedAlternatives
    )

    fun fasterRouteDeclined(alternativeId: Int, route: NavigationRoute) {
        rejectedRoutesTracker.addRejectedRoutes(mapOf(alternativeId to route))
    }

    suspend fun findFasterRouteInUpdate(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        val metadataByRouteId = mutableMapOf<String, AlternativeRouteMetadata>().apply {
            alternativeRoutesMetadata.forEach { this[it.navigationRoute.id] = it }
        }
        return when (update.reason) {
            RoutesExtra.ROUTES_UPDATE_REASON_NEW, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE -> {
                onNewRoutes(alternativeRoutesMetadata)
                FasterRouteResult.NoFasterRoute
            }
            RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE ->
                onAlternativesChanged(alternativeRoutesMetadata, update, metadataByRouteId)
            else -> FasterRouteResult.NoFasterRoute
        }
    }

    private suspend fun onAlternativesChanged(
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        update: RoutesUpdatedResult,
        metadataByRouteId: Map<String, AlternativeRouteMetadata>
    ): FasterRouteResult {
        val fasterAlternatives: Map<Int, NavigationRoute> = findFasterAlternatives(
            alternativeRoutesMetadata,
            update
        )
        if (fasterAlternatives.isEmpty()) {
            logD(FASTER_ROUTE_LOG_CATEGORY) {
                "no alternatives which are faster then primary route found"
            }
            return FasterRouteResult.NoFasterRoute
        }
        logPotentialFasterRoutes(fasterAlternatives)

        val untrackedAlternatives = rejectedRoutesTracker.findUntrackedAlternatives(
            fasterAlternatives
        )
        if (untrackedAlternatives.isEmpty()) {
            logD(FASTER_ROUTE_LOG_CATEGORY) {
                "all faster alternatives are similar to already rejected"
            }
            return FasterRouteResult.NoFasterRoute
        }
        logUntrackedFasterRoutes(untrackedAlternatives, metadataByRouteId)

        return findTheFastestAlternative(untrackedAlternatives, metadataByRouteId, update)
    }

    private fun findTheFastestAlternative(
        untracked: List<NavigationRoute>,
        metadataByRouteId: Map<String, AlternativeRouteMetadata>,
        update: RoutesUpdatedResult
    ): FasterRouteResult {
        val fasterAlternative = untracked.minByOrNull {
            metadataByRouteId[it.id]!!.infoFromStartOfPrimary.duration
        } ?: return FasterRouteResult.NoFasterRoute
        val primaryRouteDuration = update.navigationRoutes.first().directionsRoute.duration()
        val fasterAlternativeRouteDuration = metadataByRouteId[fasterAlternative.id]!!
            .infoFromStartOfPrimary.duration
        val fasterThanPrimary = primaryRouteDuration - fasterAlternativeRouteDuration
        logD(
            "route ${fasterAlternative.id} is faster than primary by $fasterThanPrimary",
            FASTER_ROUTE_LOG_CATEGORY
        )
        return FasterRouteResult.NewFasterRouteFound(
            fasterAlternative,
            fasterThanPrimaryBy = fasterThanPrimary,
            alternativeId = metadataByRouteId[fasterAlternative.id]!!.alternativeId
        )
    }

    private fun findFasterAlternatives(
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        update: RoutesUpdatedResult
    ): MutableMap<Int, NavigationRoute> {
        return mutableMapOf<Int, NavigationRoute>()
            .apply {
                alternativeRoutesMetadata
                    .filter {
                        it.infoFromStartOfPrimary.duration < update.navigationRoutes.first()
                            .directionsRoute.duration()
                    }
                    .forEach { this[it.alternativeId] = it.navigationRoute }
            }
    }

    private fun onNewRoutes(alternativeRoutesMetadata: List<AlternativeRouteMetadata>) {
        val routeByAlternativeId: Map<Int, NavigationRoute> = mutableMapOf<Int, NavigationRoute>()
            .apply {
                alternativeRoutesMetadata.forEach {
                    this[it.alternativeId] = it.navigationRoute
                }
            }
        logD(
            "New routes were set, resetting tracker state",
            FASTER_ROUTE_LOG_CATEGORY
        )
        rejectedRoutesTracker.clean()
        rejectedRoutesTracker.addRejectedRoutes(routeByAlternativeId)
    }

    private fun logUntrackedFasterRoutes(
        untracked: List<NavigationRoute>,
        metadataMap: Map<String, AlternativeRouteMetadata>
    ) {
        logD(FASTER_ROUTE_LOG_CATEGORY) {
            "following routes are not similar to already rejected: " +
                untracked.joinToString(separator = ", ") {
                    "${it.id}(${metadataMap[it.id]!!.alternativeId})"
                }
        }
    }

    private fun logPotentialFasterRoutes(fasterAlternatives: Map<Int, NavigationRoute>) {
        val fasterAlternativesIdsLog = fasterAlternatives.entries
            .joinToString(separator = ", ") { "${it.value.id}(${it.key})" }
        logD(FASTER_ROUTE_LOG_CATEGORY) {
            "considering following routes as a faster alternative: $fasterAlternativesIdsLog"
        }
    }
}

internal sealed class FasterRouteResult {
    object NoFasterRoute : FasterRouteResult()
    data class NewFasterRouteFound(
        val route: NavigationRoute,
        val fasterThanPrimaryBy: Double,
        val alternativeId: Int
    ) : FasterRouteResult()
}
