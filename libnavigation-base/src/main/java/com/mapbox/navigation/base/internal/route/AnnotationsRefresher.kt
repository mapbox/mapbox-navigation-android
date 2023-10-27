package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.navigation.utils.internal.logE
import kotlin.math.min

internal object AnnotationsRefresher {

    private const val LOG_CATEGORY = "AnnotationsRefresher"

    fun getRefreshedAnnotations(
        oldAnnotation: LegAnnotation?,
        newAnnotation: LegAnnotation?,
        startingLegGeometryIndex: Int
    ): LegAnnotation? {
        if (oldAnnotation == null) {
            return null
        }
        val congestionNumeric = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { congestionNumeric() }
        val congestion = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { congestion() }
        val distance = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { distance() }
        val duration = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { duration() }
        val speed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { speed() }
        val maxSpeed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { maxspeed() }
        val freeFlowSpeed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { freeflowSpeed() }
        val currentSpeed = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
        ) { currentSpeed() }
        val unrecognizedProperties = oldAnnotation.unrecognizedPropertiesNames
            .union(newAnnotation?.unrecognizedPropertiesNames ?: emptySet())
            .associateNonNullValuesWith { propertyName ->
                if (
                    oldAnnotation.getUnrecognizedProperty(propertyName)?.isJsonArray == false ||
                    newAnnotation?.getUnrecognizedProperty(propertyName)?.isJsonArray == false
                ) {
                    null
                } else {
                    val extractor = { annotation: LegAnnotation ->
                        annotation.getUnrecognizedProperty(propertyName)?.asJsonArray?.toList()
                    }
                    mergeAnnotationProperty(
                        oldAnnotation,
                        newAnnotation,
                        startingLegGeometryIndex,
                        extractor
                    )?.toJsonArray()
                }
            }.ifEmpty { null }
        return LegAnnotation.builder()
            .unrecognizedJsonProperties(unrecognizedProperties)
            .congestion(congestion)
            .congestionNumeric(congestionNumeric)
            .maxspeed(maxSpeed)
            .distance(distance)
            .duration(duration)
            .speed(speed)
            .freeflowSpeed(freeFlowSpeed)
            .currentSpeed(currentSpeed)
            .build()
    }

    private fun <T> mergeAnnotationProperty(
        oldAnnotation: LegAnnotation,
        newAnnotation: LegAnnotation?,
        startingLegGeometryIndex: Int,
        propertyExtractor: LegAnnotation.() -> List<T>?,
    ): List<T>? {
        val oldProperty = oldAnnotation.propertyExtractor() ?: return null
        val newProperty = newAnnotation?.propertyExtractor() ?: emptyList()
        val expectedSize = oldProperty.size
        if (expectedSize < startingLegGeometryIndex) {
            logE(
                "Annotations sizes mismatch: " +
                    "index=$startingLegGeometryIndex, expected_size=$expectedSize",
                LOG_CATEGORY
            )
            return null
        }
        val result = mutableListOf<T>()
        repeat(startingLegGeometryIndex) { result.add(oldProperty[it]) }
        repeat(min(expectedSize - startingLegGeometryIndex, newProperty.size)) {
            result.add(newProperty[it])
        }
        val filledSize = result.size
        repeat(expectedSize - filledSize) { result.add(oldProperty[it + filledSize]) }

        return result
    }

    private fun List<JsonElement>.toJsonArray(): JsonArray {
        return JsonArray(this.size).also { array ->
            forEach { array.add(it) }
        }
    }

    private fun <T, R> Iterable<T>.associateNonNullValuesWith(block: (T) -> R?): Map<T, R> {
        val map = hashMapOf<T, R>()
        forEach { key ->
            val value = block(key)
            if (value != null) {
                map[key] = value
            }
        }
        return map
    }
}
