package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.navigation.NavigationState
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class RouteAlternativeStateControllerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val alternativeObserverSlot = slot<NavigationRouteAlternativesObserver>()
    private val store = spyk(TestStore())
    private val sut = RouteAlternativeStateController(store)

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `alternative routes observer when current and alternative routes are offboard`() =
        runBlockingTest {
            val route1 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Offboard
            }
            val route2 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Offboard
            }
            val alternativeRoutes = listOf(route2)
            val mockProgress = mockk<RouteProgress>(relaxed = true) {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                }
            }
            val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
                every {
                    registerRouteAlternativesObserver(capture(alternativeObserverSlot))
                } just Runs
                every { getNavigationRoutes() } returns listOf(route1)
            }

            sut.onAttached(mapboxNavigation)

            alternativeObserverSlot.captured.onRouteAlternatives(
                mockProgress,
                alternativeRoutes,
                RouterOrigin.Offboard
            )

            verify {
                store.dispatch(RoutesAction.SetRoutesWithIndex(listOf(route1, route2), 0))
            }
        }

    @Test
    fun `alternative routes observer when alternative routes are offboard`() =
        runBlockingTest {
            val route1 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Onboard
            }
            val route2 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Offboard
            }
            val alternativeRoutes = listOf(route2)
            val mockProgress = mockk<RouteProgress>(relaxed = true) {
                every { currentLegProgress } returns mockk {
                    every { legIndex } returns 0
                }
            }
            val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
                every {
                    registerRouteAlternativesObserver(capture(alternativeObserverSlot))
                } just Runs
                every { getNavigationRoutes() } returns listOf(route1)
            }

            sut.onAttached(mapboxNavigation)

            alternativeObserverSlot.captured.onRouteAlternatives(
                mockProgress,
                alternativeRoutes,
                RouterOrigin.Offboard
            )

            verify {
                store.dispatch(RoutesAction.SetRoutesWithIndex(listOf(route2), 0))
            }
        }

    @Test
    fun `alternative routes observer when all routes are onboard`() =
        runBlockingTest {
            val route1 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Onboard
            }
            val route2 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Onboard
            }
            val alternativeRoutes = listOf(route2)
            val mockProgress = mockk<RouteProgress>(relaxed = true)
            val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
                every {
                    registerRouteAlternativesObserver(capture(alternativeObserverSlot))
                } just Runs
                every { getNavigationRoutes() } returns listOf(route1)
            }
            every { store.state.value.navigation } returns NavigationState.RoutePreview

            sut.onAttached(mapboxNavigation)

            alternativeObserverSlot.captured.onRouteAlternatives(
                mockProgress,
                alternativeRoutes,
                RouterOrigin.Offboard
            )

            verify {
                store.dispatch(RoutesAction.SetRoutesWithIndex(listOf(route1, route2), 0))
            }
        }

    @Test
    fun `alternative routes observer when there is no primary route`() =
        runBlockingTest {
            val route1 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Offboard
            }
            val route2 = mockk<NavigationRoute> {
                every { origin } returns RouterOrigin.Offboard
            }
            val alternativeRoutes = listOf(route2)
            val mockProgress = mockk<RouteProgress>(relaxed = true)
            val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
                every {
                    registerRouteAlternativesObserver(capture(alternativeObserverSlot))
                } just Runs
                every { getNavigationRoutes() } returns listOf()
            }

            sut.onAttached(mapboxNavigation)

            alternativeObserverSlot.captured.onRouteAlternatives(
                mockProgress,
                alternativeRoutes,
                RouterOrigin.Offboard
            )

            verify(exactly = 0) {
                store.dispatch(RoutesAction.SetRoutesWithIndex(listOf(route1, route2), 0))
            }
        }
}
