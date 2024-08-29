package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Modifier

@RunWith(Parameterized::class)
class LegAnnotationExSizeSingleAnnotationPresentTest(
    private val legAnnotation: LegAnnotation,
    private val expected: Int,
) {

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<Any>> {
            val expectedDataSize = LegAnnotation::class.java.declaredMethods.filter {
                it.name != "toBuilder" && (it.modifiers and Modifier.STATIC) == 0
            }.size
            val singleAnnotationData = listOf<Array<Any>>(
                arrayOf(LegAnnotation.builder().speed(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().duration(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().distance(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().congestion(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().congestionNumeric(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().currentSpeed(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().freeflowSpeed(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().maxspeed(List(5) { null }).build(), 5),
                arrayOf(LegAnnotation.builder().trafficTendency(List(5) { null }).build(), 5),
            )
            assertEquals(expectedDataSize, singleAnnotationData.size)
            return singleAnnotationData
        }
    }

    @Test
    fun annotationIsConsidered() {
        assertEquals(expected, legAnnotation.size())
    }
}
