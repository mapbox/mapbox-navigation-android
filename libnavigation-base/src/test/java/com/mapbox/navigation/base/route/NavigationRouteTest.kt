package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.base.internal.route.RouteCompatibilityCache
import com.mapbox.navigation.base.internal.route.toTestNavigationRoute
import com.mapbox.navigation.base.internal.route.toTestNavigationRoutes
import com.mapbox.navigation.base.internal.utils.DirectionsRouteMissingConditionsCheck
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.MapboxJavaObjectsFactory
import com.mapbox.navigation.testing.factories.createClosure
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URL
import java.util.UUID

class NavigationRouteTest {

    @Before
    fun setup() {
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val response = JSONObject(this.firstArg<String>())
            val routesCount = response.getJSONArray("routes").length()
            val idBase = if (response.has("uuid")) {
                response.getString("uuid")
            } else {
                "local@${UUID.randomUUID()}"
            }
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeInfo } returns mockk(relaxed = true)
                            every { routeId } returns "$idBase#$it"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                            every { waypoints } returns emptyList()
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun `toNavigationRoute - waypoints back filled from route legs`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("multileg_route.json")
        )

        val navigationRoute = directionsRoute.toTestNavigationRoute(RouterOrigin.Custom())

        assertEquals(3, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.157347, 38.783004))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.167276, 38.775717))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(-77.153468, 38.77091))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![2]
        )
    }

    @Test
    fun `toNavigationRoute - waypoints back filled from route options`() {
        val directionsRoute = spyk(MapboxJavaObjectsFactory.directionsRoute()) {
            every { requestUuid() } returns "asdf"
            every { routeIndex() } returns "0"
            every { routeOptions() } returns RouteOptions.builder()
                .profile("driving")
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 1.1),
                        Point.fromLngLat(2.2, 2.2),
                    )
                )
                .build()
            every { legs() } returns null
        }

        val navigationRoute = directionsRoute.toTestNavigationRoute(RouterOrigin.Custom())

        assertEquals(2, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(1.1, 1.1))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(2.2, 2.2))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
    }

    @Test
    fun `toNavigationRoute - uuid from route used`() {
        val directionsRoute = spyk(MapboxJavaObjectsFactory.directionsRoute()) {
            every { requestUuid() } returns "asdf"
            every { routeIndex() } returns "0"
            every { routeOptions() } returns RouteOptions.builder()
                .profile("driving")
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 1.1),
                        Point.fromLngLat(2.2, 2.2),
                    )
                )
                .build()
            every { legs() } returns null
        }

        val navigationRoute = directionsRoute.toTestNavigationRoute(RouterOrigin.Offboard)

        val responseJsonSlot = slot<String>()
        verify {
            NativeRouteParserWrapper.parseDirectionsResponse(
                capture(responseJsonSlot),
                any(),
                any()
            )
        }
        assertEquals("asdf", DirectionsResponse.fromJson(responseJsonSlot.captured).uuid())
        assertEquals("asdf#0", navigationRoute.id)
    }

    @Test
    fun `toNavigationRoute - waypoints back filled from route options ignoring silent`() {
        val directionsRoute = spyk(MapboxJavaObjectsFactory.directionsRoute()) {
            every { requestUuid() } returns "asdf"
            every { routeIndex() } returns "0"
            every { routeOptions() } returns RouteOptions.builder()
                .profile("driving")
                .coordinatesList(
                    listOf(
                        Point.fromLngLat(1.1, 1.1),
                        Point.fromLngLat(2.2, 2.2),
                        Point.fromLngLat(3.3, 3.3),
                        Point.fromLngLat(4.4, 4.4),
                    )
                )
                .waypointIndicesList(listOf(0, 2, 3))
                .build()
            every { legs() } returns null
        }

        val navigationRoute = directionsRoute.toTestNavigationRoute(RouterOrigin.Custom())

        assertEquals(3, navigationRoute.directionsResponse.waypoints()!!.size)
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(1.1, 1.1))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![0]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(3.3, 3.3))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![1]
        )
        assertEquals(
            DirectionsWaypoint.builder()
                .name("")
                .rawLocation(doubleArrayOf(4.4, 4.4))
                .build(),
            navigationRoute.directionsResponse.waypoints()!![2]
        )
    }

    @Test
    fun `when route created, compatibility cache notified`() {
        mockkObject(RouteCompatibilityCache)

        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")
        val routes = NavigationRoute.create(
            routeRequestUrl = requestUrl,
            directionsResponseJson = responseJson
        )

        verify(exactly = 1) {
            RouteCompatibilityCache.cacheCreationResult(routes)
        }

        unmockkObject(RouteCompatibilityCache)
    }

    @Test
    fun `when route created from objects, compatibility cache notified`() {
        mockkObject(RouteCompatibilityCache)

        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val routeOptions = RouteOptions.fromUrl(URL(requestUrl))
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")
        val response = DirectionsResponse.fromJson(responseJson)
        val routes = NavigationRoute.create(
            routeOptions = routeOptions,
            directionsResponse = response
        )

        verify(exactly = 1) {
            RouteCompatibilityCache.cacheCreationResult(routes)
        }

        unmockkObject(RouteCompatibilityCache)
    }

    @Test
    fun `when route copied, compatibility cache notified`() {
        mockkObject(RouteCompatibilityCache)

        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")
        val routes = NavigationRoute.create(
            routeRequestUrl = requestUrl,
            directionsResponseJson = responseJson
        )

        val originalRoute = routes.first()
        val copiedRoute = originalRoute.copy(
            directionsResponse = originalRoute.directionsResponse.toBuilder().uuid("diff").build()
        )

        verify(exactly = 1) {
            RouteCompatibilityCache.cacheCreationResult(listOf(copiedRoute))
        }

        unmockkObject(RouteCompatibilityCache)
    }

    @Test
    fun `origin access`() {
        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")

        val navigationRoute = NavigationRoute.create(
            directionsResponseJson = responseJson,
            routeRequestUrl = requestUrl,
            routerOrigin = RouterOrigin.Onboard
        )

        assertTrue(navigationRoute.all { it.origin == RouterOrigin.Onboard })
    }

    @Test
    fun `id access`() {
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val routesCount = JSONObject(this.firstArg<String>())
                .getJSONArray("routes")
                .length()
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeInfo } returns mockk(relaxed = true)
                            every { routeId } returns "some_id"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                            every { waypoints } returns emptyList()
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
        val requestUrl = FileUtils.loadJsonFixture("test_directions_request_url.txt")
        val responseJson = FileUtils.loadJsonFixture("test_directions_response.json")

        val navigationRoute = NavigationRoute.create(
            directionsResponseJson = responseJson,
            routeRequestUrl = requestUrl,
            routerOrigin = RouterOrigin.Onboard
        )

        assertTrue(navigationRoute.all { it.id == "some_id" })
    }

    @Test
    fun `map from NavigationRoute to DirectionsRoute produce exception if not pass check`() {
        mockkObject(DirectionsRouteMissingConditionsCheck) {
            val mockDirectionsRoute = mockk<DirectionsRoute>()
            val mockNavigationRoute = mockk<NavigationRoute> {
                every { directionsRoute } returns mockDirectionsRoute
            }
            every {
                DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(mockDirectionsRoute)
            } throws IllegalStateException()

            assertThrows(IllegalStateException::class.java) {
                listOf(mockNavigationRoute).toDirectionsRoutes()
            }
        }
    }

    @Test
    fun `map from DirectionsRoute to NavigationRoute produce exception if not pass check`() {
        mockkObject(DirectionsRouteMissingConditionsCheck, NavigationRoute) {
            val mockDirectionsRoute = mockk<DirectionsRoute>()
            every {
                DirectionsRouteMissingConditionsCheck.checkDirectionsRoute(mockDirectionsRoute)
            } throws IllegalStateException()

            assertThrows(IllegalStateException::class.java) {
                listOf(mockDirectionsRoute).toTestNavigationRoutes(RouterOrigin.Offboard)
            }
        }
    }

    @Test
    fun `fill expected closures on original route creation`() {
        val routeJson = FileUtils.loadJsonFixture("route_closure_second_waypoint.json")
        val actual = NavigationRoute(
            DirectionsResponse.builder()
                .routes(listOf(DirectionsRoute.fromJson(routeJson)))
                .uuid("uuid")
                .code("Ok")
                .build(),
            0,
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates("0.0,0.0;1.1,1.1")
                .build(),
            mockk(relaxed = true)
        )

        assertEquals(
            listOf(listOf(createClosure(5, 8)), listOf(createClosure(0, 8))),
            actual.unavoidableClosures
        )
    }

    @Test
    fun `copy expected closures form original route`() {
        val routeJson = FileUtils.loadJsonFixture("route_closure_second_waypoint.json")
        val original = NavigationRoute(
            DirectionsResponse.builder()
                .routes(listOf(DirectionsRoute.fromJson(routeJson)))
                .uuid("uuid")
                .code("Ok")
                .build(),
            0,
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinates("0.0,0.0;1.1,1.1")
                .build(),
            mockk(relaxed = true)
        )

        val newRouteJson = FileUtils.loadJsonFixture("route_closure_second_silent_waypoint.json")
        val copied = original.copy(
            directionsResponse = DirectionsResponse.builder()
                .routes(listOf(DirectionsRoute.fromJson(newRouteJson)))
                .uuid("uuid")
                .code("Ok")
                .build()
        )

        assertEquals(original.unavoidableClosures, copied.unavoidableClosures)
    }
}
