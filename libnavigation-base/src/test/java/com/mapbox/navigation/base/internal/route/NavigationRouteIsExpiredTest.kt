package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import com.mapbox.navigation.testing.factories.toDataRef
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationRouteIsExpiredTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()
    private val responseTime = 12345L

    @Before
    fun setUp() {
        mockkObject(Time.SystemClockImpl)
        mockkObject(ThreadController)
        every { ThreadController.DefaultDispatcher } returns Dispatchers.Main
    }

    @After
    fun tearDown() {
        unmockkObject(Time.SystemClockImpl)
        unmockkObject(ThreadController)
    }

    @Test
    fun isRouteExpired() = coroutineRule.runBlockingTest {
        val routes = NavigationRoute.createAsync(
            FileUtils.loadJsonFixture("routes_with_refresh_ttl.json").toDataRef(),
            RouteOptions.builder()
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(18.576644, 54.410361),
                        Point.fromLngLat(18.576235, 54.412025)
                    )
                )
                .build().toUrl("").toString(),
            RouterOrigin.Offboard,
            responseTime,
            TestSDKRouteParser()
        )

        every { Time.SystemClockImpl.seconds() } returns responseTime + 9

        assertFalse(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertFalse(routes[2].isExpired())

        every { Time.SystemClockImpl.seconds() } returns responseTime + 11

        assertTrue(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertFalse(routes[2].isExpired())

        every { Time.SystemClockImpl.seconds() } returns responseTime + 16

        assertTrue(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertTrue(routes[2].isExpired())
    }
}
