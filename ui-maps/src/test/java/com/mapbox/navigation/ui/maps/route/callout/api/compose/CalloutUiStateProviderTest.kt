package com.mapbox.navigation.ui.maps.route.callout.api.compose

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.callout.api.MapboxRouteCalloutsApi
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersObserver
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CalloutUiStateProviderTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockRoutesSetToRouteLineDataProvider =
        mockk<RoutesSetToRouteLineDataProvider>(relaxed = true)
    private val mockRoutesAttachedToLayersDataProvider =
        mockk<RoutesAttachedToLayersDataProvider>(relaxed = true)
    private val mockRouteCalloutsApi = mockk<MapboxRouteCalloutsApi>(relaxed = true)
    private val mockRoute1 = mockk<NavigationRoute> {
        every { id } returns "route-1"
    }
    private val mockRoute2 = mockk<NavigationRoute> {
        every { id } returns "route-2"
    }

    private val routesSetObserverSlot = slot<RoutesSetToRouteLineObserver>()
    private val routesAttachedObserverSlot = slot<RoutesAttachedToLayersObserver>()

    @Before
    fun setUp() {
        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } returns Unit
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } returns Unit
    }

    @Test
    fun `constructor registers observers`() {
        CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        verify {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(any())
        }
        verify {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `uiStateData initially contains empty list`() {
        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        val initialState = provider.uiStateData.value

        assertTrue("Initial state should be empty", initialState.callouts.isEmpty())
    }

    @Test
    fun `routesSetToRouteLineObserver updates callouts data`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }
        val calloutData = RouteCalloutData(
            listOf(mockCallout1, mockCallout2),
        )

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns calloutData

        // Mock the MapboxRouteCalloutsApi to return our test data
        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // We need to test the internal logic, so we'll simulate the observer call
        val routes = listOf(mockRoute1, mockRoute2)
        val alternativeMetadata = emptyList<AlternativeRouteMetadata>()

        // Get the registered observer and call it
        val observer = routesSetObserverSlot.captured
        observer.onSet(routes, alternativeMetadata)

        // The uiStateData should still be empty until routes are attached to layers
        val currentState = provider.uiStateData.value
        assertTrue("State should be empty until routes attached", currentState.callouts.isEmpty())
    }

    @Test
    fun `routesAttachedToLayersObserver updates ui state`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }
        val calloutData = RouteCalloutData(
            listOf(mockCallout1, mockCallout2),
        )

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns calloutData

        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // First set the callouts data via routes set observer
        val routesSetObserver = routesSetObserverSlot.captured
        routesSetObserver.onSet(listOf(mockRoute1, mockRoute2), emptyList())

        // Then simulate routes being attached to layers
        val routesToLayers = mapOf(
            "route-1" to "layer-1",
            "route-2" to "layer-2",
        )

        val routesAttachedObserver = routesAttachedObserverSlot.captured
        routesAttachedObserver.onAttached(routesToLayers)

        val currentState = provider.uiStateData.value

        assertEquals(
            listOf(
                CalloutUiState(mockCallout1, "layer-1"),
                CalloutUiState(mockCallout2, "layer-2"),
            ),
            currentState.callouts,
        )
    }

    @Test
    fun `only routes with matching layers are included in ui state`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockk<NavigationRoute> {
                every { id } returns "route-3"
            }
        }
        val calloutData = RouteCalloutData(
            listOf(mockCallout1, mockCallout2),
        )

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns calloutData

        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Set callouts data for both routes
        val routesSetObserver = routesSetObserverSlot.captured
        routesSetObserver.onSet(listOf(mockRoute1, mockRoute2), emptyList())

        // Only route-1 is attached to a layer
        val routesToLayers = mapOf("route-1" to "layer-1", "route-2" to "layer-2")

        val routesAttachedObserver = routesAttachedObserverSlot.captured
        routesAttachedObserver.onAttached(routesToLayers)

        assertEquals(
            listOf(
                CalloutUiState(mockCallout1, "layer-1"),
            ),
            provider.uiStateData.value.callouts,
        )
    }

    @Test
    fun `empty routes to layers map results in empty ui state`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns RouteCalloutData(listOf(mockCallout1, mockCallout2))

        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Set callouts data
        val routesSetObserver = routesSetObserverSlot.captured
        routesSetObserver.onSet(listOf(mockRoute1, mockRoute2), emptyList())

        // Empty routes to layers map
        val routesToLayers = emptyMap<String, String>()

        val routesAttachedObserver = routesAttachedObserverSlot.captured
        routesAttachedObserver.onAttached(routesToLayers)

        assertEquals(
            emptyList<CalloutUiState>(),
            provider.uiStateData.value.callouts,
        )
    }

    @Test
    fun `destroy unregisters observers`() {
        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        provider.destroy()

        verify {
            mockRoutesSetToRouteLineDataProvider.unregisterRoutesSetToRouteLineObserver(any())
        }
        verify {
            mockRoutesAttachedToLayersDataProvider.unregisterRoutesAttachedToLayersObserver(any())
        }
    }

    @Test
    fun `multiple updates to routes attached to layers work correctly`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1), any())
        } returns RouteCalloutData(listOf(mockCallout1))
        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns RouteCalloutData(listOf(mockCallout1, mockCallout2))
        every {
            mockRouteCalloutsApi.setNavigationRoutes(emptyList(), any())
        } returns RouteCalloutData(emptyList())

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns RouteCalloutData(listOf(mockCallout1, mockCallout2))

        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Set callouts data
        val routesSetObserver = routesSetObserverSlot.captured
        routesSetObserver.onSet(listOf(mockRoute1, mockRoute2), emptyList())

        val routesAttachedObserver = routesAttachedObserverSlot.captured

        // First update - only route-1 attached
        routesAttachedObserver.onAttached(mapOf("route-1" to "layer-1"))
        var currentState = provider.uiStateData.value
        assertEquals(
            listOf(CalloutUiState(mockCallout1, "layer-1")),
            currentState.callouts,
        )

        // Second update - both routes attached
        routesAttachedObserver.onAttached(
            mapOf("route-1" to "layer-1", "route-2" to "layer-2"),
        )

        currentState = provider.uiStateData.value
        assertEquals(
            listOf(
                CalloutUiState(mockCallout1, "layer-1"),
                CalloutUiState(mockCallout2, "layer-2"),
            ),
            currentState.callouts,
        )

        // Third update - no routes attached
        routesAttachedObserver.onAttached(emptyMap())
        currentState = provider.uiStateData.value
        assertEquals(
            emptyList<CalloutUiState>(),
            currentState.callouts,
        )
    }

    @Test
    fun `ui state updates correctly when callouts data changes`() {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1), any())
        } returns RouteCalloutData(listOf(mockCallout1))
        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns RouteCalloutData(listOf(mockCallout1, mockCallout2))

        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        val routesSetObserver = routesSetObserverSlot.captured
        val routesAttachedObserver = routesAttachedObserverSlot.captured

        // Set initial callouts data and layer mapping
        routesSetObserver.onSet(listOf(mockRoute1), emptyList())
        routesAttachedObserver.onAttached(mapOf("route-1" to "layer-1"))

        var currentState = provider.uiStateData.value
        assertEquals(
            listOf(
                CalloutUiState(mockCallout1, "layer-1"),
            ),
            currentState.callouts,
        )

        // Update callouts data with new route
        routesSetObserver.onSet(
            listOf(mockRoute1, mockRoute2),
            emptyList(),
        )
        routesAttachedObserver.onAttached(
            mapOf(
                "route-1" to "layer-1",
                "route-2" to "layer-2",
            ),
        )

        currentState = provider.uiStateData.value
        assertEquals(
            listOf(
                CalloutUiState(mockCallout1, "layer-1"),
                CalloutUiState(mockCallout2, "layer-2"),
            ),
            currentState.callouts,
        )
    }

    @Test
    fun `ui state is empty when no callouts data is set`() {
        val provider = CalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
        )

        val routesAttachedObserver = routesAttachedObserverSlot.captured

        // Only update routes to layers without setting callouts data
        routesAttachedObserver.onAttached(mapOf("route-1" to "layer-1"))

        val currentState = provider.uiStateData.value
        assertEquals(
            emptyList<CalloutUiState>(),
            currentState.callouts,
        )
    }
}
