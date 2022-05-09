package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxRerouteControllerTest {

    private lateinit var rerouteController: MapboxRerouteController

    @MockK
    private lateinit var directionsSession: DirectionsSession

    @MockK
    private lateinit var tripSession: TripSession

    @MockK
    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    @MockK
    private lateinit var rerouteOptions: RerouteOptions

    @MockK
    private lateinit var successFromResult: RouteOptionsUpdater.RouteOptionsResult.Success

    @MockK
    private lateinit var routeOptionsFromSuccessResult: RouteOptions

    @MockK
    private lateinit var errorFromResult: RouteOptionsUpdater.RouteOptionsResult.Error

    @MockK
    private lateinit var routeCallback: RerouteController.RoutesCallback

    @MockK
    private lateinit var navigationRouteCallback: NavigationRerouteController.RoutesCallback

    @MockK
    lateinit var primaryRerouteObserver: RerouteController.RerouteStateObserver

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        rerouteController = spyk(
            MapboxRerouteController(
                directionsSession,
                tripSession,
                routeOptionsUpdater,
                rerouteOptions,
                ThreadController()
            )
        )
        every { successFromResult.routeOptions } returns routeOptionsFromSuccessResult
    }

    @After
    fun cleanUp() {
        assertEquals(RerouteState.Idle, rerouteController.state)
    }

    @Test
    fun initial_state() {
        assertEquals(RerouteState.Idle, rerouteController.state)
        verify(exactly = 0) { rerouteController.reroute(any<RerouteController.RoutesCallback>()) }
        verify(exactly = 0) { rerouteController.interrupt() }
    }

    @Test
    fun initial_state_with_added_state_observer() {
        val added = addRerouteStateObserver()

        assertTrue("RerouteStateObserver is not added", added)
        verify(exactly = 1) { primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle) }
    }

    @Test
    fun reroute_get_from_inputs() {
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())

        verify(exactly = 2) {
            tripSession.locationMatcherResult
        }
        verify(exactly = 2) {
            tripSession.getRouteProgress()
        }
        verify(exactly = 1) {
            directionsSession.getPrimaryRouteOptions()
        }
    }

    @Test
    fun reroute_success() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routes = listOf(
            mockk<NavigationRoute> {
                every { directionsRoute } returns mockk()
            }
        )
        val origin = mockk<RouterOrigin>()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(routes, origin)

        val expectedRoutes = routes.toDirectionsRoutes()
        verify(exactly = 1) { routeCallback.onNewRoutes(expectedRoutes) }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.RouteFetched(origin))
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.RouteFetched(origin))
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_success_from_alternative() {
        mockkStatic(
            NavigationRoute::routerOrigin,
        ) {
            val routeId1 = "id_1"
            val routeId2 = "id_2"
            val mockRoutes = listOf<NavigationRoute>(
                mockk {
                    every { id } returns routeId1
                    every {
                        routerOrigin
                    } throws IllegalStateException(
                        "route 1 origin mustn't be invoked: it removes from list and is not mapped"
                    )
                },
                mockk {
                    every { id } returns routeId2
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                },
            )
            val expectedRoutes = mockRoutes.drop(1)
            every { directionsSession.routes } returns mockRoutes
            every { tripSession.getRouteProgress()?.routeAlternativeId } returns routeId2
            val slotNewRoutes = slot<List<NavigationRoute>>()
            val slotOrigin = slot<RouterOrigin>()
            every {
                navigationRouteCallback.onNewRoutes(capture(slotNewRoutes), capture(slotOrigin))
            } just runs
            addRerouteStateObserver()

            rerouteController.reroute(navigationRouteCallback)

            verify(exactly = 1) {
                navigationRouteCallback.onNewRoutes(any(), any())
            }
            assertEquals(expectedRoutes, slotNewRoutes.captured)
            assertEquals(RouterOrigin.Offboard, slotOrigin.captured)

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(RouterOrigin.Offboard)
                )
                navigationRouteCallback.onNewRoutes(expectedRoutes, RouterOrigin.Offboard)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            }
        }
    }

    @Test
    fun reroute_unsuccess() {
        addRerouteStateObserver()
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onFailure(mockk(), mockk())

        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(ofType<RerouteState.Failed>())
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(ofType<RerouteState.Failed>())
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_request_canceled_external() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onCanceled(mockk(), mockk())

        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_calls_interrupt_if_currently_fetching() {
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L
        rerouteController.reroute(routeCallback)

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())

        verify(exactly = 1) { directionsSession.cancelRouteRequest(1L) }
    }

    @Test
    fun reroute_only_calls_interrupt_if_currently_fetching() {
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())

        verify(exactly = 0) { directionsSession.cancelAll() }
        verify(exactly = 0) { directionsSession.cancelRouteRequest(any()) }
    }

    @Test
    fun interrupt_route_request() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L
        every {
            directionsSession.cancelRouteRequest(1L)
        } answers {
            routeRequestCallback.captured.onCanceled(mockk(), mockk())
        }

        rerouteController.reroute(routeCallback)
        rerouteController.interrupt()

        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun invalid_route_option() {
        mockRouteOptionsResult(errorFromResult)
        addRerouteStateObserver()

        rerouteController.reroute(routeCallback)

        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(ofType<RerouteState.Failed>())
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(ofType<RerouteState.Failed>())
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any()) }
    }

    @Test
    fun reroute_options_seconds_to_meters_radius() {
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        listOf(
            Triple(200f, 1, 200.0),
            Triple(0f, 1, null),
            Triple(200f, 3, 600.0),
            Triple(5000f, 1, 1000.0),
            Triple(200f, 0, null),
        ).forEach { (speed, secondsRadius, expectedMetersRadius) ->
            val mockRo = mockk<RouteOptions> {
                every { profile() } returns DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            }
            val mockRoBuilder = mockk<RouteOptions.Builder>()
            every { directionsSession.getPrimaryRouteOptions() } returns mockRo
            every { mockRo.toBuilder() } returns mockRoBuilder
            every { mockRoBuilder.avoidManeuverRadius(any()) } returns mockRoBuilder
            every { mockRoBuilder.build() } returns mockRo
            mockRouteOptionsResult(successFromResult)
            addRerouteStateObserver()
            every { rerouteOptions.avoidManeuverSeconds } returns secondsRadius
            every {
                tripSession.locationMatcherResult
            } returns mockk {
                every {
                    enhancedLocation
                } returns mockk {
                    every { getSpeed() } returns speed
                }
            }

            rerouteController.reroute(routeCallback)

            verify(exactly = 1) {
                mockRoBuilder.avoidManeuverRadius(expectedMetersRadius)
            }
        }

        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())
    }

    @Test
    fun reroute_options_avoid_maneuvers_only_driving() {
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns 1L

        listOf(
            Pair(DirectionsCriteria.PROFILE_CYCLING, false),
            Pair(DirectionsCriteria.PROFILE_DRIVING, true),
            Pair(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, true),
            Pair(DirectionsCriteria.PROFILE_WALKING, false),
        ).forEach { (profile, result) ->
            val mockRo = mockk<RouteOptions> {
                every { profile() } returns profile
            }
            val mockRoBuilder = mockk<RouteOptions.Builder>()
            every { directionsSession.getPrimaryRouteOptions() } returns mockRo
            every { mockRo.toBuilder() } returns mockRoBuilder
            every { mockRoBuilder.avoidManeuverRadius(any()) } returns mockRoBuilder
            every { mockRoBuilder.build() } returns mockRo
            mockRouteOptionsResult(successFromResult)
            addRerouteStateObserver()
            every { rerouteOptions.avoidManeuverSeconds } returns 1
            every {
                tripSession.locationMatcherResult
            } returns mockk {
                every {
                    enhancedLocation
                } returns mockk {
                    every { speed } returns 200f
                }
            }

            rerouteController.reroute(routeCallback)

            verify(
                exactly = if (result) {
                    1
                } else {
                    0
                }
            ) {
                mockRoBuilder.avoidManeuverRadius(any())
            }
        }

        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())
    }

    @Test
    fun add_the_same_observer_twice_and_remove_twice() {
        assertTrue(addRerouteStateObserver())
        assertFalse(addRerouteStateObserver())

        assertTrue(rerouteController.unregisterRerouteStateObserver(primaryRerouteObserver))
        assertFalse(rerouteController.unregisterRerouteStateObserver(primaryRerouteObserver))
    }

    private fun addRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver = primaryRerouteObserver
    ): Boolean {
        return rerouteController.registerRerouteStateObserver(rerouteStateObserver)
    }

    @Test
    fun uses_route_options_delegate() {
        mockRouteOptionsResult(successFromResult)
        val mockNewRouteOptions = mockk<RouteOptions>()
        val mockRerouteOptionsDelegateManger = mockk<RerouteOptionsAdapter> {
            every { onRouteOptions(any()) } returns mockNewRouteOptions
        }
        val routeOptionsSlot = slot<RouteOptions>()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                capture(routeOptionsSlot),
                capture(routeRequestCallback)
            )
        } returns 1L

        rerouteController.setRerouteOptionsAdapter(mockRerouteOptionsDelegateManger)
        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk(), mockk())

        verify(exactly = 1) {
            mockRerouteOptionsDelegateManger.onRouteOptions(routeOptionsFromSuccessResult)
        }
        assertEquals(mockNewRouteOptions, routeOptionsSlot.captured)
    }

    private fun mockRouteOptionsResult(
        _routeOptionsResult: RouteOptionsUpdater.RouteOptionsResult
    ) {
        assertFalse(
            "routeOptionsResult mustn't be the *RouteOptionsProvider.RouteOptionsResult*, " +
                "subclass is applied only",
            _routeOptionsResult::class.isAbstract
        )
        every {
            routeOptionsUpdater.update(
                any(),
                any(),
                any<LocationMatcherResult>(),
            )
        } returns _routeOptionsResult
    }
}
