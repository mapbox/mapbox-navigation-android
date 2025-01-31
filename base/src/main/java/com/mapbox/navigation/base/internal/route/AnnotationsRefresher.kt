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
        startingLegGeometryIndex: Int,
        startFakeCongestionIndex: Int?,
        fakeCongestionLength: Int? = 0,
    ): LegAnnotation? {
        if (oldAnnotation == null) {
            return null
        }

        val congestionNumeric = mergeAnnotationProperty(
            oldAnnotation,
            newAnnotation,
            startingLegGeometryIndex,
            startOverrideIndex = startFakeCongestionIndex ?: 0,
            overrideLength = fakeCongestionLength ?: 0,
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
                        overrideLength = 0,
                        startOverrideIndex = 0,
                        extractor,
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
        overrideLength: Int = 0,
        startOverrideIndex: Int = 0,
        propertyExtractor: LegAnnotation.() -> List<T>?,
    ): List<T>? {
        val oldProperty = oldAnnotation.propertyExtractor() ?: return null
        val newProperty = newAnnotation?.propertyExtractor().orEmpty()
        val expectedSize = oldProperty.size
        if (expectedSize < startingLegGeometryIndex) {
            logE(
                "Annotations sizes mismatch: " +
                    "index=$startingLegGeometryIndex, expected_size=$expectedSize",
                LOG_CATEGORY,
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

        if (overrideLength > 0) {
            val endOverrideIndex = (startOverrideIndex + overrideLength - 1)
                .coerceAtMost(expectedSize)
            (startOverrideIndex..endOverrideIndex).forEach {
                result[it] = oldProperty[it]
            }
        }

        return result
    }

    private fun List<JsonElement>.toJsonArray(): JsonArray {
        return JsonArray(this.size).also { array ->
            for (item in this) { array.add(item) }
        }
    }

    private fun <T, R> Iterable<T>.associateNonNullValuesWith(block: (T) -> R?): Map<T, R> {
        val map = hashMapOf<T, R>()
        this.forEach { key ->
            block(key)?.also { value -> map[key] = value }
        }
        return map
    }
}
