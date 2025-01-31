package com.mapbox.navigation.base.internal

import androidx.annotation.RestrictTo

/**
 * Represents the data about what line segment of the leg's congestionNumeric annotation was
 * overridden
 *
 * @param legIndex the leg index of the route where congestionNumeric is overridden
 * @param startIndex the geometry index where override starts from
 * @param length the length of overridden segment
 * @param originalCongestionNumeric the original segment of annotation came from the backend
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class CongestionNumericOverride(
    val legIndex: Int,
    val startIndex: Int,
    val length: Int,
    val originalCongestionNumeric: List<Int?>?,
)
