package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.fasterroute.Log.Companion.FASTER_ROUTE_LOG_CATEGORY
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.withContext

internal class RejectedRoutesTracker(
    val maximumGeometrySimilarity: Double
) {

    private var rejectedAlternatives = mutableMapOf<Int, NavigationRoute>()

    fun addRejectedRoutes(alternatives: Map<Int, NavigationRoute>) {
        for ((alternativeId, alternative) in alternatives) {
            rejectedAlternatives[alternativeId] = alternative
        }
    }

    suspend fun findUntrackedAlternatives(
        alternatives: Map<Int, NavigationRoute>
    ): List<NavigationRoute> {
        val untracked = mutableListOf<NavigationRoute>()
        for ((alternativeId, alternative) in alternatives) {
            if (rejectedAlternatives.containsKey(alternativeId)) {
                continue
            }
            val similarities = withContext(ThreadController.DefaultDispatcher) {
                rejectedAlternatives.values.map {
                    val similarity = calculateGeometrySimilarity(it, alternative)
                    logD(
                        "route ${it.id} has $similarity similarity with ${alternative.id}",
                        FASTER_ROUTE_LOG_CATEGORY
                    )
                    similarity
                }
            }
            val similarity = similarities.maxOrNull() ?: 0.0
            if (similarity < maximumGeometrySimilarity) {
                untracked.add(alternative)
            }
        }
        return untracked
    }

    fun clean() {
        rejectedAlternatives.clear()
    }
}
