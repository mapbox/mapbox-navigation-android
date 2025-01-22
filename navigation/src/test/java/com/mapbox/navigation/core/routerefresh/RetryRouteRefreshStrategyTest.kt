package com.mapbox.navigation.core.routerefresh

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryRouteRefreshStrategyTest {

    @Test
    fun maxRetryCountIsZero() {
        val sut = RetryRouteRefreshStrategy(0)

        assertFalse(sut.shouldRetry())

        sut.onNextAttempt()
        assertFalse(sut.shouldRetry())
    }

    @Test
    fun maxRetryCountIsThree() {
        val sut = RetryRouteRefreshStrategy(3)

        assertTrue(sut.shouldRetry())

        sut.onNextAttempt()
        assertTrue(sut.shouldRetry())

        sut.onNextAttempt()
        assertTrue(sut.shouldRetry())

        sut.onNextAttempt()
        assertFalse(sut.shouldRetry())

        sut.reset()

        assertTrue(sut.shouldRetry())
    }

    @Test
    fun shouldRetryDoesNotChangeState() {
        val sut = RetryRouteRefreshStrategy(1)

        assertTrue(sut.shouldRetry())
        assertTrue(sut.shouldRetry())
    }

    @Test
    fun resetWhenMaxAttemptsCountIsNotReached() {
        val sut = RetryRouteRefreshStrategy(2)
        sut.onNextAttempt()

        sut.reset()

        assertTrue(sut.shouldRetry())

        sut.onNextAttempt()
        assertTrue(sut.shouldRetry())

        sut.onNextAttempt()
        assertFalse(sut.shouldRetry())
    }
}
