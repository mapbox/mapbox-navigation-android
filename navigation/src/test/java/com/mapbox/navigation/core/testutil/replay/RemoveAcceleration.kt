package com.mapbox.navigation.core.testutil.replay

import junit.framework.Assert.assertEquals
import org.junit.Test

/**
 * This function works with list of speed updates, for example [0, 1, 2, 3, 3, 3, 3, 2, 1, 0 ].
 * An object starts moving from 0 speed, slowly accelerates to 3, moves with constant speed 3,
 * and slowing down until it stops.
 *
 * Purpose of this function is to remove initial and final acceleration to let you analyse speed
 * along a way.
 * Example:
 * [0, 1, 2, 3, 3, 3, 3, 2, 1, 0 ] -> [3, 3, 3, 3]
 */
fun List<Double>.removeAccelerationAndBrakingSpeedUpdates(): List<Double> {
    val accelerationEndsOn = findEndOfInitialAcceleration()
    val brakingStartsOn = findBeginningOfBraking()
    return take(brakingStartsOn + 1)
        .drop(accelerationEndsOn)
}

private fun List<Double>.findEndOfInitialAcceleration(): Int {
    var accelerationEndsOn = 0
    for (i in 1 until this.size) {
        if (this[i] <= this[i - 1]) {
            accelerationEndsOn = i - 1
            break
        }
    }
    return accelerationEndsOn
}

private fun List<Double>.findBeginningOfBraking(): Int {
    var brakingStartsOn = 0
    for (i in this.size - 1 downTo 2) {
        if (this[i] >= this[i - 1]) {
            brakingStartsOn = i
            break
        }
    }
    return brakingStartsOn
}

class RemoveAccelerationTest {
    @Test
    fun `acceleration and braking is removed from example route`() {
        val speedUpdates = listOf(0.0, 1.0, 2.0, 3.0, 3.0, 3.0, 3.0, 2.0, 1.0, 0.0)
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            listOf(3.0, 3.0, 3.0, 3.0),
            result,
        )
    }

    @Test
    fun `constant speed list doesn't change`() {
        val speedUpdates = listOf(5.0, 5.0, 5.0, 5.0, 5.0)
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            speedUpdates,
            result,
        )
    }

    @Test
    fun `empty list doesn't change`() {
        val speedUpdates = listOf<Double>()
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            speedUpdates,
            result,
        )
    }

    @Test
    fun `stop in the middle of a route stays`() {
        val speedUpdates = listOf(1.0, 3.0, 5.0, 5.0, 5.0, 2.0, 0.0, 2.0, 5.0, 5.0, 3.0, 1.0)
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            listOf(5.0, 5.0, 5.0, 2.0, 0.0, 2.0, 5.0, 5.0),
            result,
        )
    }

    @Test
    fun `only acceleration is removed when there is no breaking`() {
        val speedUpdates = listOf(1.0, 3.0, 5.0, 4.0, 5.0, 5.0, 5.0)
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            listOf(5.0, 4.0, 5.0, 5.0, 5.0),
            result,
        )
    }

    @Test
    fun `only breaking is removed when there is no initial acceleration`() {
        val speedUpdates = listOf(5.0, 5.0, 5.0, 4.0, 5.0, 3.0, 1.0)
        val result = speedUpdates.removeAccelerationAndBrakingSpeedUpdates()
        assertEquals(
            listOf(5.0, 5.0, 5.0, 4.0, 5.0),
            result,
        )
    }
}
