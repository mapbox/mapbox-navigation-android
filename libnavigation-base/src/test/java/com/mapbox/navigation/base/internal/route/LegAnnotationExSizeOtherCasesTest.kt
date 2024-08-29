package com.mapbox.navigation.base.internal.route

import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.LegAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LegAnnotationExSizeOtherCasesTest {

    @Test
    fun nullAnnotationThrows() {
        val annotation: LegAnnotation? = null
        assertThrows(IllegalArgumentException::class.java) {
            annotation.size()
        }
    }

    @Test
    fun noAnnotationsThrows() {
        val annotation = LegAnnotation.builder().build()
        assertThrows(IllegalArgumentException::class.java) {
            annotation.size()
        }
    }

    @Test
    fun singleUnrecognizedProperty() {
        val array = JsonArray()
        repeat(5) { array.add(20) }
        val annotation = LegAnnotation.builder()
            .unrecognizedJsonProperties(
                mapOf("some_property" to array),
            )
            .build()
        assertEquals(5, annotation.size())
    }

    @Test
    fun nonArrayUnrecognizedProperty() {
        val array = JsonArray()
        repeat(5) { array.add(20) }
        val annotation = LegAnnotation.builder()
            .unrecognizedJsonProperties(
                mapOf(
                    "a" to JsonPrimitive("b"),
                    "some_property" to array,
                ),
            )
            .build()
        assertEquals(5, annotation.size())
    }

    @Test
    fun multipleAnnotationsPresent() {
        val array = JsonArray()
        repeat(5) { array.add(20) }
        val annotation = LegAnnotation.builder()
            .duration(List(5) { null })
            .distance(List(5) { null })
            .speed(List(5) { null })
            .maxspeed(List(5) { null })
            .currentSpeed(List(5) { null })
            .freeflowSpeed(List(5) { null })
            .trafficTendency(List(5) { null })
            .congestion(List(5) { null })
            .congestionNumeric(List(5) { null })
            .unrecognizedJsonProperties(mapOf("some_property" to array))
            .build()
        assertEquals(5, annotation.size())
    }

    @Test
    fun multipleInconsistentAnnotationsPresent() {
        val array = JsonArray()
        repeat(14) { array.add(20) }
        val annotation = LegAnnotation.builder()
            .duration(List(5) { null })
            .distance(List(6) { null })
            .speed(List(7) { null })
            .maxspeed(List(8) { null })
            .currentSpeed(List(9) { null })
            .freeflowSpeed(List(10) { null })
            .trafficTendency(List(11) { null })
            .congestion(List(12) { null })
            .congestionNumeric(List(13) { null })
            .unrecognizedJsonProperties(mapOf("some_property" to array))
            .build()
        // undefined behaviour: any annotation size can be used
        assertTrue(annotation.size() in 5..14)
    }
}
