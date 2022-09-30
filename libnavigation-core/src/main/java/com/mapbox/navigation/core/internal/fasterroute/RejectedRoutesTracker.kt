package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute

class RejectedRoutesTracker(
    val minimumGeometrySimilarity: Double
) {

    private var rejectedAlternatives = mutableMapOf<Int, NavigationRoute>()

    fun addRejectedRoutes(alternatives: Map<Int, NavigationRoute>) {
        for ((alternativeId, alternative) in alternatives) {
            rejectedAlternatives[alternativeId] = alternative
        }
    }

    fun checkAlternatives(alternatives: Map<Int, NavigationRoute>): CheckAlternativesResult {
        val untracked = mutableListOf<NavigationRoute>()
        for ((alternativeId, alternative) in alternatives) {
            if (rejectedAlternatives.containsKey(alternativeId)) {
                continue
            }
            val similarities = rejectedAlternatives.values.map { calculateGeometrySimilarity(it, alternative) }
            val similarity = similarities.maxOrNull() ?: 0.0
            if (similarity < minimumGeometrySimilarity) {
                untracked.add(alternative)
            }
        }
        return CheckAlternativesResult(untracked)
    }

    fun clean() {
        rejectedAlternatives.clear()
    }
}

data class CheckAlternativesResult(
    val untracked: List<NavigationRoute>
)