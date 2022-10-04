package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.ThreadController
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

    suspend fun checkAlternatives(alternatives: Map<Int, NavigationRoute>): CheckAlternativesResult {
        val untracked = mutableListOf<NavigationRoute>()
        for ((alternativeId, alternative) in alternatives) {
            if (rejectedAlternatives.containsKey(alternativeId)) {
                continue
            }
            val similarities = withContext(ThreadController.DefaultDispatcher) {
                rejectedAlternatives.values.map { calculateGeometrySimilarity(it, alternative) }
            }
            val similarity = similarities.maxOrNull() ?: 0.0
            if (similarity < maximumGeometrySimilarity) {
                untracked.add(alternative)
            }
        }
        return CheckAlternativesResult(untracked)
    }

    fun clean() {
        rejectedAlternatives.clear()
    }
}

internal data class CheckAlternativesResult(
    val untracked: List<NavigationRoute>
)