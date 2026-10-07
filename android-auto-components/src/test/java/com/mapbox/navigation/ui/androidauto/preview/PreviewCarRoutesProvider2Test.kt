package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.common.dispatchers.SdkDispatchersTestRule
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class PreviewCarRoutesProvider2Test {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val sdkDispatchersRule = SdkDispatchersTestRule(dispatcher)

    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private lateinit var sut: PreviewCarRoutesProvider2

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        sut = PreviewCarRoutesProvider2()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `all collectors share one navigation observer`() {
        val observers = mutableListOf<MapboxNavigationObserver>()
        val unregistered = mutableListOf<MapboxNavigationObserver>()
        every { MapboxNavigationApp.registerObserver(capture(observers)) } returns
            MapboxNavigationApp
        every { MapboxNavigationApp.unregisterObserver(capture(unregistered)) } returns
            MapboxNavigationApp
        val collectors = TestScope(dispatcher)

        sut.routesPreview.launchIn(collectors)
        repeat(3) { sut.navigationRoutes.launchIn(collectors) }

        assertEquals(1, observers.size)
        collectors.cancel()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(observers, unregistered)
    }

    @Test
    fun `a late collector receives the current routes preview`() {
        val observer = slot<MapboxNavigationObserver>()
        every { MapboxNavigationApp.registerObserver(capture(observer)) } returns
            MapboxNavigationApp
        every { MapboxNavigationApp.unregisterObserver(any()) } returns MapboxNavigationApp
        val routesObserver = slot<RoutesPreviewObserver>()
        every { mapboxNavigation.registerRoutesPreviewObserver(capture(routesObserver)) } answers {}
        val routesPreview = mockk<RoutesPreview>()
        val collectors = TestScope(dispatcher)
        sut.routesPreview.launchIn(collectors)
        observer.captured.onAttached(mapboxNavigation)
        routesObserver.captured.routesPreviewUpdated(
            mockk<RoutesPreviewUpdate> { every { this@mockk.routesPreview } returns routesPreview },
        )

        val received = mutableListOf<RoutesPreview?>()
        sut.routesPreview.onEach { received.add(it) }.launchIn(collectors)

        assertEquals(listOf(routesPreview), received)
        collectors.cancel()
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
