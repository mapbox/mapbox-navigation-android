package com.mapbox.navigation.utils.timer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class MapboxTimerTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @Test
    fun start() = runBlocking {
        var counter = 0
        val testLambda = {  counter += 1 }

        MapboxTimer(100L, testLambda).start()
        delay(220)

        assertEquals(2, counter)
    }

    @Test
    fun stop() = runBlocking {
        var counter = 0
        val testLambda = { counter += 1 }

        val timer = MapboxTimer(100L, testLambda)
        timer.start()
        delay(120L)
        timer.stop()
        delay(200L)

        assertEquals(1, counter)
    }

    @Test
    fun stop_when_timerCanceled_lambdaNotCalled() = runBlocking {
        var lambdaCalled = false
        val testLambda = {  lambdaCalled = true }

        val timer = MapboxTimer(100L, testLambda)
        timer.start()
        timer.stop()
        delay(150)

        assertFalse(lambdaCalled)
    }

    @Test
    fun executeLambda_notCalled_when_startNotCalled() = runBlocking  {
        var lambdaCalled = false
        val testLambda = {  lambdaCalled = true }

        MapboxTimer(0L, testLambda)
        delay(10)

        assertFalse(lambdaCalled)
    }

    @Test
    fun timerNotStartedUntilStartCalled() = runBlocking {
        var counter = 0
        val testLambda = { counter += 1 }

        MapboxTimer(100L, testLambda)

        delay(200L)

        assertEquals(0, counter)
    }

    @Test
    fun multipleStartCalls_doNotExecuteMultipleJobs() = runBlocking {
        var counter = 0
        val testLambda = { counter += 1 }

        val timer = MapboxTimer(100L, testLambda)
        timer.start()
        timer.start()
        timer.start()
        timer.start()
        delay(150L)

        assertEquals(1, counter)
    }
}