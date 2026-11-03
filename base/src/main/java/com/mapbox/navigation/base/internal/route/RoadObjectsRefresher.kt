package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.Incident

internal object IncidentsRefresher : RoadObjectsRefresher<Incident, Incident.Builder>(
    { this.toBuilder() },
    { this.build() },
    { this.geometryIndexStart() },
    { this.geometryIndexEnd() },
    { this.geometryIndexStart(it) },
    { this.geometryIndexEnd(it) },
)

internal object ClosuresRefresher : RoadObjectsRefresher<Closure, Closure.Builder>(
    { this.toBuilder() },
    { this.build() },
    { this.geometryIndexStart() },
    { this.geometryIndexEnd() },
    { this.geometryIndexStart(it) },
    { this.geometryIndexEnd(it) },
)

internal open class RoadObjectsRefresher<T, BUILDER>(
    private val toBuilder: T.() -> BUILDER,
    private val build: BUILDER.() -> T,
    private val startIndexExtractor: T.() -> Int?,
    private val endIndexExtractor: T.() -> Int?,
    private val startIndexSetter: BUILDER.(Int) -> BUILDER,
    private val endIndexSetter: BUILDER.(Int) -> BUILDER,
) {

    fun getRefreshedRoadObjects(
        oldRoadObjects: List<T>?,
        newRoadObjects: List<T>?,
        startingLegGeometryIndex: Int,
        lastRefreshLegGeometryIndex: Int,
    ): List<T> {
        val old = oldRoadObjects.orEmpty()
        // A placed object is tested on its own, as the native route merge does: an object
        // before the range is kept even when one that overlaps the range is listed ahead of it.
        // An object without geometry indices has no place, so it is kept when it is listed in
        // the leading run of before-range objects or the trailing run of after-range ones, and
        // twice when it is listed in both.
        val leadingRunSize = old.indexOfFirst { !it.isBefore(startingLegGeometryIndex) }
            .takeIf { it >= 0 } ?: old.size
        val trailingRunStart = old.indexOfLast { !it.isAfter(lastRefreshLegGeometryIndex) } + 1
        val prevRoadObjects = old.filterIndexed { index, roadObject ->
            if (roadObject.hasNoGeometryIndex()) {
                index < leadingRunSize
            } else {
                roadObject.isBefore(startingLegGeometryIndex)
            }
        }
        val refreshedRoadObjects = newRoadObjects?.map { roadObject ->
            roadObject.toBuilder()
                .apply {
                    adjustedIndex(startingLegGeometryIndex, roadObject.startIndexExtractor())?.let {
                        startIndexSetter(it)
                    }
                    adjustedIndex(startingLegGeometryIndex, roadObject.endIndexExtractor())?.let {
                        endIndexSetter(it)
                    }
                }
                .build()
        }.orEmpty()
        val nextRoadObjects = old.filterIndexed { index, roadObject ->
            if (roadObject.hasNoGeometryIndex()) {
                index >= trailingRunStart
            } else {
                roadObject.isAfter(lastRefreshLegGeometryIndex)
            }
        }
        return prevRoadObjects + refreshedRoadObjects + nextRoadObjects
    }

    private fun T.isBefore(startingLegGeometryIndex: Int): Boolean {
        val endIndex = endIndexExtractor()
        if (endIndex != null) {
            return endIndex < startingLegGeometryIndex
        }
        val startIndex = startIndexExtractor()
        return startIndex == null || startIndex < startingLegGeometryIndex
    }

    private fun T.isAfter(lastRefreshLegGeometryIndex: Int): Boolean {
        val startIndex = startIndexExtractor()
        if (startIndex != null) {
            return startIndex > lastRefreshLegGeometryIndex
        }
        val endIndex = endIndexExtractor()
        return endIndex == null || endIndex > lastRefreshLegGeometryIndex
    }

    private fun T.hasNoGeometryIndex(): Boolean =
        startIndexExtractor() == null && endIndexExtractor() == null

    private fun adjustedIndex(offsetIndex: Int, originalIndex: Int?): Int? {
        return originalIndex?.let { offsetIndex + it }
    }
}
