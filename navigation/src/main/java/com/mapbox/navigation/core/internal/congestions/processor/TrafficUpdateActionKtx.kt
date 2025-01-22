package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.trip.model.RouteLegProgress

internal fun getUpcomingCongestion(
    currentLegProgress: RouteLegProgress,
    limitGeometry: Int,
): List<Int?> {
    val congestionNumeric = currentLegProgress
        .routeLeg
        ?.annotation()
        ?.congestionNumeric() ?: return emptyList()

    val toIndex = limitGeometry
        .coerceAtMost(congestionNumeric.lastIndex)

    if (currentLegProgress.geometryIndex > toIndex) {
        return emptyList()
    }

    return congestionNumeric.subList(currentLegProgress.geometryIndex, toIndex)
}
