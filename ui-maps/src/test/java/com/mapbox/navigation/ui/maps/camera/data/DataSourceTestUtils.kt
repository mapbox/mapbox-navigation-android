package com.mapbox.navigation.ui.maps.camera.data

import com.google.gson.internal.LinkedTreeMap
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert

internal fun <V, D> assertArrays1(
    expected: List<V>,
    actual: List<V>,
    adapter: ArrayTestAdapter<V, D>,
) {
    Assert.assertTrue(expected.size == actual.size)
    expected.forEachIndexed { index, expectedValue ->
        val actualValue = actual[index]
        adapter.assertEqual(expectedValue, actualValue)
    }
}

internal fun <V, D> assertArrays2(
    expected: List<List<V>>,
    actual: List<List<V>>,
    adapter: ArrayTestAdapter<V, D>,
) {
    Assert.assertTrue(expected.size == actual.size)
    expected.forEachIndexed { index, nestedExpected ->
        assertArrays1(nestedExpected, actual[index], adapter)
    }
}

internal fun <V, D> assertArrays3(
    expected: List<List<List<V>>>,
    actual: List<List<List<V>>>,
    adapter: ArrayTestAdapter<V, D>,
) {
    Assert.assertTrue(expected.size == actual.size)
    expected.forEachIndexed { index, nestedExpected ->
        assertArrays2(nestedExpected, actual[index], adapter)
    }
}

internal fun <V, D> decodeArrays1(list: List<D>, adapter: ArrayTestAdapter<V, D>): List<V> {
    return list.map { value ->
        adapter.decode(value)
    }
}

internal fun <V, D> decodeArrays2(
    list: List<List<D>>,
    adapter: ArrayTestAdapter<V, D>,
): List<List<V>> {
    return list.map { nestedList ->
        decodeArrays1(nestedList, adapter)
    }
}

internal fun <V, D> decodeArrays3(
    list: List<List<List<D>>>,
    adapter: ArrayTestAdapter<V, D>,
): List<List<List<V>>> {
    return list.map { nestedList ->
        decodeArrays2(nestedList, adapter)
    }
}

/* below functions are used to update test suite if necessary */

internal fun <V, D> encodeArrays1(list: List<V>, adapter: ArrayTestAdapter<V, D>): String {
    val builder = StringBuilder()
    builder.append("[")
    list.forEachIndexed { index, value ->
        if (index > 0) {
            builder.append(",")
        }
        builder.append(adapter.encode(value))
    }
    builder.append("]")
    return builder.toString()
}

internal fun <V, D> encodeArrays2(list: List<List<V>>, adapter: ArrayTestAdapter<V, D>): String {
    val builder = StringBuilder()
    builder.append("[")
    list.forEachIndexed { index, nestedList ->
        if (index > 0) {
            builder.append(",")
        }
        builder.append(encodeArrays1(nestedList, adapter))
    }
    builder.append("]")
    return builder.toString()
}

internal fun <V, D> encodeArrays3(
    list: List<List<List<V>>>,
    adapter: ArrayTestAdapter<V, D>,
): String {
    val builder = StringBuilder()
    builder.append("[")
    list.forEachIndexed { index, nestedList ->
        if (index > 0) {
            builder.append(",")
        }
        builder.append(encodeArrays2(nestedList, adapter))
    }
    builder.append("]")
    return builder.toString()
}

internal fun routeProgressWith(
    upcomingManeuverType: String,
    distanceToUpcomingManeuver: Float,
): RouteProgress = mockk {
    every { currentLegProgress } returns mockk {
        every { currentStepProgress } returns mockk {
            every { distanceRemaining } returns distanceToUpcomingManeuver
        }
    }
    every { bannerInstructions } returns mockk {
        every { primary() } returns mockk {
            every { type() } returns upcomingManeuverType
        }
    }
}

internal interface ArrayTestAdapter<V, D> {
    fun encode(value: V): String
    fun decode(value: D): V
    fun assertEqual(expected: V, actual: V)
}

internal class PointArrayTestAdapter : ArrayTestAdapter<Point, LinkedTreeMap<String, String>> {
    override fun encode(value: Point): String {
        return value.toJson()
    }

    override fun decode(value: LinkedTreeMap<String, String>): Point {
        val coordinates = value["coordinates"] as ArrayList<Double>
        return Point.fromLngLat(coordinates[0], coordinates[1])
    }

    override fun assertEqual(expected: Point, actual: Point) {
        Assert.assertEquals(expected.longitude(), actual.longitude(), 0.0000001)
        Assert.assertEquals(expected.latitude(), actual.latitude(), 0.0000001)
    }
}

internal class DoubleArrayTestAdapter : ArrayTestAdapter<Double, Double> {
    override fun encode(value: Double): String {
        return value.toString()
    }

    override fun decode(value: Double): Double {
        return value
    }

    override fun assertEqual(expected: Double, actual: Double) {
        Assert.assertEquals(expected, actual, 0.0000001)
    }
}
