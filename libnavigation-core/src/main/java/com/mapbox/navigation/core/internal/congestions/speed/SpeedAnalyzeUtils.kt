package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.update
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import kotlin.math.min

internal fun restoreTraffic(
    route: NavigationRoute,
    congestionNumericOverride: CongestionNumericOverride,
): NavigationRoute {
    return route.update(
        directionsRouteBlock = {
            this.toBuilder()
                .legs(
                    legs()?.mapIndexed { index, routeLeg ->
                        if (index == congestionNumericOverride.legIndex) {
                            val congestionNumeric =
                                routeLeg.annotation()?.congestionNumeric()
                            val updatedCongestions = congestionNumeric?.toMutableList<Int?>()
                                ?: mutableListOf()
                            if (congestionNumeric != null) {
                                val startIndex = congestionNumericOverride.startIndex
                                val length = congestionNumericOverride.length
                                for (i in startIndex until startIndex + length) {
                                    updatedCongestions[i] =
                                        congestionNumericOverride.originalCongestionNumeric?.get(
                                            i - congestionNumericOverride.startIndex,
                                        )
                                            ?: updatedCongestions[i]
                                }
                            }

                            routeLeg.toBuilder()
                                .annotation(
                                    routeLeg
                                        .annotation()
                                        ?.toBuilder()
                                        ?.congestionNumeric(updatedCongestions)
                                        ?.build(),
                                )
                                .build()
                        } else {
                            routeLeg
                        }
                    },
                ).build()
        },
        waypointsBlock = {
            this
        },
        overriddenTraffic = null,
    )
}

internal fun updateTraffic(
    route: NavigationRoute,
    routeLegProgress: RouteLegProgress,
    expectedCongestion: Int,
    geometryLengthToUpdateTrafficNear: Int,
    geometryLengthToUpdateTrafficFar: Int,
    trafficUpdateLimitIndex: Int?,
    shouldKeepOriginalTraffic: Boolean,
    transformNearFunction: (currentValue: Int?, expectedValue: Int) -> Int,
    transformFarFunction: (currentValue: Int?, expectedValue: Int) -> Int,
): NavigationRoute {
    val startIndex = routeLegProgress.geometryIndex
    val endIndex = minOf(
        startIndex + geometryLengthToUpdateTrafficNear +
            geometryLengthToUpdateTrafficFar,
        trafficUpdateLimitIndex ?: Int.MAX_VALUE,
    )
    val originCongestionNumeric =
        routeLegProgress.routeLeg?.annotation()?.congestionNumeric()?.let {
            it.subList(startIndex, endIndex.coerceIn(startIndex..it.size))
        }
            .orEmpty()
    val overriddenTraffic = CongestionNumericOverride(
        routeLegProgress.legIndex,
        startIndex,
        originCongestionNumeric.size,
        if (shouldKeepOriginalTraffic) originCongestionNumeric else null,
    )
    val result = route.update(
        directionsRouteBlock = {
            this.toBuilder()
                .legs(
                    legs()?.mapIndexed { index, routeLeg ->
                        if (index == routeLegProgress.legIndex) {
                            routeLeg.toBuilder()
                                .annotation(
                                    routeLeg.annotation()?.let { legAnnotation ->
                                        val nearCongestionAheadIndexLimit =
                                            routeLegProgress.geometryIndex +
                                                geometryLengthToUpdateTrafficNear
                                        val farCongestionAheadIndexLimit =
                                            nearCongestionAheadIndexLimit +
                                                geometryLengthToUpdateTrafficFar
                                        legAnnotation.toBuilder()
                                            .congestionNumeric(
                                                transformCongestions(
                                                    legAnnotation,
                                                    routeLegProgress,
                                                    expectedCongestion,
                                                    trafficUpdateLimitIndex ?: Int.MAX_VALUE,
                                                    nearCongestionAheadIndexLimit,
                                                    farCongestionAheadIndexLimit,
                                                    transformNearFunction,
                                                    transformFarFunction,
                                                ),
                                            )
                                            .build()
                                    },
                                )
                                .build()
                        } else {
                            routeLeg
                        }
                    },
                )
                .build()
        },
        waypointsBlock = {
            this
        },
        overriddenTraffic = overriddenTraffic,
    )
    return result
}

private fun transformCongestions(
    legAnnotation: LegAnnotation,
    routeLegProgress: RouteLegProgress,
    expectedCongestionNear: Int,
    trafficUpdateLimit: Int,
    nearCongestionAheadIndexLimit: Int,
    farCongestionAheadIndexLimit: Int,
    transformNearFunction: (currentValue: Int?, expectedValue: Int) -> Int,
    transformFarFunction: (currentValue: Int?, expectedValue: Int) -> Int,
): List<Int>? {
    val currentCongestions = legAnnotation.congestionNumeric() ?: return null
    val updatedCongestions = currentCongestions.toMutableList()
    var lastNearCongestion = 0
    for (index in 0 until min(updatedCongestions.size, trafficUpdateLimit)) {
        val value = currentCongestions[index]
        val updatedValue = when (index) {
            in routeLegProgress.geometryIndex until nearCongestionAheadIndexLimit -> {
                val result = transformNearFunction(
                    value,
                    expectedCongestionNear,
                )
                lastNearCongestion = result
                result
            }

            in nearCongestionAheadIndexLimit until farCongestionAheadIndexLimit -> {
                transformFarFunction(value, lastNearCongestion)
            }

            else -> value
        }
        updatedCongestions[index] = updatedValue
    }
    return updatedCongestions
}
