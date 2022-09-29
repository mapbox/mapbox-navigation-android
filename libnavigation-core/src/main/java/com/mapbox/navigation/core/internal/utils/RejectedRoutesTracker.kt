package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.base.route.NavigationRoute
import java.util.concurrent.ConcurrentHashMap

class RejectedRoutesTracker {

    private var rejectedAlternatives = mutableMapOf<Int, NavigationRoute>()

    fun trackAlternatives(alternatives: List<NavigationRoute>): TrackAlternativesResult {
        return TrackAlternativesResult(emptyList())
    }
}

data class TrackAlternativesResult(
    val untracked: List<NavigationRoute>
)