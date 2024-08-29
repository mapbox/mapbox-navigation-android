package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.Closure
import com.mapbox.api.directions.v5.models.Incident

internal class IncidentsRefresher : RoadObjectsRefresher<Incident, Incident.Builder>(
    { this.toBuilder() },
    { this.build() },
    { this.geometryIndexStart() },
    { this.geometryIndexEnd() },
    { this.geometryIndexStart(it) },
    { this.geometryIndexEnd(it) },
)

internal class ClosuresRefresher : RoadObjectsRefresher<Closure, Closure.Builder>(
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
        val prevRoadObjects = oldRoadObjects?.takeWhile {
            val endIndex = it.endIndexExtractor()
            if (endIndex != null) {
                endIndex < startingLegGeometryIndex
            } else {
                val startIndex = it.startIndexExtractor()
                startIndex == null || startIndex < startingLegGeometryIndex
            }
        }.orEmpty()
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
        val nextRoadObjects = oldRoadObjects?.takeLastWhile {
            val startIndex = it.startIndexExtractor()
            if (startIndex != null) {
                startIndex > lastRefreshLegGeometryIndex
            } else {
                val endIndex = it.endIndexExtractor()
                endIndex == null || endIndex > lastRefreshLegGeometryIndex
            }
        }.orEmpty()
        return prevRoadObjects + refreshedRoadObjects + nextRoadObjects
    }

    private fun adjustedIndex(offsetIndex: Int, originalIndex: Int?): Int? {
        return originalIndex?.let { offsetIndex + it }
    }
}
