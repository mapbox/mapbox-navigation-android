package com.mapbox.navigation.utils.timer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapboxTimerTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun start() = runBlocking {
        var lambdaCalled = false
        val testLambda = {  lambdaCalled = true }

        MapboxTimer(0L, testLambda).start()
        delay(10)

        assertTrue(lambdaCalled)
    }

    @Test
    fun stop() = runBlocking {
        var counter = 0
        val testLambda = { counter += 1 }

        val timer = MapboxTimer(100L, testLambda)
        timer.start()
        delay(150)
        timer.stop()
        delay(150)

        assertEquals(1, counter)
    }
}