@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class FasterRoutesTrackerTest {

    @Test
    fun `faster route found`() = runBlockingTest {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<ComparisonFasterRouteTrackerCore>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTrackerCore = fasterRouteTrackerMock
        )
        val fasterRouteCallback = mockk<NewFasterRouteObserver>(relaxed = true)
        fasterRoutes.registerNewFasterRouteObserver(fasterRouteCallback)
        coEvery {
            fasterRouteTrackerMock.findFasterRouteInUpdate(
                any(),
                any()
            )
        } returns createNewFasterRouteFoundForTest()

        routeObserver.onRoutesChanged(mockk(relaxed = true))

        verify(exactly = 1) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }

    @Test
    fun `no faster route found`() = runBlockingTest {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<ComparisonFasterRouteTrackerCore>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTrackerCore = fasterRouteTrackerMock
        )
        val fasterRouteCallback = mockk<NewFasterRouteObserver>(relaxed = true)
        fasterRoutes.registerNewFasterRouteObserver(fasterRouteCallback)
        coEvery {
            fasterRouteTrackerMock.findFasterRouteInUpdate(
                any(),
                any()
            )
        } returns FasterRouteResult.NoFasterRoute

        routeObserver.onRoutesChanged(mockk(relaxed = true))

        verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
    }

    @Test
    fun `old results are ignored when routes are updated faster then processing not processed `() =
        runBlockingTest {
            val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
            val fasterRouteTrackerMock = mockk<ComparisonFasterRouteTrackerCore>()
            val routeObserver = mapboxNavigation.recordRoutesObservers()
            val fasterRoutes = createFasterRoutes(
                mapboxNavigation = mapboxNavigation,
                fasterRouteTrackerCore = fasterRouteTrackerMock,
            )
            val fasterRouteCallback = mockk<NewFasterRouteObserver>(relaxed = true)
            fasterRoutes.registerNewFasterRouteObserver(fasterRouteCallback)
            val firstRouteUpdateProcessing = CompletableDeferred<FasterRouteResult>()
            coEvery {
                fasterRouteTrackerMock.findFasterRouteInUpdate(
                    any(),
                    any()
                )
            } coAnswers {
                firstRouteUpdateProcessing.await()
            }
            routeObserver.onRoutesChanged(mockk(relaxed = true))
            verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
            coEvery {
                fasterRouteTrackerMock.findFasterRouteInUpdate(
                    any(),
                    any()
                )
            } answers {
                FasterRouteResult.NoFasterRoute
            }
            routeObserver.onRoutesChanged(mockk(relaxed = true))
            firstRouteUpdateProcessing.complete(
                createNewFasterRouteFoundForTest()
            )
            verify(exactly = 0) { fasterRouteCallback.onNewFasterRouteFound(any()) }
        }

    @Test
    fun `accept faster route`() = runBlockingTest {
        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val fasterRouteTrackerMock = mockk<ComparisonFasterRouteTrackerCore>()
        val routeObserver = mapboxNavigation.recordRoutesObservers()
        val fasterRoutes = createFasterRoutes(
            mapboxNavigation = mapboxNavigation,
            fasterRouteTrackerCore = fasterRouteTrackerMock
        )
        val currentRoutes = createNavigationRoutes(
            response = createDirectionsResponse(
                uuid = "testRoutes",
                routes = listOf(
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                    createDirectionsRoute(),
                )
            )
        )
        every { mapboxNavigation.getNavigationRoutes() } returns currentRoutes
        coEvery {
            fasterRouteTrackerMock.findFasterRouteInUpdate(
                any(),
                any()
            )
        } returns createNewFasterRouteFoundForTest(route = currentRoutes.last())

        fasterRoutes.registerNewFasterRouteObserver {
            fasterRoutes.acceptFasterRoute(it)
        }
        routeObserver.onRoutesChanged(mockk(relaxed = true))

        val expectedRoutes = listOf(currentRoutes[2], currentRoutes[0], currentRoutes[1])
        verify { mapboxNavigation.setNavigationRoutes(expectedRoutes) }
    }
}

private fun CoroutineScope.createFasterRoutes(
    fasterRouteTrackerCore: ComparisonFasterRouteTrackerCore = createFasterRoutesTracker(),
    mapboxNavigation: MapboxNavigation = mockk(relaxed = true),
) = FasterRoutesTracker(
    mapboxNavigation = mapboxNavigation,
    fasterRouteTrackerCore = fasterRouteTrackerCore,
    this
)

private fun MapboxNavigation.recordRoutesObservers(): RoutesObserver {
    val observers = mutableListOf<RoutesObserver>()
    val navigation = this
    every { navigation.registerRoutesObserver(capture(observers)) } returns Unit
    return RoutesObserver { result -> observers.forEach { it.onRoutesChanged(result) } }
}

private fun createNewFasterRouteFoundForTest(
    route: NavigationRoute = mockk(relaxed = true)
) = FasterRouteResult.NewFasterRouteFound(
    route,
    8.9,
    1
)
