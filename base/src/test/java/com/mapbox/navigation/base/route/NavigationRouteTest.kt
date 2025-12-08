@file:OptIn(ExperimentalMapboxNavigationAPI::class, ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.route

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.route.updateExpirationTime
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.NativeRouteParserRule
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.factories.toDataRef
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.URL

class NavigationRouteTest {

    @get:Rule
    val routeParserRule = NativeRouteParserRule()

    @get:Rule
    val loggerFrontendTestRule = LoggingFrontendTestRule()

    @Test
    fun `origin access`() {
        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")

        val navigationRoute = NavigationRoute.create(
            directionsResponseJson = responseJson,
            routeRequestUrl = requestUrl,
            routerOrigin = RouterOrigin.OFFLINE,
        )

        assertTrue(navigationRoute.all { it.origin == RouterOrigin.OFFLINE })
    }

    @Test
    fun `id access`() {
        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")

        val navigationRoute = NavigationRoute.create(
            directionsResponseJson = responseJson,
            routeRequestUrl = requestUrl,
            routerOrigin = RouterOrigin.OFFLINE,
        )

        assertEquals(
            listOf(
                "FYenNs6nfVvkDQgvLWnYcZvn2nvekWStF7nM0JV0X_IBAlsXWvomuA==#0",
            ),
            navigationRoute.map { it.id },
        )
    }

    @Test
    fun `navigation routes serialised from string, data_ref, and model are equals`() =
        runBlocking<Unit> {
            val responseString = FileUtils.loadJsonFixture("test_directions_response.json")
            val responseModel = DirectionsResponse.fromJson(
                FileUtils.loadJsonFixture("test_directions_response.json"),
            )
            val responseDataRef = responseString.toDataRef()
            val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
            val testRouteOptions = RouteOptions.fromUrl(URL(requestUrl))

            val serialisedFromModel = NavigationRoute.create(
                responseModel,
                testRouteOptions,
                RouterOrigin.ONLINE,
            )
            val serialisedFromString = NavigationRoute.create(
                responseString,
                requestUrl,
                RouterOrigin.ONLINE,
            )
            val serialisedFromDataRef = NavigationRoute.createAsync(
                responseDataRef,
                requestUrl,
                RouterOrigin.ONLINE,
                0L,
                false,
            )

            assertEquals(serialisedFromModel, serialisedFromString)
            assertEquals(serialisedFromModel, serialisedFromDataRef.routes)
        }

    @Test
    fun `fill expected closures on original route creation`() {
        val routeJson = FileUtils.loadJsonFixture("route_closure_second_waypoint.json")
        val route = DirectionsRoute.fromJson(routeJson)
        val actual = NavigationRoute(
            route,
            route.waypoints(),
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates("0.0,0.0;1.1,1.1")
                .build(),
            mockk(relaxed = true),
            null,
            ResponseOriginAPI.DIRECTIONS_API,
        )

        assertEquals(
            listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
            actual.unavoidableClosures,
        )
    }

    @Test
    fun `copy expected closures form original route`() {
        val routeJson = FileUtils.loadJsonFixture("route_closure_second_waypoint.json")
        val route = DirectionsRoute.fromJson(routeJson)
        val original = NavigationRoute(
            route,
            route.waypoints(),
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates("0.0,0.0;1.1,1.1")
                .build(),
            mockk(relaxed = true),
            null,
            ResponseOriginAPI.DIRECTIONS_API,
        )

        val newRouteJson = FileUtils.loadJsonFixture("route_closure_second_silent_waypoint.json")
        val copied = original.copy(
            directionsRoute = DirectionsRoute.fromJson(newRouteJson),
            overriddenTraffic = null,
        )

        assertEquals(original.unavoidableClosures, copied.unavoidableClosures)
    }

    @Test
    fun updateExpirationTime() {
        val routeJson = FileUtils.loadJsonFixture("route_closure_second_waypoint.json")
        val route = DirectionsRoute.fromJson(routeJson)
        val navigationRoute = NavigationRoute(
            route,
            route.waypoints(),
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates("0.0,0.0;1.1,1.1")
                .build(),
            mockk(relaxed = true),
            null,
            ResponseOriginAPI.DIRECTIONS_API,
        )

        navigationRoute.updateExpirationTime(45)
        assertEquals(45L, navigationRoute.expirationTimeElapsedSeconds)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `access ev max charge from route`() {
        val testMaxChargeValue = 3892
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                routeOptions = createRouteOptions(
                    unrecognizedProperties = mapOf(
                        "ev_max_charge" to JsonPrimitive(testMaxChargeValue.toString()),
                    ),
                ),
            ),
        )

        val value = route.evMaxCharge

        assertEquals(testMaxChargeValue, value)
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @Test
    fun `access ev max charge from route with invalid request`() {
        val route = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                routeOptions = createRouteOptions(
                    unrecognizedProperties = mapOf(
                        "ev_max_charge" to JsonPrimitive("wrong value"),
                    ),
                ),
            ),
        )

        val value = route.evMaxCharge

        assertNull(value)
    }

    @Test
    fun `route refresh metadata is null after creation`() {
        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")

        val navigationRoute = NavigationRoute.create(
            directionsResponseJson = responseJson,
            routeRequestUrl = requestUrl,
            routerOrigin = RouterOrigin.ONLINE,
        )

        assertTrue(
            navigationRoute.all {
                it.routeRefreshMetadata == null
            },
        )
    }

    @Test
    fun `map matched route`() {
        val requestUrl = FileUtils.loadJsonFixture("kochelsee_map_matching_request.txt")
        val responseJson = FileUtils.loadJsonFixture("kochelsee_map_matching_response.json")

        val result = NavigationRoute.createMatchedRoutes(
            responseJson,
            requestUrl,
        )

        assertNull(result.error)
        val matches = result.value!!

        assertEquals(1, matches.size)
        val match = matches[0]

        assertEquals(
            0.0,
            match.confidence,
            0.01,
        )
        assertEquals(
            2,
            match.navigationRoute.waypoints?.size,
        )
        assertEquals(
            1,
            match.navigationRoute.directionsRoute.legs()?.size,
        )
        assertEquals(
            2,
            match.navigationRoute.directionsRoute.legs()?.get(0)?.steps()?.size,
        )
    }

    @Test
    fun `multiple matches`() {
        val requestUrl = FileUtils.loadJsonFixture(
            "kochelsee_multiple_matches_map_matching_request.txt",
        )
        val responseJson = FileUtils.loadJsonFixture(
            "kochelsee_multiple_matches_map_matching_response.json",
        )

        val result = NavigationRoute.createMatchedRoutes(
            responseJson,
            requestUrl,
        )

        assertNull(result.error)
        val matches = result.value!!

        assertEquals(2, matches.size)

        val firstMatch = matches[0]
        assertEquals(
            listOf(
                Point.fromLngLat(11.359198, 47.642065),
                Point.fromLngLat(11.351706, 47.632409),
            ),
            firstMatch.navigationRoute.waypoints?.map { it.location() },
        )

        val secondMatch = matches[1]
        assertEquals(
            listOf(
                Point.fromLngLat(11.353961, 47.631334),
                Point.fromLngLat(11.355391, 47.628011),
            ),
            secondMatch.navigationRoute.waypoints?.map { it.location() },
        )
    }
}
