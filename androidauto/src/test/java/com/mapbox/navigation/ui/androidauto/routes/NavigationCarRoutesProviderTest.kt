package com.mapbox.navigation.ui.androidauto.routes

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationCarRoutesProviderTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    var carAppTestRule = CarAppTestRule()

    private val sut = NavigationCarRoutesProvider()

    @Test
    fun `does not call MapboxNavigationApp by default`() {
        NavigationCarRoutesProvider()

        verify(exactly = 0) {
            MapboxNavigationApp.registerObserver(any())
            MapboxNavigationApp.unregisterObserver(any())
            MapboxNavigationApp.current()
        }
    }

    @Test
    fun `navigationRoutes will collect route changes`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<List<NavigationRoute>>()
        val routesObserverSlot = slot<RoutesObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRoutesObserver(capture(routesObserverSlot)) } just Runs
            every { unregisterRoutesObserver(capture(routesObserverSlot)) } just Runs
        }

        val results = async { sut.navigationRoutes.collect { resultsSlot.add(it) } }
        carAppTestRule.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk(), mockk()) },
        )
        results.cancelAndJoin()

        assertEquals(1, resultsSlot.size)
    }

    @Test
    fun `navigationRoutes will collect multiple route changes`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<List<NavigationRoute>>()
        val routesObserverSlot = slot<RoutesObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRoutesObserver(capture(routesObserverSlot)) } just Runs
            every { unregisterRoutesObserver(capture(routesObserverSlot)) } just Runs
        }

        val results = async { sut.navigationRoutes.collect { resultsSlot.add(it) } }
        carAppTestRule.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk(), mockk()) },
        )
        routesObserverSlot.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(mockk(), mockk()) },
        )
        results.cancelAndJoin()

        assertEquals(2, resultsSlot.size)
        assertEquals(2, resultsSlot[0].size)
    }

    @Test
    fun `navigationRoutes will unregister when MapboxNavigation is detached`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<List<NavigationRoute>>()
        val routesObserverSlot = slot<RoutesObserver>()
        val mapboxNavigation: MapboxNavigation = mockk {
            every { registerRoutesObserver(capture(routesObserverSlot)) } just Runs
            every { unregisterRoutesObserver(any()) } just Runs
        }

        val results = async { sut.navigationRoutes.collect { resultsSlot.add(it) } }
        carAppTestRule.onAttached(mapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
            },
        )
        carAppTestRule.onDetached(mapboxNavigation)
        results.cancelAndJoin()

        assertEquals(2, resultsSlot.size)
        assertEquals(2, resultsSlot[0].size)
        assertEquals(0, resultsSlot[1].size)
        verifyOrder {
            MapboxNavigationApp.registerObserver(any())
            MapboxNavigationApp.unregisterObserver(any())
        }
    }

    @Test
    fun `navigationRoutes will collect from new instances of MapboxNavigation`() = coroutineRule.runBlockingTest {
        val resultsSlot = mutableListOf<List<NavigationRoute>>()
        val firstRoutesObserverSlot = slot<RoutesObserver>()
        val firstMapboxNavigation: MapboxNavigation = mockk {
            every { registerRoutesObserver(capture(firstRoutesObserverSlot)) } just Runs
            every { unregisterRoutesObserver(any()) } just Runs
        }
        val secondRoutesObserverSlot = slot<RoutesObserver>()
        val secondMapboxNavigation: MapboxNavigation = mockk {
            every { registerRoutesObserver(capture(secondRoutesObserverSlot)) } just Runs
            every { unregisterRoutesObserver(any()) } just Runs
        }

        val results = async { sut.navigationRoutes.collect { resultsSlot.add(it) } }
        carAppTestRule.onAttached(firstMapboxNavigation)
        firstRoutesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk())
            },
        )
        carAppTestRule.onDetached(firstMapboxNavigation)
        carAppTestRule.onAttached(secondMapboxNavigation)
        secondRoutesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns listOf(mockk(), mockk(), mockk())
            },
        )
        carAppTestRule.onDetached(secondMapboxNavigation)
        results.cancelAndJoin()

        // Verify there were 4 route changes
        // 1. firstMapboxNavigation is attached and emits 2 routes
        // 2. firstMapboxNavigation is detached and emits empty
        // 3. secondMapboxNavigation is attached and emits 3 routes
        // 4. secondMapboxNavigation is detached and emits empty
        assertEquals(4, resultsSlot.size)
        assertEquals(2, resultsSlot[0].size)
        assertEquals(0, resultsSlot[1].size)
        assertEquals(3, resultsSlot[2].size)
        assertEquals(0, resultsSlot[3].size)
        verifyOrder {
            MapboxNavigationApp.registerObserver(any())
            firstMapboxNavigation.registerRoutesObserver(any())
            firstMapboxNavigation.unregisterRoutesObserver(any())
            secondMapboxNavigation.registerRoutesObserver(any())
            secondMapboxNavigation.unregisterRoutesObserver(any())
            MapboxNavigationApp.unregisterObserver(any())
        }
    }
}
