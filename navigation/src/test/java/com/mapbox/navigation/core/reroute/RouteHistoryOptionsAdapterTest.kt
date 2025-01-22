package com.mapbox.navigation.core.reroute

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_CYCLING
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
import com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_WALKING
import com.mapbox.api.directions.v5.DirectionsCriteria.ProfileCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.every
import io.mockk.mockk
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.net.URL

@OptIn(ExperimentalMapboxNavigationAPI::class)
class RouteHistoryOptionsAdapterTest {

    @get:Rule
    val loggingFrontendTestRule = LoggingFrontendTestRule()

    @Test
    fun `adapter doesn't change route options if no route progress available`() {
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                null
            },
        )
        val initialOptions = createTestRouteOptions()

        val result = adapter.onRouteOptions(initialOptions, defaultRouteOptionsAdapterParams)

        assertEquals(
            initialOptions,
            result,
        )
    }

    @Test
    fun `adapter adds current route to history option`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
            ),
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 30,
                )
            },
        )
        val originalRouteOptions = createTestRouteOptions(
            unrecognizedProperties = mapOf("test-key" to JsonPrimitive("test-value")),
        )

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = updatedRouteOptions.toHttpUrl()
        assertEquals(
            "history-test,0,30",
            url.getRoutesHistory(),
        )
        assertEquals(
            "test-value",
            url.queryParameter("test-key"),
        )
    }

    @Test
    fun `adapter doesn't add current route to history for no driving traffic profiles`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
            ),
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 30,
                )
            },
        )
        for (profile in listOf(PROFILE_CYCLING, PROFILE_WALKING, PROFILE_DRIVING)) {
            val originalRouteOptions = createTestRouteOptions(
                profile = profile,
            )

            val updatedRouteOptions = adapter.onRouteOptions(
                originalRouteOptions,
                defaultRouteOptionsAdapterParams,
            )

            val url = updatedRouteOptions.toHttpUrl()
            assertNull(
                "failed for $profile profile",
                url.getRoutesHistory(),
            )
        }
    }

    @Test
    fun `reroute on a route with history`() {
        val testNavigationRoute1 = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
            ),
        )
        val testNavigationRoute2 = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test-2",
            ),
        )
        var currentRouteProgress = createRouteProgress(
            testNavigationRoute1[0],
            currentRouteGeometryIndexValue = 30,
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                currentRouteProgress
            },
        )
        val originalRouteOptions = createTestRouteOptions()

        val routeOptionsFirstReroute = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )
        currentRouteProgress = createRouteProgress(
            testNavigationRoute2[0],
            currentRouteGeometryIndexValue = 3,
        )
        val routeOptionsSecondReroute = adapter.onRouteOptions(
            routeOptionsFirstReroute,
            defaultRouteOptionsAdapterParams,
        )

        val url = routeOptionsSecondReroute.toHttpUrl()
        assertEquals(
            "history-test-2,0,3;history-test,0,30",
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `reroute to alternative route`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
            ),
        )
        var currentRouteProgress = createRouteProgress(
            testNavigationRoute[0],
            currentRouteGeometryIndexValue = 10,
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                currentRouteProgress
            },
        )
        val originalRouteOptions = createTestRouteOptions()

        val routeOptionsFirstReroute = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )
        currentRouteProgress = createRouteProgress(
            testNavigationRoute[1],
            currentRouteGeometryIndexValue = 15,
        )
        val routeOptionsSecondReroute = adapter.onRouteOptions(
            routeOptionsFirstReroute,
            defaultRouteOptionsAdapterParams,
        )

        val url = routeOptionsSecondReroute.toHttpUrl()
        assertEquals(
            "history-test,1,15;history-test,0,10",
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `reroute on an offline route`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = null,
            ),
            routerOrigin = RouterOrigin.OFFLINE,
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 1,
                )
            },
        )
        val originalRouteOptions = createTestRouteOptions()

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = updatedRouteOptions.toHttpUrl()
        assertNull(url.getRoutesHistory())
    }

    @Test
    fun `reroute from CA`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "test",
            ),
        ).first()
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute,
                    currentRouteGeometryIndexValue = 5,
                )
            },
        )
        val originalRouteOptions = RouteOptions.fromUrl(
            URL(
                "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/" +
                    "10.9219981868%2C48.3714761975;11.5886%2C48.18363" +
                    "?access_token=****qBMw&alternatives=true" +
                    "&annotations=distance%2Cduration%2Cspeed%2Ccongestion_numeric" +
                    "%2Cclosure%2Cfreeflow_speed%2Ccurrent_speed" +
                    "&avoid_maneuver_radius=117.28&banner_instructions=true" +
                    "&bearings=131.598%2C45%3B&continue_straight=false" +
                    "&enable_refresh=true&exclude=motorway&geometries=polyline6" +
                    "&language=de-DE&layers=0%3B&metadata=true&overview=full" +
                    "&roundabout_exits=true" +
                    "&routes_history=-essE9UPDygsfadQR-SaToZAiS7ql6I9oece-glIq3Qgs" +
                    "j759vjBKg%3D%3D_eu-west-1%2C0%2C161&snapping_include_closures=true%3Btrue" +
                    "&snapping_include_static_closures=true%3Btrue&steps=true" +
                    "&suppress_voice_instruction_local_names=true&voice_instructions=true" +
                    "&voice_units=metric" +
                    "&waypoint_names=%3BOtl-Aicher-Stra%C3%9Fe&waypoints=0%3B1" +
                    "&waypoints_per_route=true",
            ),
        )

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = updatedRouteOptions.toHttpUrl()
        assertEquals(
            "test,0,5;-essE9UPDygsfadQR-SaToZAiS7ql6I9oece-glIq3Qgsj759vjBKg==_eu-west-1,0,161",
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `many reroutes on different routes`() {
        var testRouteCounter = 0
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    createNavigationRoutes(
                        createDirectionsResponse(
                            uuid = "test-route${testRouteCounter++}",
                        ),
                    ).first(),
                    currentRouteGeometryIndexValue = 1,
                )
            },
        )

        var routeOptions = createTestRouteOptions()
        repeat(100) {
            routeOptions = adapter.onRouteOptions(
                routeOptions,
                defaultRouteOptionsAdapterParams,
            )
        }

        val url = routeOptions.toHttpUrl()
        val lastTenRoutes = (99 downTo 90)
            .joinToString(separator = ";") { "test-route$it,0,1" }
        assertEquals(
            lastTenRoutes,
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `empty existing history`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
            ),
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 30,
                )
            },
        )
        val originalRouteOptions = createTestRouteOptions(
            unrecognizedProperties = mapOf("routes_history" to JsonPrimitive("")),
        )

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = updatedRouteOptions.toHttpUrl()
        // empty initial value is preserved
        // there is no requirements to do that, consider this test as documentation
        assertEquals(
            "history-test,0,30;",
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `unexpected existing history`() {
        val testNavigationRoute = createNavigationRoutes(
            createDirectionsResponse(
                uuid = "history-test",
            ),
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 30,
                )
            },
        )
        val originalRouteOptions = createTestRouteOptions(
            unrecognizedProperties = mapOf("routes_history" to JsonPrimitive(";1;0")),
        )

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        val url = updatedRouteOptions.toHttpUrl()
        // unexpected initial value is preserved
        // there is no requirements to do that, consider this test as documentation
        assertEquals(
            "history-test,0,30;;1;0",
            url.getRoutesHistory(),
        )
    }

    @Test
    fun `adapter doesn't change route options in case of unhandled error`() {
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                error("test error")
            },
        )
        val initialOptions = createTestRouteOptions()

        val result = adapter.onRouteOptions(
            initialOptions,
            defaultRouteOptionsAdapterParams,
        )

        assertEquals(
            initialOptions,
            result,
        )
    }

    @Test
    fun `adapter doesn't change route options in case of a map matched route`() {
        val testNavigationRoute = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "history-test",
            ),
            responseOriginAPI = ResponseOriginAPI.MAP_MATCHING_API,
        )
        val adapter = createRouteHistoryOptionsAdapter(
            latestRouteProgressProvider = {
                createRouteProgress(
                    testNavigationRoute[0],
                    currentRouteGeometryIndexValue = 30,
                )
            },
        )
        val originalRouteOptions = createTestRouteOptions(
            unrecognizedProperties = mapOf("test-key" to JsonPrimitive("test-value")),
        )

        val updatedRouteOptions = adapter.onRouteOptions(
            originalRouteOptions,
            defaultRouteOptionsAdapterParams,
        )

        assertEquals(originalRouteOptions, updatedRouteOptions)
    }
}

internal fun createRouteHistoryOptionsAdapter(
    latestRouteProgressProvider: RouteProgressProvider = { null },
) = RouteHistoryOptionsAdapter(
    latestRouteProgressProvider,
)

private fun createRouteProgress(
    navigationRouteValue: NavigationRoute = createNavigationRoute(),
    currentRouteGeometryIndexValue: Int = 1,
) = mockk<RouteProgress>(relaxed = true) {
    every { navigationRoute } returns navigationRouteValue
    every { currentRouteGeometryIndex } returns currentRouteGeometryIndexValue
}

private fun createTestRouteOptions(
    @ProfileCriteria profile: String = PROFILE_DRIVING_TRAFFIC,
    unrecognizedProperties: Map<String, JsonElement>? = null,
) = createRouteOptions(
    profile = profile,
    unrecognizedProperties = unrecognizedProperties,
)

private fun RouteOptions.toHttpUrl() = toUrl("***").toString().toHttpUrl()

private fun HttpUrl.getRoutesHistory() = queryParameter("routes_history")
