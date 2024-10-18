package com.mapbox.navigation.core.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.route.createRouteRefreshMetadata
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.internal.time.parseISO8601DateToLocalTimeOrNull
import com.mapbox.navigation.base.route.NavigationRoute
import java.util.Date

internal class ExpiringDataRemover(
    private val localDateProvider: () -> Date,
) {

    fun removeExpiringDataFromRoutesProgressData(
        routesRefresherResult: RoutesRefresherResult,
    ): RoutesRefresherResult {
        val primaryRoute = removeExpiringDataFromRoute(
            routesRefresherResult.primaryRouteRefresherResult.route,
            routesRefresherResult.primaryRouteRefresherResult.routeProgressData.legIndex,
        )
        val alternativeRoutesData = routesRefresherResult.alternativesRouteRefresherResults.map {
            RouteRefresherResult(
                removeExpiringDataFromRoute(it.route, it.routeProgressData?.legIndex ?: 0),
                it.routeProgressData,
                it.status,
            )
        }
        return RoutesRefresherResult(
            RouteRefresherResult(
                primaryRoute,
                routesRefresherResult.primaryRouteRefresherResult.routeProgressData,
                routesRefresherResult.primaryRouteRefresherResult.status,
            ),
            alternativeRoutesData,
        )
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    private fun removeExpiringDataFromRoute(
        route: NavigationRoute,
        currentLegIndex: Int,
    ): NavigationRoute {
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
        return route.update(
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
