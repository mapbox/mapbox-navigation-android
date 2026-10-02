package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PreviewCarRoutesProvider2Test {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val sut = PreviewCarRoutesProvider2()

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `selected route becomes the primary previewed route`() {
        val firstRoute = mockk<NavigationRoute>()
        val secondRoute = mockk<NavigationRoute>()
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(firstRoute, secondRoute)
        }

        sut.updateSelectedRoute(1)

        verify { mapboxNavigation.changeRoutesPreviewPrimaryRoute(secondRoute) }
    }

    @Test
    fun `selection outside the current preview is ignored`() {
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(mockk())
        }

        sut.updateSelectedRoute(1)

        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }

    @Test
    fun `selection without a preview is ignored`() {
        every { mapboxNavigation.getRoutesPreview() } returns null

        sut.updateSelectedRoute(0)

        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }

    @Test
    fun `selection with detached navigation is ignored`() {
        every { MapboxNavigationApp.current() } returns null

        sut.updateSelectedRoute(0)

        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }

    @Test
    fun `route selected by id becomes the primary previewed route`() {
        val firstRoute = mockk<NavigationRoute> { every { id } returns "first" }
        val secondRoute = mockk<NavigationRoute> { every { id } returns "second" }
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(firstRoute, secondRoute)
        }

        val selected = sut.selectRoute("second")

        assertTrue(selected)
        verify { mapboxNavigation.changeRoutesPreviewPrimaryRoute(secondRoute) }
    }

    @Test
    fun `route selected by id that is no longer previewed is ignored`() {
        every { mapboxNavigation.getRoutesPreview() } returns mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                mockk<NavigationRoute> { every { id } returns "current" },
            )
        }

        val selected = sut.selectRoute("stale")

        assertFalse(selected)
        verify(exactly = 0) { mapboxNavigation.changeRoutesPreviewPrimaryRoute(any()) }
    }
}
