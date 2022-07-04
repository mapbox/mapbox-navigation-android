package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.RoutesSetCallback
import com.mapbox.navigation.core.RoutesSetCallbackError
import com.mapbox.navigation.core.RoutesSetCallbackSuccess
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import com.mapbox.navigation.utils.internal.logD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TAG = "[SetRoutesTest]"

class SetRoutesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation
    private val countDownLatch = CountDownLatch(1)

    private val callback = object : RoutesSetCallback {

        var success: RoutesSetCallbackSuccess? = null
        var error: RoutesSetCallbackError? = null

        override fun onRoutesSetResult(result: RoutesSetCallbackSuccess) {
            logD("Invoked onRoutesSetResult with result: {$result}", TAG)
            this.success = result
            countDownLatch.countDown()
        }

        override fun onRoutesSetError(result: RoutesSetCallbackError) {
            logD("Invoked onRoutesSetError with result: {$result}", TAG)
            this.error = result
            countDownLatch.countDown()
        }
    }

    @Before
    fun setUp() {
        runOnMainSync {
            mapboxNavigation = MapboxNavigationProvider.create(
                NavigationOptions.Builder(activity)
                    .accessToken(getMapboxAccessTokenFromResources(activity))
                    .build()
            )
        }
    }

    @Test
    fun set_navigation_routes_successfully() {
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        val route = mockRoute.routeResponse.routes()[0]
            .toBuilder()
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(mockRoute.routeWaypoints)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build()
            )
            .build()
            .toNavigationRoute(RouterOrigin.Custom())
        val routes = listOf(route)
        mapboxNavigation.setNavigationRoutes(routes, callback = callback)
        countDownLatch.await(5, TimeUnit.SECONDS)
        assertEquals(routes, callback.success!!.routes)
    }

    @Test
    fun set_navigation_routes_failed() {
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        val directionsRoute = mockRoute.routeResponse.routes()[0]
        val route = directionsRoute
            .toBuilder()
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(mockRoute.routeWaypoints)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build()
            )
            .build()
            .toNavigationRoute(RouterOrigin.Custom())
        val routes = listOf(route)
        mapboxNavigation.setNavigationRoutes(routes, 6, callback = callback)
        countDownLatch.await(5, TimeUnit.SECONDS)
        callback.error!!.run {
            assertEquals(routes, this.routes)
            assertNotEquals("", this.error)
        }
    }
}
