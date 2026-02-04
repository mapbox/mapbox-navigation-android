package com.mapbox.navigation.ui.maps.internal.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteArrowComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context = mockk<Context> {
        every { resources } returns mockk {
            every { configuration } returns Configuration()
        }
        every { createConfigurationContext(any()) } returns mockk()
    }
    private val routeArrowOptions by lazy { RouteArrowOptions.Builder(context).build() }

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        mockkObject(RouteArrowUtils)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicHeight } returns 24
            every { intrinsicWidth } returns 24
        }
        every { RouteArrowUtils.removeLayersAndSources(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(RouteArrowUtils)
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun `route progress test`() = coroutineRule.runBlockingTest {
        val expected = ExpectedFactory.createError<InvalidPointError, UpdateManeuverArrowValue>(
            InvalidPointError("", null),
        )
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(any()) } just Runs
            every { registerRouteProgressObserver(capture(routeProgressObserverSlot)) } just Runs
        }
        val mockApi = mockk<MapboxRouteArrowApi> {
            every { addUpcomingManeuverArrow(any()) } returns expected
        }
        val mockStyle = mockk<Style>()
        val mockView = mockk<MapboxRouteArrowView>(relaxed = true)
        val sut = RouteArrowComponent(
            mockMapWithStyleLoaded(mockStyle),
            routeArrowOptions,
            mockApi,
            mockView,
        )

        sut.onAttached(mockMapboxNavigation)
        routeProgressObserverSlot.captured.onRouteProgressChanged(mockk())

        verify { mockView.renderManeuverUpdate(mockStyle, expected) }
    }

    @Test
    fun `should clear route arrows when routes are cleared`() = coroutineRule.runBlockingTest {
        val routesObserverSlot = slot<RoutesObserver>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { registerRoutesObserver(capture(routesObserverSlot)) } just Runs
            every { registerRouteProgressObserver(any()) } just Runs
        }
        val mockStyle = mockk<Style>()
        val clearValue = mockk<ClearArrowsValue>()
        val mockApi = mockk<MapboxRouteArrowApi> {
            every { clearArrows() } returns clearValue
        }
        val mockView = mockk<MapboxRouteArrowView>(relaxed = true)
        val sut = RouteArrowComponent(
            mockMapWithStyleLoaded(mockStyle),
            routeArrowOptions,
            mockApi,
            mockView,
        )

        sut.onAttached(mockMapboxNavigation)
        routesObserverSlot.captured.onRoutesChanged(
            mockk {
                every { navigationRoutes } returns emptyList()
            },
        )

        verify { mockView.render(mockStyle, clearValue) }
    }

    @Test
    fun `onDetached should clear all route arrows`() = coroutineRule.runBlockingTest {
        val mockStyle = mockk<Style>()
        val clearValue = mockk<ClearArrowsValue>()
        val mockApi = mockk<MapboxRouteArrowApi> {
            every { clearArrows() } returns clearValue
        }
        val mockView = mockk<MapboxRouteArrowView>(relaxed = true)
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val sut = RouteArrowComponent(
            mockMapWithStyleLoaded(mockStyle),
            routeArrowOptions,
            mockApi,
            mockView,
        )

        sut.onAttached(mockMapboxNavigation)
        sut.onDetached(mockMapboxNavigation)

        verify { mockView.render(mockStyle, clearValue) }
    }

    private fun mockMapWithStyleLoaded(style: Style): MapboxMap = mockk {
        every { getStyle() } returns style
    }
}
