package com.mapbox.navigation.instrumentation_tests.core

import android.location.Location
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider
import com.mapbox.navigation.instrumentation_tests.utils.routes.RoutesProvider.toNavigationRoutes
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.utils.coroutines.sdkTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRouteTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {

    override fun setupMockLocation(): Location = mockLocationUpdatesRule.generateLocationUpdate {
        latitude = 38.894721
        longitude = -77.031991
    }

    @Test
    fun native_route_parser_uses_response_uuid_and_index() = sdkTest {
        val routes = RoutesProvider.dc_short_with_alternative(activity).toNavigationRoutes()
        routes.forEach {
            assertTrue(it.directionsResponse.uuid()?.isNotBlank() == true)
        }
        assertEquals("${routes[0].directionsResponse.uuid()}#0", routes[0].id)
        assertEquals("${routes[1].directionsResponse.uuid()}#1", routes[1].id)
    }

    @Test
    fun native_route_parser_backfills_unique_identifier_when_uuid_and_index_missing() = sdkTest {
        val routes = RoutesProvider.dc_short_with_alternative(activity)
            .toNavigationRoutes()
            .map {
                // erase the request UUID and index
                val routeWithoutUuid = it.directionsRoute.toBuilder()
                    .requestUuid(null)
                    .routeIndex("0")
                    .build()
                // map back to NavigationRoute
                routeWithoutUuid.toNavigationRoute(RouterOrigin.Custom())
            }
        // assert that Nav Native backfilled a local unique identifier
        assertTrue(routes[0].id.matches(Regex("^local@(.+)#0")))
        assertTrue(routes[1].id.matches(Regex("^local@(.+)#0")))
        assertNotEquals(routes[0].id, routes[1].id)
    }
}
