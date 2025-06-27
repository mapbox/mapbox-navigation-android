package com.mapbox.navigation.ui.maps.route.callout.api.compose

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapboxRoutesSetToRouteLineDataProviderTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    private val mockMapboxRouteLineApi = mockk<MapboxRouteLineApi>(relaxed = true)
    private val internalObserverSlot = slot<RoutesSetToRouteLineObserver>()

    private val mockRoute1 = mockk<NavigationRoute> {
        every { id } returns "route-1"
    }
    private val mockRoute2 = mockk<NavigationRoute> {
        every { id } returns "route-2"
    }
    private val mockAlternativeMetadata = mockk<AlternativeRouteMetadata>()

    private lateinit var dataProvider: MapboxRoutesSetToRouteLineDataProvider

    @Before
    fun setUp() {
        every {
            mockMapboxRouteLineApi.registerRoutesSetToRouteLineObserver(
                capture(internalObserverSlot),
            )
        } just runs

        dataProvider = MapboxRoutesSetToRouteLineDataProvider(mockMapboxRouteLineApi)
    }

    @Test
    fun `registerRoutesSetToRouteLineObserver registers first observer with MapboxRouteLineApi`() {
        val observer = mockk<RoutesSetToRouteLineObserver>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer)

        verify(exactly = 1) {
            mockMapboxRouteLineApi.registerRoutesSetToRouteLineObserver(any())
        }
    }

    @Test
    fun `registerRoutesSetToRouteLineObserver does not register second observer with MapboxRouteLineApi`() {
        val observer1 = mockk<RoutesSetToRouteLineObserver>()
        val observer2 = mockk<RoutesSetToRouteLineObserver>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer1)
        dataProvider.registerRoutesSetToRouteLineObserver(observer2)

        verify(exactly = 1) {
            mockMapboxRouteLineApi.registerRoutesSetToRouteLineObserver(any())
        }
    }

    @Test
    fun `unregisterRoutesSetToRouteLineObserver does not unregister when other observers remain`() {
        val observer1 = mockk<RoutesSetToRouteLineObserver>()
        val observer2 = mockk<RoutesSetToRouteLineObserver>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer1)
        dataProvider.registerRoutesSetToRouteLineObserver(observer2)
        dataProvider.unregisterRoutesSetToRouteLineObserver(observer1)

        verify(exactly = 0) {
            mockMapboxRouteLineApi.unregisterRoutesSetToRouteLineObserver(any())
        }
    }

    @Test
    fun `unregisterRoutesSetToRouteLineObserver unregisters when last observer is removed`() {
        val observer1 = mockk<RoutesSetToRouteLineObserver>()
        val observer2 = mockk<RoutesSetToRouteLineObserver>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer1)
        dataProvider.registerRoutesSetToRouteLineObserver(observer2)
        dataProvider.unregisterRoutesSetToRouteLineObserver(observer1)
        dataProvider.unregisterRoutesSetToRouteLineObserver(observer2)

        verify(exactly = 1) {
            mockMapboxRouteLineApi.unregisterRoutesSetToRouteLineObserver(any())
        }
    }

    @Test
    fun `internal observer notifies all registered observers with routes and metadata`() {
        val observer1 = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val observer2 = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val routes = listOf(mockRoute1, mockRoute2)
        val metadata = listOf(mockAlternativeMetadata)

        dataProvider.registerRoutesSetToRouteLineObserver(observer1)
        dataProvider.registerRoutesSetToRouteLineObserver(observer2)

        // Simulate the internal observer being called
        internalObserverSlot.captured.onSet(routes, metadata)

        verify { observer1.onSet(routes, metadata) }
        verify { observer2.onSet(routes, metadata) }
    }

    @Test
    fun `internal observer does not notify unregistered observers`() {
        val observer1 = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val observer2 = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val routes = listOf(mockRoute1)
        val metadata = emptyList<AlternativeRouteMetadata>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer1)
        dataProvider.registerRoutesSetToRouteLineObserver(observer2)
        dataProvider.unregisterRoutesSetToRouteLineObserver(observer1)

        // Simulate the internal observer being called
        internalObserverSlot.captured.onSet(routes, metadata)

        verify(exactly = 0) { observer1.onSet(any(), any()) }
        verify { observer2.onSet(routes, metadata) }
    }

    @Test
    fun `multiple notifications work correctly`() {
        val observer = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val routes1 = listOf(mockRoute1)
        val routes2 = listOf(mockRoute1, mockRoute2)
        val metadata1 = emptyList<AlternativeRouteMetadata>()
        val metadata2 = listOf(mockAlternativeMetadata)

        dataProvider.registerRoutesSetToRouteLineObserver(observer)

        // Simulate multiple notifications
        internalObserverSlot.captured.onSet(routes1, metadata1)
        internalObserverSlot.captured.onSet(routes2, metadata2)

        verify { observer.onSet(routes1, metadata1) }
        verify { observer.onSet(routes2, metadata2) }
    }

    @Test
    fun `internal observer handles empty routes and metadata correctly`() {
        val observer = mockk<RoutesSetToRouteLineObserver>(relaxed = true)
        val emptyRoutes = emptyList<NavigationRoute>()
        val emptyMetadata = emptyList<AlternativeRouteMetadata>()

        dataProvider.registerRoutesSetToRouteLineObserver(observer)

        // Simulate notification with empty collections
        internalObserverSlot.captured.onSet(emptyRoutes, emptyMetadata)

        verify { observer.onSet(emptyRoutes, emptyMetadata) }
    }
}
