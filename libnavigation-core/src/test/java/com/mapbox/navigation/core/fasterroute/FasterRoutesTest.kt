@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class FasterRoutesTest {

    @Test
    fun `faster route found`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<FasterRouteTracker>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTracker = fasterRouteTrackerMock
        )
        val fasterRouteCallback = mockk<NewFasterRouteCallback>()
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        every {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } returns FasterRouteResult.NewFasterRoadFound(
            mockk(), 8.9
        )

        routeObserver.onRoutesChanged(mockk(relaxed = true))

        verify(exactly = 1) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }

    @Test
    fun `no faster route found`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<FasterRouteTracker>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTracker = fasterRouteTrackerMock
        )
        val fasterRouteCallback = mockk<NewFasterRouteCallback>(relaxed = true)
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        every {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } returns FasterRouteResult.NoFasterRoad

        routeObserver.onRoutesChanged(mockk(relaxed = true))

        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }

    @Test
    fun `when routes are updated faster then processing not processed old results are ignored`() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<FasterRouteTracker>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTracker = fasterRouteTrackerMock,
            computationDispatcher = Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        )
        val fasterRouteCallback = mockk<NewFasterRouteCallback>(relaxed = true)
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        val countDownLatch = CountDownLatch(1)
        every {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } answers {
            countDownLatch.await()
            FasterRouteResult.NewFasterRoadFound(
                mockk(), 8.9
            )
        }
        routeObserver.onRoutesChanged(mockk(relaxed = true))
        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
        every {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } answers {
            FasterRouteResult.NoFasterRoad
        }
        routeObserver.onRoutesChanged(mockk(relaxed = true))
        countDownLatch.countDown()
        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }
}

private fun createFasterRoutes(
    fasterRouteTracker: FasterRouteTracker = createFasterRoutesTracker(),
    mapboxNavigation: MapboxNavigation = mockk(relaxed = true),
    computationDispatcher: CoroutineDispatcher = TestCoroutineDispatcher(),
    mainDispatcher: CoroutineDispatcher = TestCoroutineDispatcher(),
) = FasterRoutes(
    mapboxNavigation = mapboxNavigation,
    fasterRouteTracker = fasterRouteTracker,
    computationDispatcher = computationDispatcher,
    mainDispatcher = mainDispatcher
)

private fun MapboxNavigation.recordRoutesObservers(): RoutesObserver {
    val observers = mutableListOf<RoutesObserver>()
    val navigation = this
    every { navigation.registerRoutesObserver(capture(observers)) } returns Unit
    return RoutesObserver { result -> observers.forEach { it.onRoutesChanged(result) } }
}