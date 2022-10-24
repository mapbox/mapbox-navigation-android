package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.fasterroute.Log.Companion.FASTER_ROUTE_LOG_CATEGORY
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.utils.internal.logD
import java.lang.Double.max

// Warning: doesn't work yet
internal class StreetPenaltyFasterRouteTrackerCore : FasterRouteTrackerCore {

    private var primaryRouteDuration = 0.0
    private val primaryRouteStreetNames = mutableSetOf<String>()
    private val rejectedStreets = mutableMapOf<String, Double>()

    override fun fasterRouteDeclined(alternativeId: Int, route: NavigationRoute) {

    }

    override suspend fun findFasterRouteInUpdate(
        update: RoutesUpdatedResult,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): FasterRouteResult {
        when (update.reason) {
            RoutesExtra.ROUTES_UPDATE_REASON_NEW -> {
                recordPrimaryRoute(update.navigationRoutes[0])
                for (i in 1 until update.navigationRoutes.size) {
                    val alternative = update.navigationRoutes[i]
                    val metadata = alternativeRoutesMetadata.first { it.navigationRoute == alternative }
                    if (metadata.infoFromStartOfPrimary.duration < primaryRouteDuration) {
                        rejectRoute(metadata)
                    }
                }
            }
            RoutesExtra.ROUTES_UPDATE_REASON_REFRESH, RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE -> {
                val primary = update.navigationRoutes.first()
                update.navigationRoutes.drop(1).forEach {
                    val penalty = calculatePenalty(it)

                    val alternative = alternativeRoutesMetadata.first {
                            metadata -> metadata.navigationRoute == it
                    }
                    logD(FASTER_ROUTE_LOG_CATEGORY) {
                        "penalty for ${it.id}(${alternative.alternativeId}) is $penalty"
                    }
                    if (penalty + alternative.infoFromStartOfPrimary.duration < primary.directionsRoute.duration()) {
                        return FasterRouteResult.NewFasterRouteFound(
                            it,
                            primary.directionsRoute.duration() - alternative.infoFromStartOfPrimary.duration,
                            alternative.alternativeId
                        )
                    }
                }
            }
        }
        return FasterRouteResult.NoFasterRoute
    }

    private fun rejectRoute(alterenative: AlternativeRouteMetadata) {
        val streetsToDistances = streetNamesToDistances(alterenative.navigationRoute)
        val streetsToPenalize = mutableListOf<Pair<String, Double>>()
        streetsToDistances.entries.forEach { (street, distance) ->
            if (!primaryRouteStreetNames.contains(street)) {
                streetsToPenalize.add(Pair(street, distance))
            }
        }

        val timeDiff = primaryRouteDuration - alterenative.infoFromStartOfPrimary.duration
        val penalty = timeDiff + timeDiff * 0.2
        val distanceToPenalize = streetsToPenalize.sumOf { it.second }
        streetsToPenalize.forEach { (street, distance) ->
            val streetPercent = distance / distanceToPenalize
            rejectedStreets[street] = max(penalty * streetPercent, rejectedStreets[street] ?: 0.0)
        }
    }

    private fun recordPrimaryRoute(primary: NavigationRoute) {
        primaryRouteStreetNames.clear()
        primary.directionsRoute.legs().orEmpty()
            .flatMap { it.steps() ?: emptyList() }
            .forEach {
                it.name()?.let {
                    primaryRouteStreetNames.add(it)
                }
            }
        primaryRouteDuration = primary.directionsRoute.duration()
        rejectedStreets.clear()
    }

    private fun calculatePenalty(navigationRoute: NavigationRoute): Double {
        return navigationRoute.directionsRoute.legs().orEmpty()
            .flatMap { it.steps() ?: emptyList() }
            .map { it.name() }
            .filterNotNull()
            .distinct()
            .sumOf {
                rejectedStreets[it] ?: 0.0
            }
    }
}