@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test

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
        val fasterRouteCallback = mockk<NewFasterRouteObserver>()
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        coEvery {
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
        val fasterRouteCallback = mockk<NewFasterRouteObserver>(relaxed = true)
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        coEvery {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } returns FasterRouteResult.NoFasterRoad

        routeObserver.onRoutesChanged(mockk(relaxed = true))

        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }

    @Test
    fun `old results are ignored when routes are updated faster then processing not processed `() {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<FasterRouteTracker>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTracker = fasterRouteTrackerMock,
        )
        val fasterRouteCallback = mockk<NewFasterRouteObserver>(relaxed = true)
        fasterRoutes.fasterRouteCallback = fasterRouteCallback
        val firstRouteUpdateProcessing = CompletableDeferred<FasterRouteResult>()
        coEvery {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } coAnswers {
            firstRouteUpdateProcessing.await()
        }
        routeObserver.onRoutesChanged(mockk(relaxed = true))
        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
        coEvery {
            fasterRouteTrackerMock.routesUpdated(
                any(),
                any()
            )
        } answers {
            FasterRouteResult.NoFasterRoad
        }
        routeObserver.onRoutesChanged(mockk(relaxed = true))
        firstRouteUpdateProcessing.complete(
            FasterRouteResult.NewFasterRoadFound(
                mockk(), 8.9
            )
        )
        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }
}

private fun createFasterRoutes(
    fasterRouteTracker: FasterRouteTracker = createFasterRoutesTracker(),
    mapboxNavigation: MapboxNavigation = mockk(relaxed = true),
    mainDispatcher: CoroutineDispatcher = TestCoroutineDispatcher(),
) = FasterRoutes(
    mapboxNavigation = mapboxNavigation,
    fasterRouteTracker = fasterRouteTracker,
    mainDispatcher = mainDispatcher
)

private fun MapboxNavigation.recordRoutesObservers(): RoutesObserver {
    val observers = mutableListOf<RoutesObserver>()
    val navigation = this
    every { navigation.registerRoutesObserver(capture(observers)) } returns Unit
    return RoutesObserver { result -> observers.forEach { it.onRoutesChanged(result) } }
}