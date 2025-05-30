package com.mapbox.navigation.core.directions

import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.SetRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.DirectionsSessionRoutes
import com.mapbox.navigation.core.directions.session.IgnoredRoute
import com.mapbox.navigation.core.directions.session.routesPlusIgnored
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.utils.internal.logD

internal class ForkPointPassedObserver(
    private val directionsSession: DirectionsSession,
    private val currentLegIndex: () -> Int,
) : RouteProgressObserver {
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        val allCurrentRoutes = directionsSession.routesPlusIgnored
        if (allCurrentRoutes.isEmpty()) return

        val primary = allCurrentRoutes[0]
        val allCurrentAlternatives = allCurrentRoutes.drop(1)

        val needToHideAlternativesIndices = routeProgress
            .internalAlternativeRouteIndices()
            .filter { it.value.isForkPointPassed }
        val alternativesToHide = allCurrentAlternatives.filter {
            it.id in needToHideAlternativesIndices
        }.map {
            IgnoredRoute(it, REASON_ALTERNATIVE_FORK_POINT_PASSED)
        }
        val alternativesIgnoredForDifferentReason = directionsSession.ignoredRoutes.filter {
            it.reason != REASON_ALTERNATIVE_FORK_POINT_PASSED
        }
        val allIgnoredRoutes = alternativesToHide + alternativesIgnoredForDifferentReason

        val newRoutes = DirectionsSessionRoutes(
            acceptedRoutes = listOf(primary) + allCurrentAlternatives.filter {
                allIgnoredRoutes.none { ignored -> ignored.navigationRoute.id == it.id }
            },
            ignoredRoutes = allIgnoredRoutes,
            setRoutesInfo = SetRoutes.Alternatives(currentLegIndex()),
        )

        when {
            newRoutes.ignoredRoutes == directionsSession.ignoredRoutes &&
                newRoutes.acceptedRoutes == directionsSession.routes -> return
            else -> {
                if (newRoutes.ignoredRoutes.isNotEmpty()) {
                    logD(
                        "Hiding alternatives due to fork point has passed: " +
                            "${newRoutes.ignoredRoutes}",
                        TAG,
                    )
                }

                if (newRoutes.acceptedRoutes != directionsSession.routes) {
                    logD(
                        "Settigns new routes due to fork point changes: " +
                            "${newRoutes.acceptedRoutes}",
                        TAG,
                    )
                }

                directionsSession.setNavigationRoutesFinished(newRoutes)
            }
        }
    }

    companion object {
        private const val TAG = "ForkPointPassedObserver"
        internal const val REASON_ALTERNATIVE_FORK_POINT_PASSED = "Alternative fork point passed"
    }
}
