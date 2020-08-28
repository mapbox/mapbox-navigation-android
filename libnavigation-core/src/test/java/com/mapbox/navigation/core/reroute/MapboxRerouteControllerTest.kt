package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.Logger
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.routeoptions.RouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
    private lateinit var logger: Logger

    @MockK
    private lateinit var successFromResult: RouteOptionsUpdater.RouteOptionsResult.Success

    @MockK
    private lateinit var routeOptionsFromSuccessResult: RouteOptions

    @MockK
    private lateinit var errorFromResult: RouteOptionsUpdater.RouteOptionsResult.Error

    @MockK
    private lateinit var routeCallback: RerouteController.RoutesCallback

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
                ThreadController,
                logger
            )
        )
        every { successFromResult.routeOptions } returns routeOptionsFromSuccessResult
    }

    @After
    fun cleanUp() {
        assertEquals(RerouteState.Idle, rerouteController.state)
        // routeCallback mustn't called in current implementation. DirectionSession update routes internally
        verify(exactly = 0) { routeCallback.onNewRoutes(any()) }
    }

    @Test
    fun initial_state() {
        assertEquals(RerouteState.Idle, rerouteController.state)
        verify(exactly = 0) { rerouteController.reroute(any()) }
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
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk())

        verify(exactly = 1) {
            tripSession.getEnhancedLocation()
        }
        verify(exactly = 1) {
            tripSession.getRouteProgress()
        }
        verify(exactly = 1) {
            directionsSession.getRouteOptions()
        }
    }

    @Test
    fun reroute_success() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk())

        assertTrue(routeRequestCallback.isCaptured)
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
        }
        verify(exactly = 1) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.RouteFetched)
        }
        verify(exactly = 2) {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
        verifyOrder {
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.FetchingRoute)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.RouteFetched)
            primaryRerouteObserver.onRerouteStateChanged(RerouteState.Idle)
        }
    }

    @Test
    fun reroute_unsuccess() {
        addRerouteStateObserver()
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesRequestFailure(mockk(), mockk())

        assertTrue(routeRequestCallback.isCaptured)
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
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesRequestCanceled(mockk())

        assertTrue(routeRequestCallback.isCaptured)
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
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()
        rerouteController.reroute(routeCallback)

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk())

        verify(exactly = 1) { directionsSession.cancel() }
    }

    @Test
    fun reroute_only_calls_interrupt_if_currently_fetching() {
        mockRouteOptionsResult(successFromResult)
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()

        rerouteController.reroute(routeCallback)
        routeRequestCallback.captured.onRoutesReady(mockk())

        verify(exactly = 0) { directionsSession.cancel() }
    }

    @Test
    fun interrupt_route_request() {
        mockRouteOptionsResult(successFromResult)
        addRerouteStateObserver()
        val routeRequestCallback = slot<RoutesRequestCallback>()
        every {
            directionsSession.requestRoutes(
                routeOptionsFromSuccessResult,
                capture(routeRequestCallback)
            )
        } returns mockk()
        every {
            directionsSession.cancel()
        } answers {
            routeRequestCallback.captured.onRoutesRequestCanceled(mockk())
        }

        rerouteController.reroute(routeCallback)
        rerouteController.interrupt()

        assertTrue(routeRequestCallback.isCaptured)
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
                any()
            )
        } returns _routeOptionsResult
    }
}
