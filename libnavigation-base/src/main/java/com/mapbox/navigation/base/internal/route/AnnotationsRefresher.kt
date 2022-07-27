package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.MaxSpeed
import kotlin.math.min

internal object AnnotationsRefresher {

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
            0,
        ) { congestionNumeric() }
        val congestion = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
            "unknown"
        ) { congestion() }
        val distance = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
            0.0
        ) { distance() }
        val duration = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
            0.0
        ) { duration() }
        val speed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
            0.0
        ) { speed() }
        val maxSpeed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            legGeometryIndex,
            MaxSpeed.builder().build()
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
        defaultValue: T,
        propertyExtractor: LegAnnotation.() -> List<T>?,
    ): List<T>? {
        val newProperty = newAnnotation.propertyExtractor()
        if (newProperty == null) {
            return null
        }
        val oldProperty = oldAnnotation?.propertyExtractor()
        return (oldProperty?.mutableTake(endIndex) ?: mutableListOf()).apply {
            repeat(endIndex - size) { add(defaultValue) }
            addAll(newProperty)
        }
    }

    private fun <T> List<T>.mutableTake(n: Int): MutableList<T> {
        return mutableListOf<T>().also {
            for (index in 0 until min(n, size)) { it.add(get(index)) }
        }
    }

}
