package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.createRouteRefreshMetadata
import com.mapbox.navigation.base.internal.route.updateOrNull
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import java.util.Date

internal class ExpiringDataRemover(
    private val localDateProvider: () -> Date,
) {

    fun removeExpiringDataFromRoutesProgressData(
        routesRefresherResult: RoutesRefresherResult,
    ): RoutesRefresherResult {
        val primaryResult = routesRefresherResult.primaryRouteRefresherResult
        val primaryRoute = removeExpiringDataFromRoute(
            primaryResult.route,
            primaryResult.routeProgressData.legIndex,
        )
        val alternativeRoutesData = routesRefresherResult.alternativesRouteRefresherResults.map {
            val updatedRoute = removeExpiringDataFromRoute(
                it.route,
                it.routeProgressData?.legIndex ?: 0,
            )
            it.copy(
                route = updatedRoute ?: it.route,
                wasRouteUpdated = updatedRoute != null,
            )
        }
        return RoutesRefresherResult(
            primaryResult.copy(
                route = primaryRoute ?: primaryResult.route,
                wasRouteUpdated = primaryRoute != null,
            ),
            alternativeRoutesData,
        )
    }

    /**
     * Returns the route without expired data, or `null` when the update can't be applied,
     * for example for routes backed by a native route object.
     */
    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        currentLegIndex: Int,
    ): NavigationRoute? {
        val routeLegs = route.directionsRoute.legs()
        val directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute = {
            toBuilder().legs(
                routeLegs?.mapIndexed { legIndex, leg ->
                    val legHasAlreadyBeenPassed = legIndex < currentLegIndex
                    if (legHasAlreadyBeenPassed) {
                        leg
                    } else {
                        removeExpiredDataFromLeg(leg)
                    }
                },
            ).build()
        }
        return route.updateOrNull(
            directionsRouteBlock = directionsRouteBlock,
            waypointsBlock = { this },
            routeRefreshMetadata = createRouteRefreshMetadata(isUpToDate = false),
        )
    }

    private fun removeExpiredDataFromLeg(leg: RouteLeg): RouteLeg {
        val oldAnnotation = leg.annotation()
        return leg.toBuilder()
            .annotation(
                oldAnnotation?.let { nonNullOldAnnotation ->
                    nonNullOldAnnotation.toBuilder()
                        .congestion(nonNullOldAnnotation.congestion()?.map { "unknown" })
                        .congestionNumeric(nonNullOldAnnotation.congestionNumeric()?.map { null })
                        .build()
                },
            )
            .incidents(
                leg.incidents()?.filter {
                    val parsed = parseISO8601DateToLocalTimeOrNull(it.endTime())
                        ?: return@filter true
                    val currentDate = localDateProvider()
                    parsed > currentDate
                },
            )
            .build()
    }
}
