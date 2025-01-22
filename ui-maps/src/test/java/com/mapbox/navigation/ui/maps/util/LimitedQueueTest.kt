package com.mapbox.navigation.ui.maps.util

import org.junit.Assert.assertEquals
import org.junit.Test

internal class LimitedQueueTest {

    @Test
    fun add() {
        val queue = LimitedQueue<Int>(3)

        queue.add(1)
        checkQueue(listOf(1), queue)

        queue.add(2)
        checkQueue(listOf(1, 2), queue)

        queue.add(3)
        checkQueue(listOf(1, 2, 3), queue)

        queue.add(4)
        checkQueue(listOf(2, 3, 4), queue)

        queue.clear()
        checkQueue(emptyList(), queue)

        queue.add(1)
        checkQueue(listOf(1), queue)
    }

    private fun checkQueue(expected: List<Int>, queue: LimitedQueue<Int>) {
        val actual = mutableListOf<Int>()
        queue.forEach { actual.add(it) }
        assertEquals(expected, actual)
    }
}
