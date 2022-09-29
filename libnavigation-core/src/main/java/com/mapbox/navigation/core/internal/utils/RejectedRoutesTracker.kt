package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.route.NavigationRoute

class RejectedRoutesTracker(
    val minimumGeometrySimilarity: Double
) {

    private var rejectedAlternatives = mutableMapOf<Int, NavigationRoute>()

    fun trackAlternatives(alternatives: Map<Int, NavigationRoute>): TrackAlternativesResult {
        val untracked = mutableListOf<NavigationRoute>()
        for ((alternativeId, alternative) in alternatives) {
            if (rejectedAlternatives.containsKey(alternativeId)) {
                continue
            }
            val similarity = rejectedAlternatives.values.maxOfOrNull { calculateGeometrySimilarity(it, alternative) } ?: 0.0
            if (similarity < minimumGeometrySimilarity) {
                untracked.add(alternative)
            }
            rejectedAlternatives[alternativeId] = alternative
        }
        return TrackAlternativesResult(untracked)
    }
}

data class TrackAlternativesResult(
    val untracked: List<NavigationRoute>
)