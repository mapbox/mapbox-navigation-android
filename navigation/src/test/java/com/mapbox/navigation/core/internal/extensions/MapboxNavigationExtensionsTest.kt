package com.mapbox.navigation.core.internal.extensions

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.common.location.Location
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxNavigationExtensionsTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun navigationTripSessionStateFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<TripSessionStateObserver>()
        every {
            navigation.registerTripSessionStateObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterTripSessionStateObserver(any()) } just Runs
        var actual = TripSessionState.STOPPED

        val flow = navigation.flowTripSessionState().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = TripSessionState.STARTED
        callbackSlot.captured.onSessionStateChanged(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterTripSessionStateObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationRoutesUpdatedFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<RoutesObserver>()
        every {
            navigation.registerRoutesObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterRoutesObserver(any()) } just Runs
        val mockRoute1 = mockk<NavigationRoute>(relaxed = true)
        val mockRoute2 = mockk<NavigationRoute>(relaxed = true)
        var actual = createRoutesUpdatedResult(
            listOf(mockRoute1),
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
        )

        val flow = navigation.flowRoutesUpdated().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = createRoutesUpdatedResult(
            listOf(mockRoute2),
            RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE,
        )
        callbackSlot.captured.onRoutesChanged(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterRoutesObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationRouteProgressFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<RouteProgressObserver>()
        every {
            navigation.registerRouteProgressObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterRouteProgressObserver(any()) } just Runs
        var actual = mockk<RouteProgress>(relaxed = true)

        val flow = navigation.flowRouteProgress().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = mockk<RouteProgress>(relaxed = true)
        callbackSlot.captured.onRouteProgressChanged(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterRouteProgressObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationNewRawLocationFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<LocationObserver>()
        every {
            navigation.registerLocationObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterLocationObserver(any()) } just Runs
        var actual = mockk<Location>(relaxed = true)

        val flow = navigation.flowNewRawLocation().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = mockk<Location>(relaxed = true)
        callbackSlot.captured.onNewRawLocation(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterLocationObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationNewLocationMatcherResultFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<LocationObserver>()
        every {
            navigation.registerLocationObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterLocationObserver(any()) } just Runs
        var actual = mockk<LocationMatcherResult>(relaxed = true)

        val flow = navigation.flowLocationMatcherResult().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = mockk<LocationMatcherResult>(relaxed = true)
        callbackSlot.captured.onNewLocationMatcherResult(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterLocationObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationVoiceInstructionsFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<VoiceInstructionsObserver>()
        every {
            navigation.registerVoiceInstructionsObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterVoiceInstructionsObserver(any()) } just Runs
        var actual = mockk<VoiceInstructions>(relaxed = true)

        val flow = navigation.flowVoiceInstructions().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = mockk<VoiceInstructions>(relaxed = true)
        callbackSlot.captured.onNewVoiceInstructions(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterVoiceInstructionsObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationFinalDestinationFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>()
        val callbackSlot = slot<ArrivalObserver>()
        every {
            navigation.registerArrivalObserver(capture(callbackSlot))
        } just Runs
        every { navigation.unregisterArrivalObserver(any()) } just Runs
        var actual = mockk<RouteProgress>(relaxed = true)

        val flow = navigation.flowOnFinalDestinationArrival().onEach { actual = it }
        val job = coroutineRule.coroutineScope.launch { flow.collect() }
        advanceUntilIdle()
        val expected = mockk<RouteProgress>(relaxed = true)
        callbackSlot.captured.onFinalDestinationArrival(expected)
        advanceUntilIdle()

        assertEquals(expected, actual)

        job.cancel()
        advanceUntilIdle()

        verify { navigation.unregisterArrivalObserver(callbackSlot.captured) }
    }

    @Test
    fun navigationOnWaypointArrivalFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>(relaxed = true)
        val routeProgress = mockk<RouteProgress>()
        val arrivalObserver = slot<ArrivalObserver>()
        every {
            navigation.registerArrivalObserver(capture(arrivalObserver))
        } answers {
            arrivalObserver.captured.onWaypointArrival(routeProgress)
        }

        val flowResult = navigation.flowOnWaypointArrival().first()

        assertEquals(routeProgress, flowResult)
        verify { navigation.unregisterArrivalObserver(arrivalObserver.captured) }
    }

    @Test
    fun navigationOnNextRouteLegStartFlowable() = coroutineRule.runBlockingTest {
        val navigation = mockk<MapboxNavigation>(relaxed = true)
        val routeLegProgress = mockk<RouteLegProgress>()
        val arrivalObserver = slot<ArrivalObserver>()
        every {
            navigation.registerArrivalObserver(capture(arrivalObserver))
        } answers {
            arrivalObserver.captured.onNextRouteLegStart(routeLegProgress)
        }

        val flowResult = navigation.flowOnNextRouteLegStart().first()

        assertEquals(routeLegProgress, flowResult)
        verify { navigation.unregisterArrivalObserver(arrivalObserver.captured) }
    }
}
