package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.TestSystemClock
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import com.mapbox.navigation.testing.factories.toDataRef
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationRouteIsExpiredTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val clock = TestSystemClock()

    @Before
    fun setUp() {
        mockkObject(ThreadController)
        every { ThreadController.DefaultDispatcher } returns Dispatchers.Main
    }

    @After
    fun tearDown() {
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
                        Point.fromLngLat(18.576235, 54.412025),
                    ),
                )
                .build().toUrl("").toString(),
            RouterOrigin.ONLINE,
            clock.elapsedMillis,
            false,
            TestSDKRouteParser(),
        ).routes

        clock.advanceTimeBy(9.seconds)

        assertFalse(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertFalse(routes[2].isExpired())

        clock.advanceTimeBy(2.seconds)

        assertTrue(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertFalse(routes[2].isExpired())

        clock.advanceTimeBy(5.seconds)

        assertTrue(routes[0].isExpired())
        assertFalse(routes[1].isExpired())
        assertTrue(routes[2].isExpired())
    }
}
