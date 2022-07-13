package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.RoutesSetError
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.MapboxNavigationRule
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.sdkTest
import com.mapbox.navigation.instrumentation_tests.utils.coroutines.setNavigationRoutesAsync
import com.mapbox.navigation.instrumentation_tests.utils.routes.MockRoutesProvider
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.getMapboxAccessTokenFromResources
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SetRoutesTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @get:Rule
    val mapboxNavigationRule = MapboxNavigationRule()
    private lateinit var mapboxNavigation: MapboxNavigation

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
    fun set_navigation_routes_successfully() = sdkTest {
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        val routes = NavigationRoute.create(
            mockRoute.routeResponse,
            RouteOptions.builder()
                .coordinatesList(mockRoute.routeWaypoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build(),
            RouterOrigin.Custom()
        )
        val result = mapboxNavigation.setNavigationRoutesAsync(routes)
        result.run {
            assertTrue(this.isValue)
            assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
        }
    }

    @Test
    fun set_navigation_routes_ignore_alternatives() = sdkTest {
        val mockRoute = MockRoutesProvider.dc_very_short_with_invalid_alternatives(activity)
        val routes = NavigationRoute.create(
            mockRoute.routeResponse,
            RouteOptions.builder()
                .coordinatesList(mockRoute.routeWaypoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build(),
            RouterOrigin.Custom()
        )

        val result = mapboxNavigation.setNavigationRoutesAsync(routes)
        result.run {
            assertTrue(this.isValue)
            assertEquals(1, this.value!!.ignoredAlternatives.size)
            assertNotNull(this.value!!.ignoredAlternatives[routes[1].id])
        }
    }

    @Test
    fun set_navigation_routes_failed() = sdkTest {
        val mockRoute = MockRoutesProvider.dc_very_short(activity)
        val routes = NavigationRoute.create(
            mockRoute.routeResponse,
            RouteOptions.builder()
                .coordinatesList(mockRoute.routeWaypoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .build(),
            RouterOrigin.Custom()
        )
        val result = mapboxNavigation.setNavigationRoutesAsync(routes, 6)
        result.run {
            assertTrue(this.isError)
            assertNotNull(this.error!!.message)
        }
    }

    @Test
    fun set_navigation_routes_empty() = sdkTest {
        val result = mapboxNavigation.setNavigationRoutesAsync(emptyList())
        result.run {
            assertTrue(this.isValue)
            assertEquals(emptyMap<String, RoutesSetError>(), this.value!!.ignoredAlternatives)
        }
    }
}
