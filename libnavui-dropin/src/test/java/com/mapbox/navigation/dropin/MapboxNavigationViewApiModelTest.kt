package com.mapbox.navigation.dropin

import android.location.Location
import androidx.lifecycle.LifecycleOwner
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.dropin.viewmodel.MapboxNavigationViewModel
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapboxNavigationViewApiModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun onStart() {
        val lifecycleOwner = mockk< LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val viewModel = MapboxNavigationViewModel(factory)

        viewModel.onStart(lifecycleOwner)

        verify { mockMapboxNavigation.registerLocationObserver(any()) }
        verify { mockMapboxNavigation.registerRouteProgressObserver(any()) }
        verify { mockMapboxNavigation.registerRoutesObserver(any()) }
        verify { mockMapboxNavigation.registerArrivalObserver(any()) }
        verify { mockMapboxNavigation.registerBannerInstructionsObserver(any()) }
        verify { mockMapboxNavigation.registerTripSessionStateObserver(any()) }
    }

    @Test
    fun onStop() {
        val lifecycleOwner = mockk< LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val viewModel = MapboxNavigationViewModel(factory)

        viewModel.onStop(lifecycleOwner)

        verify { mockMapboxNavigation.unregisterLocationObserver(any()) }
        verify { mockMapboxNavigation.unregisterRouteProgressObserver(any()) }
        verify { mockMapboxNavigation.unregisterRoutesObserver(any()) }
        verify { mockMapboxNavigation.unregisterArrivalObserver(any()) }
        verify { mockMapboxNavigation.unregisterBannerInstructionsObserver(any()) }
        verify { mockMapboxNavigation.unregisterTripSessionStateObserver(any()) }
    }

    @Test
    fun rawLocationUpdates() = coroutineRule.runBlockingTest {
        val expected = mockk<Location>()
        val observerSlot = slot<LocationObserver>()
        val lifecycleOwner = mockk< LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerLocationObserver(capture(observerSlot)) }
        val def = async {
            viewModel.rawLocationUpdates().first()
        }

        observerSlot.captured.onNewRawLocation(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun newLocationMatcherResults() = coroutineRule.runBlockingTest {
        val lifecycleOwner = mockk< LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<LocationObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerLocationObserver(capture(observerSlot)) }
        val def = async {
            viewModel.newLocationMatcherResults.first()
        }
        val expected = mockk<LocationMatcherResult>()

        observerSlot.captured.onNewLocationMatcherResult(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun routeProgressUpdates() = coroutineRule.runBlockingTest {
        val expected = mockk<RouteProgress>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<RouteProgressObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerRouteProgressObserver(capture(observerSlot)) }
        val def = async {
            viewModel.routeProgressUpdates.first()
        }

        observerSlot.captured.onRouteProgressChanged(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun routesUpdatedResults() = coroutineRule.runBlockingTest {
        val expected = mockk<RoutesUpdatedResult>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<RoutesObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerRoutesObserver(capture(observerSlot)) }
        val def = async {
            viewModel.routesUpdatedResults.first()
        }

        observerSlot.captured.onRoutesChanged(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun finalDestinationArrivals() = coroutineRule.runBlockingTest {
        val expected = mockk<RouteProgress>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<ArrivalObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerArrivalObserver(capture(observerSlot)) }
        val def = async {
            viewModel.finalDestinationArrivals.first()
        }

        observerSlot.captured.onFinalDestinationArrival(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun nextRouteLegStartUpdates() = coroutineRule.runBlockingTest {
        val expected = mockk<RouteLegProgress>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<ArrivalObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerArrivalObserver(capture(observerSlot)) }
        val def = async {
            viewModel.nextRouteLegStartUpdates.first()
        }

        observerSlot.captured.onNextRouteLegStart(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun wayPointArrivals() = coroutineRule.runBlockingTest {
        val expected = mockk<RouteProgress>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<ArrivalObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerArrivalObserver(capture(observerSlot)) }
        val def = async {
            viewModel.wayPointArrivals.first()
        }

        observerSlot.captured.onWaypointArrival(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun bannerInstructions() = coroutineRule.runBlockingTest {
        val expected = mockk<BannerInstructions>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<BannerInstructionsObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerBannerInstructionsObserver(capture(observerSlot)) }
        val def = async {
            viewModel.bannerInstructions.first()
        }

        observerSlot.captured.onNewBannerInstructions(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun tripSessionStateUpdates() = coroutineRule.runBlockingTest {
        val expected = mockk<TripSessionState>()
        val lifecycleOwner = mockk<LifecycleOwner>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val factory = mockk<DropInUIMapboxNavigationFactory> {
            every { getMapboxNavigation() } returns mockMapboxNavigation
        }
        val observerSlot = slot<TripSessionStateObserver>()
        val viewModel = MapboxNavigationViewModel(factory).also {
            it.onStart(lifecycleOwner)
        }
        verify { mockMapboxNavigation.registerTripSessionStateObserver(capture(observerSlot)) }
        val def = async {
            viewModel.tripSessionStateUpdates.first()
        }

        observerSlot.captured.onSessionStateChanged(expected)
        val result = def.await()

        assertEquals(expected, result)
    }
}
