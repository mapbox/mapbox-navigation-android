@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapboxNavigationRoutesProviderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

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
        val firstRoute = route("first")
        val secondRoute = route("second")
        givenPreview(listOf(firstRoute, secondRoute))
        sut.onAttached(mapboxNavigation)

        sut.selectRoute("second")

        verify { mapboxNavigation.changeRoutesPreviewPrimaryRoute(secondRoute) }
    }

    @Test
    fun `select route ignores a route that is no longer previewed`() {
        givenPreview(listOf(route("current")))
        sut.onAttached(mapboxNavigation)

        sut.selectRoute("stale")

        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }

    @Test
    fun `start navigation on the primary route keeps the preview order`() {
        val primaryRoute = route("primary")
        val alternativeRoute = route("alternative")
        givenPreview(listOf(primaryRoute, alternativeRoute))
        sut.onAttached(mapboxNavigation)

        sut.startNavigation("primary")

        verify {
            mapboxNavigation.setNavigationRoutes(listOf(primaryRoute, alternativeRoute))
            mapboxNavigation.setRoutesPreview(emptyList())
        }
    }

    @Test
    fun `start navigation on an alternative makes it the primary route`() {
        val firstRoute = route("first")
        val secondRoute = route("second")
        val thirdRoute = route("third")
        givenPreview(listOf(firstRoute, secondRoute, thirdRoute))
        sut.onAttached(mapboxNavigation)

        sut.startNavigation("third")

        verifyOrder {
            mapboxNavigation.setNavigationRoutes(listOf(thirdRoute, firstRoute, secondRoute))
            mapboxNavigation.setRoutesPreview(emptyList())
        }
    }

    @Test
    fun `start navigation keeps the original order of the other routes`() {
        // The preview's primary is b, so routesList is [b, a, c]. Starting c must keep the
        // original order of the alternatives: [c, a, b].
        val a = route("a")
        val b = route("b")
        val c = route("c")
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { routesList } returns listOf(b, a, c)
            every { originalRoutesList } returns listOf(a, b, c)
            every { primaryRouteIndex } returns 1
        }
        sut.onAttached(mapboxNavigation)

        sut.startNavigation("c")

        verifyOrder {
            mapboxNavigation.setNavigationRoutes(listOf(c, a, b))
            mapboxNavigation.setRoutesPreview(emptyList())
        }
    }

    @Test
    fun `select route with detached navigation does nothing`() {
        every { MapboxNavigationApp.current() } returns null

        sut.selectRoute("any")

        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }

    @Test
    fun `start navigation from an outdated template does not start a newer route`() {
        // The template was built for routes [a, b]; before the click arrives the preview is
        // replaced with routes [x, y].
        givenPreview(listOf(route("x"), route("y")))
        sut.onAttached(mapboxNavigation)

        sut.startNavigation("b")

        verifyNoRouteChanges()
    }

    @Test
    fun `start navigation without a preview does nothing`() {
        sut.onAttached(mapboxNavigation)

        sut.startNavigation("any")

        verifyNoRouteChanges()
    }

    @Test
    fun `start navigation with detached navigation does nothing`() {
        every { MapboxNavigationApp.current() } returns null

        sut.startNavigation("any")

        verifyNoRouteChanges()
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

    private fun route(routeId: String) = mockk<NavigationRoute> {
        every { id } returns routeId
    }

    private fun givenPreview(routes: List<NavigationRoute>) {
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { routesList } returns routes
            every { originalRoutesList } returns routes
            every { primaryRouteIndex } returns 0
        }
    }

    private fun verifyNoRouteChanges() {
        verify(exactly = 0) { mapboxNavigation.moveRoutesFromPreviewToNavigator() }
        verify(exactly = 0) { mapboxNavigation.setNavigationRoutes(any()) }
        verify(exactly = 0) { mapboxNavigation.setRoutesPreview(any()) }
    }
}
