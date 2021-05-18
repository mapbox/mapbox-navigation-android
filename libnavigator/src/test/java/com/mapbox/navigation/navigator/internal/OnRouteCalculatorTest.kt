package com.mapbox.navigation.navigator.internal

import com.mapbox.geojson.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnRouteCalculatorTest {

    private val onRouteCalculator = OnRouteCalculator()

    @Test
    fun `should return null when location is off route`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-121.470604, 38.563172),
            Point.fromLngLat(-121.470834, 38.563233),
            Point.fromLngLat(-121.472171, 38.563588),
            Point.fromLngLat(-121.472698, 38.563728),
            Point.fromLngLat(-121.473425, 38.563883),
            Point.fromLngLat(-121.47356, 38.563918),
            Point.fromLngLat(-121.47367, 38.563947),
            Point.fromLngLat(-121.474872, 38.564282),
            Point.fromLngLat(-121.474953, 38.564304),
            Point.fromLngLat(-121.476184, 38.564655),
            Point.fromLngLat(-121.477532, 38.565013),
            Point.fromLngLat(-121.478754, 38.565313),
            Point.fromLngLat(-121.478874, 38.565353),
            Point.fromLngLat(-121.478998, 38.565394),
            Point.fromLngLat(-121.480197, 38.565721),
            Point.fromLngLat(-121.480318, 38.565753),
            Point.fromLngLat(-121.481516, 38.566072),
            Point.fromLngLat(-121.481653, 38.566108),
            Point.fromLngLat(-121.482194, 38.566252),
            Point.fromLngLat(-121.482755, 38.566401),
            Point.fromLngLat(-121.482865, 38.56643),
            Point.fromLngLat(-121.482705, 38.566798),
            Point.fromLngLat(-121.482633, 38.566962),
            Point.fromLngLat(-121.482586, 38.567067),
            Point.fromLngLat(-121.482412, 38.567471),
            Point.fromLngLat(-121.482375, 38.567556),
            Point.fromLngLat(-121.482367, 38.567575),
            Point.fromLngLat(-121.482091, 38.568208),
            Point.fromLngLat(-121.482075, 38.568253),
            Point.fromLngLat(-121.481919, 38.568604),
            Point.fromLngLat(-121.481876, 38.568702),
            Point.fromLngLat(-121.481679, 38.569155),
            Point.fromLngLat(-121.481617, 38.569297),
            Point.fromLngLat(-121.481481, 38.56961),
            Point.fromLngLat(-121.48144, 38.569703),
            Point.fromLngLat(-121.4814, 38.569795),
            Point.fromLngLat(-121.481219, 38.570252),
            Point.fromLngLat(-121.481018, 38.570716),
            Point.fromLngLat(-121.480981, 38.570799),
            Point.fromLngLat(-121.48074, 38.571351),
            Point.fromLngLat(-121.480531, 38.571807),
            Point.fromLngLat(-121.480494, 38.571899),
            Point.fromLngLat(-121.480459, 38.571989),
            Point.fromLngLat(-121.480304, 38.572314),
            Point.fromLngLat(-121.480251, 38.572436),
            Point.fromLngLat(-121.480617, 38.572533),
            Point.fromLngLat(-121.480634, 38.572537),
            Point.fromLngLat(-121.481177, 38.572682),
        )

        assertEquals(
            null,
            onRouteCalculator.indexOfNextLocation(
                Point.fromLngLat(-121.47061190142968, 38.5637445188036),
                coordinates
            )
        )
        assertFalse(
            onRouteCalculator.isOnRoute(
                Point.fromLngLat(-121.47061190142968, 38.5637445188036),
                coordinates
            )
        )
    }

    @Test
    fun `should find position in route with u turns`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-122.393181, 37.758193),
            Point.fromLngLat(-122.393364, 37.760116),
            Point.fromLngLat(-122.393364, 37.760135),
            Point.fromLngLat(-122.39338, 37.760208),
            Point.fromLngLat(-122.393265, 37.760219),
            Point.fromLngLat(-122.393227, 37.760223),
            Point.fromLngLat(-122.392861, 37.760242),
            Point.fromLngLat(-122.390091, 37.760387),
            Point.fromLngLat(-122.389649, 37.760406),
            Point.fromLngLat(-122.389618, 37.760406),
            Point.fromLngLat(-122.389504, 37.760437),
            Point.fromLngLat(-122.389603, 37.760463),
            Point.fromLngLat(-122.389618, 37.760463),
            Point.fromLngLat(-122.390358, 37.760429),
            Point.fromLngLat(-122.390381, 37.760425),
            Point.fromLngLat(-122.390458, 37.760425),
            Point.fromLngLat(-122.390465, 37.760498),
            Point.fromLngLat(-122.390511, 37.760978)
        )

        assertEquals(
            13,
            onRouteCalculator.indexOfNextLocation(
                Point.fromLngLat(-122.390358, 37.760429),
                coordinates
            )
        )
        assertTrue(
            onRouteCalculator.isOnRoute(
                Point.fromLngLat(-122.390358, 37.760429),
                coordinates
            )
        )
    }

    @Test
    fun `should take first match when there are multiple matches`() {
        val coordinates = listOf<Point>(
            Point.fromLngLat(-121.469903, 38.550876),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.470002, 38.551483),
            Point.fromLngLat(-121.469887, 38.551753),
            Point.fromLngLat(-121.470002, 38.551483),
            Point.fromLngLat(-121.470231, 38.550964),
            Point.fromLngLat(-121.470978, 38.551158)
        )

        assertEquals(
            1,
            onRouteCalculator.indexOfNextLocation(
                Point.fromLngLat(-121.470231, 38.550964),
                coordinates
            )
        )
        assertTrue(
            onRouteCalculator.isOnRoute(
                Point.fromLngLat(-121.470231, 38.550964),
                coordinates
            )
        )
    }
}
