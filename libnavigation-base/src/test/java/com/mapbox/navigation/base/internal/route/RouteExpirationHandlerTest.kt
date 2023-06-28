package com.mapbox.navigation.base.internal.route

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteExpirationHandlerTest {

    val route = mockk<NavigationRoute> {
        every { id } returns "id#0"
    }
    private val currentTime = 12345L

    @Before
    fun setUp() {
        RouteExpirationHandler.clear()
        mockkObject(Time.SystemClockImpl)
        every { Time.SystemClockImpl.seconds() } returns currentTime
    }

    @After
    fun tearDown() {
        RouteExpirationHandler.clear()
        unmockkObject(Time.SystemClockImpl)
    }

    @Test
    fun isRouteExpired_noData() {
        assertFalse(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_hasRouteData_notExpired() {
        RouteExpirationHandler.updateRouteExpirationData(route, 10)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 9
        assertFalse(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_hasRouteData_expired() {
        RouteExpirationHandler.updateRouteExpirationData(route, 10)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 11
        assertTrue(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_hasRouteData_differentRouteWithTheSameUuid_expired() {
        val newRoute = mockk<NavigationRoute> { every { id } returns "id#0" }
        RouteExpirationHandler.updateRouteExpirationData(route, 10)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 11
        assertTrue(RouteExpirationHandler.isRouteExpired(newRoute))
    }

    @Test
    fun isRouteExpired_differentRouteExpired() {
        val newRoute = mockk<NavigationRoute> { every { id } returns "id#1" }
        RouteExpirationHandler.updateRouteExpirationData(route, 10)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 11
        assertFalse(RouteExpirationHandler.isRouteExpired(newRoute))
    }

    @Test
    fun isRouteExpired_routeDataUpdated_notExpired() {
        val newCurrentTime = 12356L
        RouteExpirationHandler.updateRouteExpirationData(route, 10)
        every { Time.SystemClockImpl.seconds() } returns newCurrentTime
        RouteExpirationHandler.updateRouteExpirationData(route, 5)

        every { Time.SystemClockImpl.seconds() } returns newCurrentTime + 4
        assertFalse(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_routeDataUpdated_expired() {
        val newCurrentTime = 12356L
        RouteExpirationHandler.updateRouteExpirationData(route, 10)
        every { Time.SystemClockImpl.seconds() } returns newCurrentTime
        RouteExpirationHandler.updateRouteExpirationData(route, 5)

        every { Time.SystemClockImpl.seconds() } returns newCurrentTime + 6
        assertTrue(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_routeDataUpdatedToNull_notExpired() {
        val newCurrentTime = 12356L
        RouteExpirationHandler.updateRouteExpirationData(route, 10)
        every { Time.SystemClockImpl.seconds() } returns newCurrentTime
        RouteExpirationHandler.updateRouteExpirationData(route, null)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 9
        assertFalse(RouteExpirationHandler.isRouteExpired(route))
    }

    @Test
    fun isRouteExpired_routeDataUpdatedToNull_expired() {
        val newCurrentTime = 12356L
        RouteExpirationHandler.updateRouteExpirationData(route, 10)
        every { Time.SystemClockImpl.seconds() } returns newCurrentTime
        RouteExpirationHandler.updateRouteExpirationData(route, null)

        every { Time.SystemClockImpl.seconds() } returns currentTime + 11
        assertTrue(RouteExpirationHandler.isRouteExpired(route))
    }
}
