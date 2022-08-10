package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.utils.internal.logW
import kotlin.math.min

internal object AnnotationsRefresher {

    private const val LOG_CATEGORY = "AnnotationsRefresher"

    fun getRefreshedAnnotations(
        oldAnnotation: LegAnnotation?,
        newAnnotation: LegAnnotation?,
        legGeometryIndex: Int
    ): LegAnnotation? {
        if (newAnnotation == null) {
            return null
        }
        val congestionNumeric = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { congestionNumeric() }
        val congestion = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { congestion() }
        val distance = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { distance() }
        val duration = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { duration() }
        val speed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { speed() }
        val maxSpeed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
        ) { maxspeed() }
        // unrecognized properties migrate from new annotation
        return newAnnotation.toBuilder()
            .congestion(congestion)
            .congestionNumeric(congestionNumeric)
            .maxspeed(maxSpeed)
            .distance(distance)
            .duration(duration)
            .speed(speed)
            .build()
    }

    private fun <T> mergeAnnotationProperty(
        oldAnnotation: LegAnnotation?,
        newAnnotation: LegAnnotation,
        endIndex: Int,
        propertyExtractor: LegAnnotation.() -> List<T>?,
    ): List<T>? {
        val newProperty = newAnnotation.propertyExtractor() ?: return null
        val oldProperty = oldAnnotation?.propertyExtractor() ?: return null
        val expectedSize = oldProperty.size
        if (expectedSize < endIndex) {
            logW("Annotations sizes mismatch: index=$endIndex, expected_size=$expectedSize", LOG_CATEGORY)
            return null
        }
        val result = mutableListOf<T>()
        repeat(endIndex) { result.add(oldProperty[it]) }
        repeat(min(expectedSize - endIndex, newProperty.size)) { result.add(newProperty[it]) }
        val filledSize = result.size
        repeat(expectedSize - filledSize) { result.add(oldProperty[it + filledSize]) }

        return result
    }
}
