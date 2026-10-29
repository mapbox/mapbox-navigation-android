package com.mapbox.navigation.core.internal.router

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRouterRefreshErrorTest {

    @Test
    fun `errors that differ only in permanent are not equal`() {
        val retryable = NavigationRouterRefreshError("failed", refreshTtl = 10)
        val permanent = NavigationRouterRefreshError("failed", refreshTtl = 10, permanent = true)

        assertNotEquals(retryable, permanent)
        assertNotEquals(retryable.hashCode(), permanent.hashCode())
        assertTrue(permanent.toString().contains("permanent=true"))
    }

    @Test
    fun `an error is not permanent by default`() {
        assertEquals(
            NavigationRouterRefreshError("failed", permanent = false),
            NavigationRouterRefreshError("failed"),
        )
    }
}
