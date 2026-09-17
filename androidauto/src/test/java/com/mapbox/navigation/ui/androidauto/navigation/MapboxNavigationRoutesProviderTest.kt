@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapboxNavigationRoutesProviderTest {

    private val routesObserver = slot<RoutesObserver>()
    private val routesPreviewObserver = slot<RoutesPreviewObserver>()
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
        every { getNavigationRoutes() } returns emptyList()
        every { getRoutesPreview() } returns null
        every { registerRoutesObserver(capture(routesObserver)) } just Runs
        every { registerRoutesPreviewObserver(capture(routesPreviewObserver)) } just Runs
    }
    private val sut = MapboxNavigationRoutesProvider()

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `state follows free drive preview and active guidance`() {
        val previewRoute = mockk<NavigationRoute>()
        val activeRoute = mockk<NavigationRoute>()
        val preview = mockk<RoutesPreview> {
            every { routesList } returns listOf(previewRoute)
            every { originalRoutesList } returns listOf(previewRoute)
            every { primaryRouteIndex } returns 0
        }
        sut.onAttached(mapboxNavigation)

        assertEquals(MapboxNavigationScreenState.FREE_DRIVE, sut.state.value.screenState)

        routesPreviewObserver.captured.routesPreviewUpdated(
            mockk<RoutesPreviewUpdate> { every { routesPreview } returns preview },
        )
        assertEquals(MapboxNavigationScreenState.ROUTE_PREVIEW, sut.state.value.screenState)
        assertEquals(listOf(previewRoute), sut.state.value.routes)

        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns listOf(activeRoute) },
        )
        assertEquals(MapboxNavigationScreenState.ACTIVE_GUIDANCE, sut.state.value.screenState)
        assertEquals(listOf(activeRoute), sut.state.value.routes)

        routesObserver.captured.onRoutesChanged(
            mockk { every { navigationRoutes } returns emptyList() },
        )
        assertEquals(MapboxNavigationScreenState.ROUTE_PREVIEW, sut.state.value.screenState)

        routesPreviewObserver.captured.routesPreviewUpdated(
            mockk<RoutesPreviewUpdate> { every { routesPreview } returns null },
        )
        assertEquals(MapboxNavigationScreenState.FREE_DRIVE, sut.state.value.screenState)
    }

    @Test
    fun `select route changes preview primary route`() {
        val firstRoute = mockk<NavigationRoute>()
        val secondRoute = mockk<NavigationRoute>()
        val preview = mockk<RoutesPreview> {
            every { routesList } returns listOf(firstRoute, secondRoute)
            every { originalRoutesList } returns listOf(firstRoute, secondRoute)
            every { primaryRouteIndex } returns 0
        }
        every { mapboxNavigation.getRoutesPreview() } returns preview
        sut.onAttached(mapboxNavigation)

        sut.selectRoute(1)

        verify { mapboxNavigation.changeRoutesPreviewPrimaryRoute(secondRoute) }
    }

    @Test
    fun `start navigation promotes preview routes to navigator`() {
        val primaryRoute = mockk<NavigationRoute>()
        val alternativeRoute = mockk<NavigationRoute>()
        val preview = mockk<RoutesPreview> {
            every { routesList } returns listOf(primaryRoute, alternativeRoute)
            every { originalRoutesList } returns listOf(alternativeRoute, primaryRoute)
            every { primaryRouteIndex } returns 1
        }
        every { mapboxNavigation.getRoutesPreview() } returns preview
        sut.onAttached(mapboxNavigation)

        sut.startNavigation()

        verify { mapboxNavigation.moveRoutesFromPreviewToNavigator() }
    }

    @Test
    fun `detach clears state and unregisters observers`() {
        every { mapboxNavigation.getNavigationRoutes() } returns listOf(mockk())
        sut.onAttached(mapboxNavigation)

        sut.onDetached(mapboxNavigation)

        assertEquals(MapboxNavigationScreenState.FREE_DRIVE, sut.state.value.screenState)
        verify { mapboxNavigation.unregisterRoutesObserver(routesObserver.captured) }
        verify { mapboxNavigation.unregisterRoutesPreviewObserver(routesPreviewObserver.captured) }
    }
}
