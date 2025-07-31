@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.internalAlternativeRouteIndices
import com.mapbox.navigation.base.internal.route.routerOrigin
import com.mapbox.navigation.base.options.NavigateToFinalDestination
import com.mapbox.navigation.base.options.RerouteDisabled
import com.mapbox.navigation.base.options.RerouteOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.ResponseOriginAPI
import com.mapbox.navigation.base.route.ResponseOriginAPI.Companion.DIRECTIONS_API
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MapboxJavaObjectsFactory
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coVerify
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
import io.mockk.verifySequence
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.pauseDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class MapboxRerouteControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var rerouteController: MapboxRerouteController

    @MockK(relaxUnitFun = true)
    private lateinit var directionsSession: DirectionsSession

    @MockK
    private lateinit var tripSession: TripSession

    @MockK
    private lateinit var routeOptionsUpdater: RouteOptionsUpdater

    @MockK
    private lateinit var rerouteOptions: RerouteOptions

    private val routeOptionsFromSuccessResult = MapboxJavaObjectsFactory.routeOptions(
        coordinates = listOf(Point.fromLngLat(53.0, 27.0), Point.fromLngLat(76.5, 34.8)),
    )

    private val successFromResult =
        RouteOptionsUpdater.RouteOptionsResult.Success(routeOptionsFromSuccessResult)

    @MockK
    private lateinit var errorFromResult: RouteOptionsUpdater.RouteOptionsResult.Error

    @MockK
    private lateinit var internalRouteCallback: InternalRerouteController.RoutesCallback

    @MockK
    private lateinit var routeCallback: RerouteController.RoutesCallback

    @MockK
    lateinit var primaryRerouteObserver: RerouteController.RerouteStateObserver

    @MockK
    private lateinit var compositeRerouteOptionsAdapter: MapboxRerouteOptionsAdapter

    @MockK
    private lateinit var threadController: ThreadController

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
        every { threadController.getMainScopeAndRootJob() } answers {
            JobControl(mockk(), coroutineRule.createTestScope())
        }
        every {
            directionsSession.getPrimaryRouteOptions()
        } returns MapboxJavaObjectsFactory.routeOptions()
        val primaryRoute: NavigationRoute = mockk {
            every { responseOriginAPI } returns DIRECTIONS_API
            every { id } returns "id"
            every { origin } returns RouterOrigin.ONLINE
        }
        every { directionsSession.routes } returns listOf(primaryRoute)
        every { compositeRerouteOptionsAdapter.onRouteOptions(any(), any()) } answers {
            firstArg()
        }
        every { rerouteOptions.rerouteStrategyForMapMatchedRoutes } returns RerouteDisabled
        rerouteController = spyk(
            MapboxRerouteController(
                directionsSession,
                tripSession,
                routeOptionsUpdater,
                rerouteOptions,
                threadController,
                compositeRerouteOptionsAdapter,
            ),
        )
    }

    @After
    fun cleanUp() {
        assertEquals(RerouteState.Idle, rerouteController.state)
    }

    @Test
    fun initial_state() {
        assertEquals(RerouteState.Idle, rerouteController.state)
        verify(exactly = 0) {
            rerouteController.rerouteOnDeviation(any<InternalRerouteController.RoutesCallback>())
        }
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
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onRoutesReady(
            listOf(mockk(relaxed = true)),
            RouterOrigin.ONLINE,
        )

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
    fun reroute_on_deviation_success() = coroutineRule.runBlockingTest {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routes = listOf(
            mockk<NavigationRoute> {
                every {
                    directionsRoute
                } returns MapboxJavaObjectsFactory.directionsRoute(routeOptions = null)
                every { origin } returns RouterOrigin.ONLINE
            },
        )
        val origin = RouterOrigin.ONLINE
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onRoutesReady(routes, origin)

        verify(exactly = 1) { internalRouteCallback.onNewRoutes(RerouteResult(routes, 0, origin)) }
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
    fun app_triggered_reroute_success() = coroutineRule.runBlockingTest {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routes = listOf(
            mockk<NavigationRoute> {
                every {
                    directionsRoute
                } returns MapboxJavaObjectsFactory.directionsRoute(routeOptions = null)
            },
        )
        val origin = RouterOrigin.ONLINE
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                appTriggeredRerouteSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(routes, origin)

        verify(exactly = 1) { routeCallback.onNewRoutes(routes, origin) }
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
    fun reroute_success_from_alternative_directions_api_routes() {
        mockkStatic(
            NavigationRoute::routerOrigin,
        ) {
            val initialLegIndex = 1
            val routeId1 = "id_1"
            val routeId2 = "id_2"
            val mockRoutes = listOf<NavigationRoute>(
                mockk {
                    every { id } returns routeId1
                    every {
                        routerOrigin
                    } throws IllegalStateException(
                        "route 1 origin mustn't be invoked: " +
                            "it removes from list and is not mapped",
                    )
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
                mockk {
                    every { id } returns routeId2
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
            )
            val expectedRoutes = listOf(mockRoutes[1], mockRoutes[0])
            every { directionsSession.routes } returns mockRoutes
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every { routeAlternativeId } returns routeId2
                every { currentRouteGeometryIndex } returns 40
                every { currentLegProgress } returns mockk(relaxed = true) {
                    every { legIndex } returns 2
                    every { geometryIndex } returns 10
                }
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf(routeId2 to mockk { every { legIndex } returns initialLegIndex })
            }

            val slotRerouteResult = slot<RerouteResult>()
            every {
                internalRouteCallback.onNewRoutes(capture(slotRerouteResult))
            } just runs
            addRerouteStateObserver()

            rerouteController.rerouteOnDeviation(internalRouteCallback)

            verify(exactly = 1) {
                internalRouteCallback.onNewRoutes(any())
            }
            assertEquals(expectedRoutes, slotRerouteResult.captured.routes)
            assertEquals(initialLegIndex, slotRerouteResult.captured.initialLegIndex)
            assertEquals(RouterOrigin.ONLINE, slotRerouteResult.captured.origin)

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(RouterOrigin.ONLINE),
                )
                internalRouteCallback.onNewRoutes(
                    RerouteResult(expectedRoutes, initialLegIndex, RouterOrigin.ONLINE),
                )
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            }
        }
    }

    @Test
    fun route_replan_simultaneously_with_deviation_to_alternative() {
        val initialRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
                uuid = "test-initial",
            ),
        )
        val newRoutes = createNavigationRoutes(
            createDirectionsResponse(uuid = "test-new"),
        )
        every { directionsSession.routes } returns initialRoutes
        val getRouteSignatureSlot = slot<GetRouteSignature>()
        every {
            directionsSession.requestRoutes(any(), capture(getRouteSignatureSlot), any())
        } answers {
            val callback = thirdArg<NavigationRouterCallback>()
            callback.onRoutesReady(newRoutes, RouterOrigin.ONLINE)
            3
        }
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { routeAlternativeId } returns initialRoutes[1].id
            every { currentRouteGeometryIndex } returns 2
            every {
                internalAlternativeRouteIndices()
            } returns mapOf(initialRoutes[1].id to mockk { every { legIndex } returns 0 })
        }
        every { routeOptionsUpdater.update(any(), any(), any()) } answers {
            RouteOptionsUpdater.RouteOptionsResult.Success(firstArg())
        }

        val result = runBlocking {
            suspendCoroutine { continuation ->
                rerouteController.rerouteOnParametersChange {
                    continuation.resume(it)
                }
            }
        }

        assertEquals(newRoutes, result.routes)
        assertEquals(0, result.initialLegIndex)
        assertEquals(RouterOrigin.ONLINE, result.origin)
        assertEquals(parametersChangeSignature, getRouteSignatureSlot.captured)
    }

    @Test
    fun route_replan_is_interrupted_by_deviation_to_alternative_route() {
        val initialRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
                uuid = "test-initial",
            ),
        )
        val newRoutes = createNavigationRoutes(
            createDirectionsResponse(
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                ),
                uuid = "test-new",
            ),
        )
        every { directionsSession.routes } returns initialRoutes
        val getRouteSignatureSlot = slot<GetRouteSignature>()
        every {
            directionsSession.requestRoutes(any(), capture(getRouteSignatureSlot), any())
        } answers {
            val signature = secondArg<GetRouteSignature>()
            if (signature == deviationSignature) {
                val callback = thirdArg<NavigationRouterCallback>()
                callback.onRoutesReady(newRoutes, RouterOrigin.ONLINE)
            }
            3
        }
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { routeAlternativeId } returns null
            every { currentRouteGeometryIndex } returns 2
            every {
                internalAlternativeRouteIndices()
            } returns mapOf(initialRoutes[1].id to mockk { every { legIndex } returns 0 })
        }
        every { routeOptionsUpdater.update(any(), any(), any()) } answers {
            RouteOptionsUpdater.RouteOptionsResult.Success(firstArg())
        }
        rerouteController.rerouteOnParametersChange {
            fail("this reroute is expected to be interrupted")
        }
        every { tripSession.getRouteProgress()!!.routeAlternativeId } returns initialRoutes[1].id
        assertEquals(RerouteState.FetchingRoute, rerouteController.state)

        val rerouteOnDeviationWhichInterruptsReplan = runBlocking {
            suspendCoroutine { continuation ->
                rerouteController.rerouteOnDeviation {
                    continuation.resume(it)
                }
            }
        }

        assertEquals(newRoutes, rerouteOnDeviationWhichInterruptsReplan.routes)
        assertEquals(0, rerouteOnDeviationWhichInterruptsReplan.initialLegIndex)
        assertEquals(RouterOrigin.ONLINE, rerouteOnDeviationWhichInterruptsReplan.origin)
        assertEquals(deviationSignature, getRouteSignatureSlot.captured)

        // making sure that reroute controller isn't in broken state after
        every { directionsSession.routes } returns newRoutes
        every { tripSession.getRouteProgress()!!.routeAlternativeId } returns newRoutes[1].id
        val rerouteOnCleanDeviationToAlternativeRoute = runBlocking {
            suspendCoroutine { continuation ->
                rerouteController.rerouteOnDeviation {
                    continuation.resume(it)
                }
            }
        }
        assertEquals(
            listOf(newRoutes[1], newRoutes[0]),
            rerouteOnCleanDeviationToAlternativeRoute.routes,
        )
    }

    @Test
    fun reroute_success_from_alternative_mixed_routes() {
        mockkStatic(
            NavigationRoute::routerOrigin,
        ) {
            val initialLegIndex = 1
            val routeId1 = "id_1"
            val routeId2 = "id_2"
            val mockRoutes = listOf<NavigationRoute>(
                mockk {
                    every { id } returns routeId1
                    every {
                        routerOrigin
                    } throws IllegalStateException(
                        "route 1 origin mustn't be invoked: " +
                            "it removes from list and is not mapped",
                    )
                    every { responseOriginAPI } returns ResponseOriginAPI.MAP_MATCHING_API
                },
                mockk {
                    every { id } returns routeId2
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
            )
            val expectedRoutes = listOf(mockRoutes[1], mockRoutes[0])
            every { directionsSession.routes } returns mockRoutes
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every { routeAlternativeId } returns routeId2
                every { currentRouteGeometryIndex } returns 40
                every { currentLegProgress } returns mockk(relaxed = true) {
                    every { legIndex } returns 2
                    every { geometryIndex } returns 10
                }
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf(routeId2 to mockk { every { legIndex } returns initialLegIndex })
            }

            val slotRerouteResult = slot<RerouteResult>()
            every {
                internalRouteCallback.onNewRoutes(capture(slotRerouteResult))
            } just runs
            addRerouteStateObserver()

            rerouteController.rerouteOnDeviation(internalRouteCallback)

            verify(exactly = 1) {
                internalRouteCallback.onNewRoutes(any())
            }
            assertEquals(expectedRoutes, slotRerouteResult.captured.routes)
            assertEquals(initialLegIndex, slotRerouteResult.captured.initialLegIndex)
            assertEquals(RouterOrigin.ONLINE, slotRerouteResult.captured.origin)

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(RouterOrigin.ONLINE),
                )
                internalRouteCallback.onNewRoutes(
                    RerouteResult(expectedRoutes, initialLegIndex, RouterOrigin.ONLINE),
                )
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            }
        }
    }

    @Test
    fun reroute_success_from_alternative_no_leg_progress() {
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
                        "route 1 origin mustn't be invoked: " +
                            "it removes from list and is not mapped",
                    )
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
                mockk {
                    every { id } returns routeId2
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
            )
            val expectedRoutes = listOf(mockRoutes[1], mockRoutes[0])
            every { directionsSession.routes } returns mockRoutes
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every { routeAlternativeId } returns routeId2
                every { currentRouteGeometryIndex } returns 40
                every { currentLegProgress } returns null
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf(routeId2 to mockk { every { legIndex } returns 1 })
            }
            val slotRerouteResult = slot<RerouteResult>()
            every {
                internalRouteCallback.onNewRoutes(capture(slotRerouteResult))
            } just runs
            addRerouteStateObserver()

            rerouteController.rerouteOnDeviation(internalRouteCallback)

            verify(exactly = 1) {
                internalRouteCallback.onNewRoutes(any())
            }
            assertEquals(expectedRoutes, slotRerouteResult.captured.routes)
            assertEquals(1, slotRerouteResult.captured.initialLegIndex)
            assertEquals(RouterOrigin.ONLINE, slotRerouteResult.captured.origin)

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(RouterOrigin.ONLINE),
                )
                internalRouteCallback.onNewRoutes(
                    RerouteResult(expectedRoutes, 1, RouterOrigin.ONLINE),
                )
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            }
        }
    }

    @Test
    fun reroute_success_from_alternative_no_alternative_index() {
        mockkStatic(
            NavigationRoute::routerOrigin,
        ) {
            val routeId1 = "id_1"
            val routeId2 = "id_2"
            val routeId3 = "id_3"
            val mockRoutes = listOf<NavigationRoute>(
                mockk {
                    every { id } returns routeId1
                    every {
                        routerOrigin
                    } throws IllegalStateException(
                        "route 1 origin mustn't be invoked: " +
                            "it removes from list and is not mapped",
                    )
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
                mockk {
                    every { id } returns routeId2
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
                mockk {
                    every { id } returns routeId3
                    every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONLINE
                    every { responseOriginAPI } returns ResponseOriginAPI.DIRECTIONS_API
                },
            )
            val expectedRoutes = listOf(mockRoutes[2], mockRoutes[0], mockRoutes[1])
            every { directionsSession.routes } returns mockRoutes
            every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
                every { routeAlternativeId } returns routeId3
                every { currentRouteGeometryIndex } returns 40
                every { currentLegProgress } returns mockk(relaxed = true) {
                    every { legIndex } returns 2
                    every { geometryIndex } returns 10
                }
                every {
                    internalAlternativeRouteIndices()
                } returns mapOf("bad_id" to mockk { every { legIndex } returns 1 })
            }
            val slotRerouteResult = slot<RerouteResult>()
            every {
                internalRouteCallback.onNewRoutes(capture(slotRerouteResult))
            } just runs
            addRerouteStateObserver()

            rerouteController.rerouteOnDeviation(internalRouteCallback)

            verify(exactly = 1) {
                internalRouteCallback.onNewRoutes(any())
            }
            assertEquals(expectedRoutes, slotRerouteResult.captured.routes)
            assertEquals(0, slotRerouteResult.captured.initialLegIndex)
            assertEquals(RouterOrigin.ONLINE, slotRerouteResult.captured.origin)

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(
                    RerouteState.RouteFetched(RouterOrigin.ONLINE),
                )
                internalRouteCallback.onNewRoutes(
                    RerouteResult(expectedRoutes, 0, RouterOrigin.ONLINE),
                )
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
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onFailure(mockk(), MapboxJavaObjectsFactory.routeOptions())

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
    fun reroute_fails_for_map_matched_route_with_disabled_reroute_strategy() {
        val mockRoutes = listOf<NavigationRoute>(
            mockk {
                every { id } returns "id_1"
                every { responseOriginAPI } returns ResponseOriginAPI.MAP_MATCHING_API
                every { origin } returns RouterOrigin.ONLINE
            },
        )
        every { directionsSession.routes } returns mockRoutes
        every { tripSession.getRouteProgress() } returns mockk(relaxed = true) {
            every { currentRouteGeometryIndex } returns 40
            every { currentLegProgress } returns mockk(relaxed = true) {
                every { legIndex } returns 2
                every { geometryIndex } returns 10
            }
        }

        addRerouteStateObserver()

        rerouteController.rerouteOnDeviation(internalRouteCallback)

        verify(exactly = 0) {
            internalRouteCallback.onNewRoutes(any())
        }

        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(
                match { it is RerouteState.Failed },
            )
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_succeeded_for_map_matched_route_with_fallback_to_directions_api_strategy() =
        coroutineRule.runBlockingTest {
            mockRouteOptionsResult(successFromResult)
            addRerouteStateObserver()
            val routes = listOf(
                mockk<NavigationRoute> {
                    every { id } returns "id_1"
                    every { responseOriginAPI } returns ResponseOriginAPI.MAP_MATCHING_API
                    every { directionsRoute } returns MapboxJavaObjectsFactory.directionsRoute(null)
                    every { origin } returns RouterOrigin.ONLINE
                },
            )
            every { directionsSession.routes } returns routes
            val origin = RouterOrigin.ONLINE
            val routeRequestCallback = slot<NavigationRouterCallback>()
            every {
                directionsSession.requestRoutes(
                    any(),
                    any(),
                    capture(routeRequestCallback),
                )
            } returns 1L
            every {
                rerouteOptions.rerouteStrategyForMapMatchedRoutes
            } returns NavigateToFinalDestination

            rerouteController.rerouteOnDeviation(internalRouteCallback)
            routeRequestCallback.captured.onRoutesReady(routes, origin)

            verify(exactly = 1) {
                internalRouteCallback.onNewRoutes(RerouteResult(routes, 0, origin))
            }
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
    fun reroute_request_canceled_external() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onCanceled(
            MapboxJavaObjectsFactory.routeOptions(),
            RouterOrigin.ONLINE,
        )

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
    fun reroute_calls_interrupt_if_currently_fetching() = coroutineRule.runBlockingTest {
        addRerouteStateObserver()
        val routeOptions1 = MapboxJavaObjectsFactory.routeOptions(
            coordinates = listOf(Point.fromLngLat(1.0, 2.0), Point.fromLngLat(3.0, 4.0)),
        )
        val updaterSuccess1 = RouteOptionsUpdater.RouteOptionsResult.Success(routeOptions1)
        val routeOptions2 = MapboxJavaObjectsFactory.routeOptions(
            coordinates = listOf(Point.fromLngLat(1.5, 2.5), Point.fromLngLat(3.0, 4.0)),
        )
        val updaterSuccess2 = RouteOptionsUpdater.RouteOptionsResult.Success(routeOptions2)
        val routeRequestCallback1 = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptions1,
                deviationSignature,
                capture(routeRequestCallback1),
            )
        } returns 1L
        val routeRequestCallback2 = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptions2,
                deviationSignature,
                capture(routeRequestCallback2),
            )
        } returns 2L

        mockRouteOptionsResult(updaterSuccess1)
        rerouteController.rerouteOnDeviation(internalRouteCallback)
        pauseDispatcher {
            // this ensure that we don't run coroutines synchronously that could
            // make the test pass even if state changes were scheduled back to the message queue
            // in an incorrect order
            mockRouteOptionsResult(updaterSuccess2)
            rerouteController.rerouteOnDeviation(internalRouteCallback)
            routeRequestCallback1.captured.onCanceled(routeOptions1, RouterOrigin.ONLINE)
        }
        verify(exactly = 0) { directionsSession.cancelRouteRequest(1L) }

        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_only_calls_interrupt_if_currently_fetching() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onRoutesReady(
            listOf(mockk(relaxed = true)),
            RouterOrigin.ONLINE,
        )

        verify(exactly = 0) { directionsSession.cancelAll() }
        verify(exactly = 0) { directionsSession.cancelRouteRequest(any()) }
        verify(exactly = 0) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
        }
    }

    @Test
    fun interrupt_route_request() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L
        every {
            directionsSession.cancelRouteRequest(1L)
        } answers {
            routeRequestCallback.captured.onCanceled(
                MapboxJavaObjectsFactory.routeOptions(),
                RouterOrigin.ONLINE,
            )
        }

        rerouteController.rerouteOnDeviation(internalRouteCallback)
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
    fun interrupt_route_request_with_pending_callback() = coroutineRule.runBlockingTest {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        rerouteController.interrupt()

        routeRequestCallback.captured.onRoutesReady(
            listOf(mockk(relaxed = true)),
            RouterOrigin.ONLINE,
        )

        coVerify(exactly = 0) { internalRouteCallback.onNewRoutes(any()) }
    }

    @Test
    fun cancelling_scope_changes_state_to_interrupted() = coroutineRule.runBlockingTest {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        // we don't invoke onCancel callback here
        rerouteController.interrupt()

        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun interrupting_request_changes_state_to_interrupted_only_once() =
        coroutineRule.runBlockingTest {
            mockRouteOptionsResult(successFromResult)
            addRerouteStateObserver()
            val routeRequestCallback = slot<NavigationRouterCallback>()
            every {
                directionsSession.requestRoutes(
                    routeOptionsFromSuccessResult,
                    deviationSignature,
                    capture(routeRequestCallback),
                )
            } returns 1L
            every {
                directionsSession.cancelRouteRequest(1L)
            } answers {
                routeRequestCallback.captured.onCanceled(
                    MapboxJavaObjectsFactory.routeOptions(),
                    RouterOrigin.ONLINE,
                )
            }

            rerouteController.rerouteOnDeviation(internalRouteCallback)
            rerouteController.interrupt()

            verifyOrder {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            }
            verify(exactly = 1) {
                primaryRerouteObserver.onRerouteStateChanged(RerouteState.Interrupted)
            }
        }

    @Test
    fun `deliver failure synchronously when options update fails`() =
        coroutineRule.runBlockingTest {
            mockRouteOptionsResult(errorFromResult)
            addRerouteStateObserver()

            pauseDispatcher {
                rerouteController.rerouteOnDeviation(internalRouteCallback)

                verifySequence {
                    primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                    primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
                    primaryRerouteObserver.onRerouteStateChanged(ofType<RerouteState.Failed>())
                    primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
                }
            }
        }

    @Test
    fun `failure with pre-router reasons`() =
        coroutineRule.runBlockingTest {
            val preRouterFailure = mockk<PreRouterFailure>()
            mockRouteOptionsResult(
                RouteOptionsUpdater.RouteOptionsResult.Error(mockk(), preRouterFailure),
            )
            addRerouteStateObserver()

            pauseDispatcher {
                rerouteController.reroute(routeCallback)

                val states = mutableListOf<RerouteState>()
                verify { primaryRerouteObserver.onRerouteStateChanged(capture(states)) }
                assertTrue(
                    states.any {
                        it is RerouteState.Failed && preRouterFailure in it.preRouterReasons
                    },
                )
            }
        }

    @Test
    fun `failure without pre-router reasons`() =
        coroutineRule.runBlockingTest {
            mockRouteOptionsResult(
                RouteOptionsUpdater.RouteOptionsResult.Error(mockk()),
            )
            addRerouteStateObserver()

            pauseDispatcher {
                rerouteController.reroute(routeCallback)

                val states = mutableListOf<RerouteState>()
                verify { primaryRerouteObserver.onRerouteStateChanged(capture(states)) }
                assertTrue(
                    states.any { it is RerouteState.Failed && it.preRouterReasons.isEmpty() },
                )
            }
        }

    @Test
    fun `interrupt while no reroute running shouldn't crash`() {
        rerouteController.interrupt()
    }

    @Test
    fun invalid_route_option() {
        mockRouteOptionsResult(errorFromResult)
        addRerouteStateObserver()

        rerouteController.rerouteOnDeviation(internalRouteCallback)

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
        verify(exactly = 0) { directionsSession.requestRoutes(any(), any(), any()) }
    }

    @Test
    fun reroute_options_seconds_to_meters_radius() {
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        listOf(
            Triple(200.0, 1, 200.0),
            Triple(0.0, 1, null),
            Triple(200.0, 3, 600.0),
            Triple(5000.0, 1, 1000.0),
            Triple(200.0, 0, null),
        ).forEach { (speed, secondsRadius, expectedMetersRadius) ->
            val mockRoute = MapboxJavaObjectsFactory.routeOptions(
                profile = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC,
            )
            every { directionsSession.getPrimaryRouteOptions() } returns mockRoute
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

            clearMocks(routeOptionsUpdater, answers = false)
            rerouteController.rerouteOnDeviation(internalRouteCallback)

            routeRequestCallback.captured.onRoutesReady(
                listOf(mockk(relaxed = true)),
                RouterOrigin.ONLINE,
            )

            verify(exactly = 1) {
                routeOptionsUpdater.update(
                    mockRoute.toBuilder().avoidManeuverRadius(expectedMetersRadius).build(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Test
    fun reroute_options_avoid_maneuvers_only_driving() {
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        listOf(
            Pair(DirectionsCriteria.PROFILE_CYCLING, false),
            Pair(DirectionsCriteria.PROFILE_DRIVING, true),
            Pair(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, true),
            Pair(DirectionsCriteria.PROFILE_WALKING, false),
        ).forEach { (profile, result) ->
            val mockRo = MapboxJavaObjectsFactory.routeOptions(profile = profile)
            every { directionsSession.getPrimaryRouteOptions() } returns mockRo
            mockRouteOptionsResult(successFromResult)
            addRerouteStateObserver()
            every { rerouteOptions.avoidManeuverSeconds } returns 1
            every {
                tripSession.locationMatcherResult
            } returns mockk {
                every {
                    enhancedLocation
                } returns mockk {
                    every { speed } returns 200.0
                }
            }

            clearMocks(routeOptionsUpdater, answers = false)
            rerouteController.rerouteOnDeviation(internalRouteCallback)

            routeRequestCallback.captured.onRoutesReady(
                listOf(mockk(relaxed = true)),
                RouterOrigin.ONLINE,
            )

            verify(exactly = 1) {
                routeOptionsUpdater.update(
                    mockRo.toBuilder().avoidManeuverRadius(if (result) 200.0 else null).build(),
                    any(),
                    any(),
                )
            }
        }
    }

    @Test
    fun add_the_same_observer_twice_and_remove_twice() {
        assertTrue(addRerouteStateObserver())
        assertFalse(addRerouteStateObserver())

        assertTrue(rerouteController.unregisterRerouteStateObserver(primaryRerouteObserver))
        assertFalse(rerouteController.unregisterRerouteStateObserver(primaryRerouteObserver))
    }

    private fun addRerouteStateObserver(
        rerouteStateObserver: RerouteController.RerouteStateObserver = primaryRerouteObserver,
    ): Boolean {
        return rerouteController.registerRerouteStateObserver(rerouteStateObserver)
    }

    @Test
    fun uses_internal_route_options_delegate() {
        mockRouteOptionsResult(successFromResult)
        val mockNewRouteOptions = MapboxJavaObjectsFactory.routeOptions()
        every {
            compositeRerouteOptionsAdapter.onRouteOptions(any(), any())
        } returns mockNewRouteOptions
        val routeOptionsSlot = slot<RouteOptions>()
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                capture(routeOptionsSlot),
                deviationSignature,
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.rerouteOnDeviation(internalRouteCallback)
        routeRequestCallback.captured.onRoutesReady(
            listOf(mockk(relaxed = true)),
            RouterOrigin.ONLINE,
        )

        verify(exactly = 1) {
            compositeRerouteOptionsAdapter.onRouteOptions(
                routeOptionsFromSuccessResult,
                match { it.signature == deviationSignature },
            )
        }
        assertEquals(mockNewRouteOptions, routeOptionsSlot.captured)
    }

    @Test
    fun set_reroute_options_adapter() {
        val mockRerouteOptionsDelegateManger = mockk<RerouteOptionsAdapter>()

        rerouteController.setRerouteOptionsAdapter(mockRerouteOptionsDelegateManger)

        verify(exactly = 1) {
            compositeRerouteOptionsAdapter.externalOptionsAdapter = mockRerouteOptionsDelegateManger
        }
    }

    @Test
    fun set_null_reroute_options_adapter() {
        rerouteController.setRerouteOptionsAdapter(null)

        verify(exactly = 1) {
            compositeRerouteOptionsAdapter.externalOptionsAdapter = null
        }
    }

    @Test
    fun reroute_with_navigation_router_callback() = coroutineRule.runBlockingTest {
        val callback = mockk<RerouteController.RoutesCallback>(relaxed = true)
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<NavigationRouterCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                GetRouteSignature(
                    GetRouteSignature.Reason.REROUTE_OTHER,
                    GetRouteSignature.Origin.APP,
                ),
                capture(routeRequestCallback),
            )
        } returns 1L

        rerouteController.reroute(callback)
        val navigationRoute = mockk<NavigationRoute>(relaxed = true)
        val origin = RouterOrigin.ONLINE
        routeRequestCallback.captured.onRoutesReady(listOf(navigationRoute), origin)

        verify(exactly = 1) {
            callback.onNewRoutes(listOf(navigationRoute), origin)
        }
    }

    private fun mockRouteOptionsResult(
        _routeOptionsResult: RouteOptionsUpdater.RouteOptionsResult,
    ) {
        assertFalse(
            "routeOptionsResult mustn't be the *RouteOptionsProvider.RouteOptionsResult*, " +
                "subclass is applied only",
            _routeOptionsResult::class.isAbstract,
        )
        every {
            routeOptionsUpdater.update(
                any(),
                any(),
                any<LocationMatcherResult>(),
                any(),
                any(),
            )
        } returns _routeOptionsResult
    }
}
