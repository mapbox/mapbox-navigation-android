package com.mapbox.navigation.dropin

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val mapboxNavigationObserver = slot<MapboxNavigationObserver>()
    private lateinit var viewModel: MapboxNavigationViewModel

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
        every {
            MapboxNavigationApp.setup(any())
        } returns MapboxNavigationApp
        every {
            MapboxNavigationApp.registerObserver(capture(mapboxNavigationObserver))
        } returns MapboxNavigationApp

        viewModel = MapboxNavigationViewModel()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `verify MapboxNavigation is registered onAttached`() {
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

        verify { mockMapboxNavigation.registerLocationObserver(any()) }
        verify { mockMapboxNavigation.registerRouteProgressObserver(any()) }
        verify { mockMapboxNavigation.registerRoutesObserver(any()) }
        verify { mockMapboxNavigation.registerArrivalObserver(any()) }
        verify { mockMapboxNavigation.registerBannerInstructionsObserver(any()) }
        verify { mockMapboxNavigation.registerTripSessionStateObserver(any()) }
    }

    @Test
    fun `verify MapboxNavigation is unregistered onDetached`() {
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onDetached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<LocationObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<RouteProgressObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<RoutesObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<ArrivalObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<ArrivalObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val observerSlot = slot<ArrivalObserver>()
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

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
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

        val observerSlot = slot<BannerInstructionsObserver>()
        verify { mockMapboxNavigation.registerBannerInstructionsObserver(capture(observerSlot)) }
        val def = async {
            viewModel.bannerInstructions.first()
        }

        observerSlot.captured.onNewBannerInstructions(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun voiceInstructions() = coroutineRule.runBlockingTest {
        val expected = mockk<VoiceInstructions>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)

        val observerSlot = slot<VoiceInstructionsObserver>()
        verify { mockMapboxNavigation.registerVoiceInstructionsObserver(capture(observerSlot)) }
        val def = async {
            viewModel.voiceInstructions.first()
        }

        observerSlot.captured.onNewVoiceInstructions(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun tripSessionStateUpdates() = coroutineRule.runBlockingTest {
        val expected = mockk<TripSessionState>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mapboxNavigationObserver.captured.onAttached(mockMapboxNavigation)
        val observerSlot = slot<TripSessionStateObserver>()
        verify { mockMapboxNavigation.registerTripSessionStateObserver(capture(observerSlot)) }
        val def = async {
            viewModel.tripSessionStateUpdates.first()
        }

        observerSlot.captured.onSessionStateChanged(expected)
        val result = def.await()

        assertEquals(expected, result)
    }

    @Test
    fun setRoutes() {
        val routes = listOf<DirectionsRoute>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation

        viewModel.setRoutes(routes)

        verify { mockMapboxNavigation.setRoutes(routes) }
    }

    @Test
    fun fetchAndSetRoute() {
        val points = listOf(
            Point.fromLngLat(14.75, 55.19),
            Point.fromLngLat(12.54, 55.68)
        )
        val options = RouteOptions.builder()
            .profile("foobar")
            .coordinatesList(points)
            .layersList(listOf(1))
            .build()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation

        viewModel.fetchAndSetRoute(options)

        verify { mockMapboxNavigation.requestRoutes(options, any()) }
    }

    @Test
    fun fetchAndSetRoute_withRouteOptionsLayerListUpdate() {
        val points = listOf(
            Point.fromLngLat(14.75, 55.19),
            Point.fromLngLat(12.54, 55.68)
        )
        val options = RouteOptions.builder()
            .profile("foobar")
            .coordinatesList(points)
            .build()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { getZLevel() } returns 99
        }
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation
        val optionsSlot = slot<RouteOptions>()

        viewModel.fetchAndSetRoute(options)

        verify { mockMapboxNavigation.requestRoutes(capture(optionsSlot), any()) }
        assertEquals(99, optionsSlot.captured.layersList()!!.first())
    }

    @Test
    fun fetchAndSetRoute_onRoutesReady() {
        val points = listOf(
            Point.fromLngLat(14.75, 55.19),
            Point.fromLngLat(12.54, 55.68)
        )
        val options = RouteOptions.builder()
            .profile("foobar")
            .coordinatesList(points)
            .layersList(listOf(1))
            .build()
        val routes = listOf<DirectionsRoute>()
        val callbackSlot = slot<RouterCallback>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { requestRoutes(options, capture(callbackSlot)) } answers {
                callbackSlot.captured.onRoutesReady(routes, mockk<RouterOrigin>())
                0L
            }
        }
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation

        viewModel.fetchAndSetRoute(options)

        verify { mockMapboxNavigation.requestRoutes(options, any()) }
        verify { mockMapboxNavigation.setRoutes(routes) }
    }
}
