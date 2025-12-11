package com.mapbox.navigation.core.reroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RerouteStateExTest {

    @Test
    fun `toRerouteState maps Idle to Idle`() {
        val stateV2 = RerouteStateV2.Idle()
        val result = stateV2.toRerouteState()

        assertEquals(RerouteState.Idle, result)
    }

    @Test
    fun `toRerouteState maps FetchingRoute to FetchingRoute`() {
        val stateV2 = RerouteStateV2.FetchingRoute()
        val result = stateV2.toRerouteState()

        assertEquals(RerouteState.FetchingRoute, result)
    }

    @Test
    fun `toRerouteState maps Interrupted to Interrupted`() {
        val stateV2 = RerouteStateV2.Interrupted()
        val result = stateV2.toRerouteState()

        assertEquals(RerouteState.Interrupted, result)
    }

    @Test
    fun `toRerouteState maps Failed with message only`() {
        val message = "Route fetch failed"

        val stateV2 = RerouteStateV2.Failed(message)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.Failed)
        val failedState = result as RerouteState.Failed
        assertEquals(message, failedState.message)
        assertNull(failedState.throwable)
        assertNull(failedState.reasons)
    }

    @Test
    fun `toRerouteState maps Failed with everything`() {
        val message = "Route fetch failed"
        val throwable = RuntimeException("Network error")

        val reasons = listOf<RouterFailure>(mockk(), mockk())
        val preRouterReasons = listOf<PreRouterFailure>(mockk(), mockk())
        val stateV2 = RerouteStateV2.Failed(message, throwable, reasons, preRouterReasons)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.Failed)
        val failedState = result as RerouteState.Failed
        assertEquals(message, failedState.message)
        assertEquals(throwable, failedState.throwable)
        assertEquals(reasons, failedState.reasons)
        assertEquals(preRouterReasons, failedState.preRouterReasons)
    }

    @Test
    fun `toRerouteState maps Failed with null parameters`() {
        val message = "Route fetch failed"

        val stateV2 = RerouteStateV2.Failed(message, null, null)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.Failed)
        val failedState = result as RerouteState.Failed
        assertEquals(message, failedState.message)
        assertNull(failedState.throwable)
        assertNull(failedState.reasons)
    }

    @Test
    fun `toRerouteState maps RouteFetched with ONLINE origin`() {
        val routerOrigin = RouterOrigin.ONLINE

        val stateV2 = RerouteStateV2.RouteFetched(routerOrigin)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.RouteFetched)
        val fetchedState = result as RerouteState.RouteFetched
        assertEquals(routerOrigin, fetchedState.routerOrigin)
    }

    @Test
    fun `toRerouteState maps RouteFetched with OFFLINE origin`() {
        val routerOrigin = RouterOrigin.OFFLINE

        val stateV2 = RerouteStateV2.RouteFetched(routerOrigin)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.RouteFetched)
        val fetchedState = result as RerouteState.RouteFetched
        assertEquals(routerOrigin, fetchedState.routerOrigin)
    }

    @Test
    fun `toRerouteState maps RouteFetched with custom routerOrigin`() {
        val routerOrigin = "custom_router"

        val stateV2 = RerouteStateV2.RouteFetched(routerOrigin)
        val result = stateV2.toRerouteState()

        assertTrue(result is RerouteState.RouteFetched)
        val fetchedState = result as RerouteState.RouteFetched
        assertEquals(routerOrigin, fetchedState.routerOrigin)
    }

    @Test
    fun `toRerouteState maps Deviation ApplyingRoute to null`() {
        val stateV2 = RerouteStateV2.Deviation.ApplyingRoute()
        val result = stateV2.toRerouteState()

        assertNull(result)
    }

    @Test
    fun `toRerouteState maps Deviation RouteIgnored to null`() {
        val stateV2 = RerouteStateV2.Deviation.RouteIgnored()
        val result = stateV2.toRerouteState()

        assertNull(result)
    }
}
