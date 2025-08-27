@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsApi
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesAttachedToLayersObserver
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPreviewMapboxNavigationAPI::class)
class RouteCalloutUiStateProviderTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val mockRoutesSetToRouteLineDataProvider = mockk<RoutesSetToRouteLineDataProvider>(
        relaxed = true,
    )
    private val mockRoutesAttachedToLayersDataProvider = mockk<RoutesAttachedToLayersDataProvider>(
        relaxed = true,
    )
    private val mockRouteCalloutsApi = mockk<MapboxRouteCalloutsApi>(relaxed = true)
    private val mockRoute1 = mockk<NavigationRoute> {
        every { id } returns "route-1"
    }
    private val mockRoute2 = mockk<NavigationRoute> {
        every { id } returns "route-2"
    }

    private val routesSetObserverSlot = slot<RoutesSetToRouteLineObserver>()
    private val routesAttachedObserverSlot = slot<RoutesAttachedToLayersObserver>()

    @Test
    fun `ui state is not set when we only have callouts data`() = runTest {
        val mockCallout1 =
            mockk<RouteCallout> {
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
        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // We need to test the internal logic, so we'll simulate the observer call
        val routes = listOf(mockRoute1, mockRoute2)
        val alternativeMetadata = emptyList<AlternativeRouteMetadata>()

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(routes, alternativeMetadata)
        }

        val currentState = withTimeoutOrNull(500) {
            provider.uiStateData.first()
        }

        // The uiStateData should still be empty until routes are attached to layers
        assertNull(currentState)
    }

    @Test
    fun `routesAttachedToLayersObserver updates ui state`() = runTest {
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

        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Then simulate routes being attached to layers
        val routesToLayers = mapOf(
            "route-1" to "layer-1",
            "route-2" to "layer-2",
        )

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(listOf(mockRoute1, mockRoute2), emptyList())
        }
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(routesToLayers)
        }

        val currentState = provider.uiStateData.first()

        assertEquals(
            listOf(
                RouteCalloutUiState(mockCallout1, "layer-1"),
                RouteCalloutUiState(mockCallout2, "layer-2"),
            ),
            currentState.callouts,
        )
    }

    @Test
    fun `only routes with matching layers are included in ui state`() = runTest {
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

        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Only route-1 is attached to a layer
        val routesToLayers = mapOf("route-1" to "layer-1", "route-2" to "layer-2")

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(listOf(mockRoute1, mockRoute2), emptyList())
        }
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(routesToLayers)
        }

        assertEquals(
            listOf(
                RouteCalloutUiState(mockCallout1, "layer-1"),
            ),
            provider.uiStateData.first().callouts,
        )
    }

    @Test
    fun `empty routes to layers map results in empty ui state`() = runTest {
        val mockCallout1 = mockk<RouteCallout> {
            every { route } returns mockRoute1
        }
        val mockCallout2 = mockk<RouteCallout> {
            every { route } returns mockRoute2
        }

        every {
            mockRouteCalloutsApi.setNavigationRoutes(listOf(mockRoute1, mockRoute2), any())
        } returns RouteCalloutData(listOf(mockCallout1, mockCallout2))

        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        // Empty routes to layers map
        val routesToLayers = emptyMap<String, String>()

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(listOf(mockRoute1, mockRoute2), emptyList())
        }
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(routesToLayers)
        }

        assertEquals(
            emptyList<RouteCalloutUiState>(),
            provider.uiStateData.first().callouts,
        )
    }

    @Test
    fun `multiple updates to routes attached to layers work correctly`() = runTest {
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

        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(listOf(mockRoute1, mockRoute2), emptyList())
        }
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(mapOf("route-1" to "layer-1"))
            routesAttachedObserverSlot.captured.onAttached(
                mapOf("route-1" to "layer-1", "route-2" to "layer-2"),
            )
            routesAttachedObserverSlot.captured.onAttached(emptyMap())
        }

        val currentStates = provider.uiStateData.take(3).toList()

        // First update - only route-1 attached
        assertEquals(
            listOf(RouteCalloutUiState(mockCallout1, "layer-1")),
            currentStates[0].callouts,
        )

        // Second update - both routes attached
        assertEquals(
            listOf(
                RouteCalloutUiState(mockCallout1, "layer-1"),
                RouteCalloutUiState(mockCallout2, "layer-2"),
            ),
            currentStates[1].callouts,
        )

        // Third update - no routes attached
        assertEquals(
            emptyList<RouteCalloutUiState>(),
            currentStates[2].callouts,
        )
    }

    @Test
    fun `ui state updates correctly when callouts data changes`() = runTest {
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

        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
            mockRouteCalloutsApi,
        )

        every {
            mockRoutesSetToRouteLineDataProvider.registerRoutesSetToRouteLineObserver(
                capture(routesSetObserverSlot),
            )
        } answers {
            routesSetObserverSlot.captured.onSet(listOf(mockRoute1), emptyList())
            routesSetObserverSlot.captured.onSet(
                listOf(mockRoute1, mockRoute2),
                emptyList(),
            )
        }
        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(mapOf("route-1" to "layer-1"))
            routesAttachedObserverSlot.captured.onAttached(
                mapOf(
                    "route-1" to "layer-1",
                    "route-2" to "layer-2",
                ),
            )
        }

        val currentStates = provider.uiStateData.take(2).toList()
        assertEquals(
            listOf(
                RouteCalloutUiState(mockCallout1, "layer-1"),
            ),
            currentStates[0].callouts,
        )

        assertEquals(
            listOf(
                RouteCalloutUiState(mockCallout1, "layer-1"),
                RouteCalloutUiState(mockCallout2, "layer-2"),
            ),
            currentStates[1].callouts,
        )
    }

    @Test
    fun `ui state is not set when no callouts data is set`() = runTest {
        val provider = RouteCalloutUiStateProvider(
            mockRoutesSetToRouteLineDataProvider,
            mockRoutesAttachedToLayersDataProvider,
        )

        every {
            mockRoutesAttachedToLayersDataProvider.registerRoutesAttachedToLayersObserver(
                capture(routesAttachedObserverSlot),
            )
        } answers {
            routesAttachedObserverSlot.captured.onAttached(mapOf("route-1" to "layer-1"))
        }

        val currentState = withTimeoutOrNull(500) {
            provider.uiStateData.first()
        }
        assertNull(currentState)
    }
}
