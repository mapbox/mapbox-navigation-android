package com.mapbox.navigation.dropin.tripsession

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifyOrder
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class ReplayRouteTripSessionTest {

    private lateinit var context: Context
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var replayer: MapboxReplayer
    private lateinit var options: NavigationOptions
    private lateinit var sut: ReplayRouteTripSession

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        replayer = mockk(relaxed = true)
        options = mockk(relaxed = true) {
            every { applicationContext } returns context
        }
        mapboxNavigation = mockk(relaxed = true) {
            every { mapboxReplayer } returns replayer
            every { navigationOptions } returns options
        }

        sut = ReplayRouteTripSession()
    }

    @Test
    fun `start - should stop trip session and start replay session`() {
        sut.start(mapboxNavigation)

        verifyOrder {
            mapboxNavigation.stopTripSession()
            mapboxNavigation.startReplayTripSession()
            replayer.pushRealLocation(any(), 0.0)
            replayer.play()
        }
    }

    @Test
    fun `start - should reset trip session and replayer when navigation routes are cleared`() {
        val routesObserver = slot<RoutesObserver>()
        every { mapboxNavigation.registerRoutesObserver(capture(routesObserver)) } returns Unit
        sut.start(mapboxNavigation)

        routesObserver.captured.apply {
            val result = mockk<RoutesUpdatedResult> {
                every { navigationRoutes } returns emptyList()
            }
            onRoutesChanged(result)
        }

        verifyOrder {
            replayer.clearEvents()
            mapboxNavigation.resetTripSession()
            replayer.pushRealLocation(any(), 0.0)
            replayer.play()
        }
    }

    @Test
    fun `start - should register ReplayProgressObserver`() {
        val progressObserver = slot<RouteProgressObserver>()
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(progressObserver))
        } returns Unit
        sut.start(mapboxNavigation)

        assertTrue(progressObserver.captured is ReplayProgressObserver)
    }

    @Test
    fun `stop - should unregister ReplayProgressObserver`() {
        val progressObserver = slot<RouteProgressObserver>()
        every {
            mapboxNavigation.unregisterRouteProgressObserver(capture(progressObserver))
        } returns Unit
        sut.start(mapboxNavigation)
        sut.stop(mapboxNavigation)

        assertTrue(progressObserver.captured is ReplayProgressObserver)
    }

    @Test
    fun `stop - should stop trip session and replayer`() {
        sut.stop(mapboxNavigation)

        verifyOrder {
            replayer.stop()
            replayer.clearEvents()
            mapboxNavigation.stopTripSession()
        }
    }
}
