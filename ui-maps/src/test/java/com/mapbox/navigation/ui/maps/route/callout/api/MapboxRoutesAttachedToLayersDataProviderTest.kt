package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesAttachedToLayersObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapboxRoutesAttachedToLayersDataProviderTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private val mockMapboxRouteLineView = mockk<MapboxRouteLineView>(relaxed = true)
    private val internalObserverSlot = slot<RoutesAttachedToLayersObserver>()

    private lateinit var dataProvider: MapboxRoutesAttachedToLayersDataProvider

    @Before
    fun setUp() {
        every {
            mockMapboxRouteLineView.registerRoutesAttachedToLayersObserver(
                capture(internalObserverSlot),
            )
        } just runs

        dataProvider = MapboxRoutesAttachedToLayersDataProvider(mockMapboxRouteLineView)
    }

    @Test
    fun `registerRoutesAttachedToLayersObserver registers first observer with MapboxRouteLineView`() {
        val observer = mockk<RoutesAttachedToLayersObserver>()

        dataProvider.registerRoutesAttachedToLayersObserver(observer)

        verify(exactly = 1) {
            mockMapboxRouteLineView.registerRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `registerRoutesAttachedToLayersObserver does not register second observer with MapboxRouteLineView`() {
        val observer1 = mockk<RoutesAttachedToLayersObserver>()
        val observer2 = mockk<RoutesAttachedToLayersObserver>()

        dataProvider.registerRoutesAttachedToLayersObserver(observer1)
        dataProvider.registerRoutesAttachedToLayersObserver(observer2)

        verify(exactly = 1) {
            mockMapboxRouteLineView.registerRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `unregisterRoutesAttachedToLayersObserver does not unregister when other observers remain`() {
        val observer1 = mockk<RoutesAttachedToLayersObserver>()
        val observer2 = mockk<RoutesAttachedToLayersObserver>()

        dataProvider.registerRoutesAttachedToLayersObserver(observer1)
        dataProvider.registerRoutesAttachedToLayersObserver(observer2)
        dataProvider.unregisterRoutesAttachedToLayersObserver(observer1)

        verify(exactly = 0) {
            mockMapboxRouteLineView.unregisterRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `unregisterRoutesAttachedToLayersObserver unregisters when last observer is removed`() {
        val observer1 = mockk<RoutesAttachedToLayersObserver>()
        val observer2 = mockk<RoutesAttachedToLayersObserver>()

        dataProvider.registerRoutesAttachedToLayersObserver(observer1)
        dataProvider.registerRoutesAttachedToLayersObserver(observer2)
        dataProvider.unregisterRoutesAttachedToLayersObserver(observer1)
        dataProvider.unregisterRoutesAttachedToLayersObserver(observer2)

        verify(exactly = 1) {
            mockMapboxRouteLineView.unregisterRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `internal observer notifies all registered observers`() {
        val observer1 = mockk<RoutesAttachedToLayersObserver>(relaxed = true)
        val observer2 = mockk<RoutesAttachedToLayersObserver>(relaxed = true)
        val routesToLayers = mapOf("route-1" to "layer-1", "route-2" to "layer-2")

        dataProvider.registerRoutesAttachedToLayersObserver(observer1)
        dataProvider.registerRoutesAttachedToLayersObserver(observer2)

        // Simulate the internal observer being called
        internalObserverSlot.captured.onAttached(routesToLayers)

        verify { observer1.onAttached(routesToLayers) }
        verify { observer2.onAttached(routesToLayers) }
    }

    @Test
    fun `internal observer does not notify unregistered observers`() {
        val observer1 = mockk<RoutesAttachedToLayersObserver>(relaxed = true)
        val observer2 = mockk<RoutesAttachedToLayersObserver>(relaxed = true)
        val routesToLayers = mapOf("route-1" to "layer-1")

        dataProvider.registerRoutesAttachedToLayersObserver(observer1)
        dataProvider.registerRoutesAttachedToLayersObserver(observer2)
        dataProvider.unregisterRoutesAttachedToLayersObserver(observer1)

        // Simulate the internal observer being called
        internalObserverSlot.captured.onAttached(routesToLayers)

        verify(exactly = 0) { observer1.onAttached(any()) }
        verify { observer2.onAttached(routesToLayers) }
    }

    @Test
    fun `multiple notifications work correctly`() {
        val observer = mockk<RoutesAttachedToLayersObserver>(relaxed = true)
        val routesToLayers1 = mapOf("route-1" to "layer-1")
        val routesToLayers2 = mapOf("route-2" to "layer-2")

        dataProvider.registerRoutesAttachedToLayersObserver(observer)

        // Simulate multiple notifications
        internalObserverSlot.captured.onAttached(routesToLayers1)
        internalObserverSlot.captured.onAttached(routesToLayers2)

        verify { observer.onAttached(routesToLayers1) }
        verify { observer.onAttached(routesToLayers2) }
    }
}
