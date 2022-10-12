package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.fasterroute.Log.Companion.FASTER_ROUTE_LOG_CATEGORY
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.withContext

internal class RejectedRoutesTracker(
    val maximumGeometrySimilarity: Double
) {

    private val rejectedAlternatives = mutableMapOf<Int, NavigationRoute>()

    fun addRejectedRoutes(alternatives: Map<Int, NavigationRoute>) {
        rejectedAlternatives.putAll(alternatives)
    }

    suspend fun findUntrackedAlternatives(
        alternatives: Map<Int, NavigationRoute>
    ): List<NavigationRoute> {
        val untracked = mutableListOf<NavigationRoute>()
        for ((alternativeId, alternativeRoute) in alternatives) {
            if (rejectedAlternatives.containsKey(alternativeId)) {
                continue
            }
            val similarities = withContext(ThreadController.DefaultDispatcher) {
                rejectedAlternatives.entries.map { (rejectedRouteAlternativeId, rejectedRoute) ->
                    val similarity = calculateStreetsSimilarity(rejectedRoute, alternativeRoute)
                    logD(
                        "${alternativeRoute.id}($alternativeId) route has " +
                            "similarity $similarity with rejected" +
                            " ${rejectedRoute.id}($rejectedRouteAlternativeId)",
                        FASTER_ROUTE_LOG_CATEGORY
                    )
                    similarity
                }
            }
            val similarity = similarities.maxOrNull() ?: 0.0
            if (similarity < maximumGeometrySimilarity) {
                untracked.add(alternativeRoute)
            }
        }
        return untracked
    }

    fun clean() {
        rejectedAlternatives.clear()
    }
}
