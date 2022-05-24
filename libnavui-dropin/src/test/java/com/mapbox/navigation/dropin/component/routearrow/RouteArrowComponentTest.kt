package com.mapbox.navigation.dropin.component.routearrow

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.internal.ui.RouteArrowComponent
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteArrowComponentTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val context = mockk<Context>()
    private val routeArrowOptions by lazy { RouteArrowOptions.Builder(context).build() }

    @Before
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true) {
            every { intrinsicHeight } returns 24
            every { intrinsicWidth } returns 24
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun `route progress test`() {
        val expected = ExpectedFactory.createError<InvalidPointError, UpdateManeuverArrowValue>(
            InvalidPointError("", null)
        )
        val routeProgressObserverSlot = slot<RouteProgressObserver>()
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val mockApi = mockk<MapboxRouteArrowApi> {
            every { addUpcomingManeuverArrow(any()) } returns expected
        }
        val mockStyle = mockk<Style>()
        val mockMap = mockk<MapboxMap> {
            every { getStyle() } returns mockStyle
        }
        val mockMapView = mockk<MapView>() {
            every { getMapboxMap() } returns mockMap
        }
        val mockView = mockk<MapboxRouteArrowView>(relaxed = true)
        RouteArrowComponent(
            mockMapView,
            routeArrowOptions,
            mockApi,
            mockView
        ).onAttached(mockMapboxNavigation)
        verify {
            mockMapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        }

        routeProgressObserverSlot.captured.onRouteProgressChanged(mockk())

        verify { mockView.renderManeuverUpdate(mockStyle, expected) }
    }

    @Test
    fun `onDetached should clear all route arrows`() {
        val style = mockk<Style>()
        val mockMapView = mockk<MapView> {
            every { getMapboxMap() } returns mockk {
                every { getStyle() } returns style
            }
        }
        val clearValue = mockk<ClearArrowsValue>()
        val mockApi = mockk<MapboxRouteArrowApi> {
            every { clearArrows() } returns clearValue
        }
        val mockView = mockk<MapboxRouteArrowView>(relaxed = true)
        val mockMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)

        val sut = RouteArrowComponent(
            mockMapView,
            routeArrowOptions,
            mockApi,
            mockView
        )
        sut.onAttached(mockMapboxNavigation)
        sut.onDetached(mockMapboxNavigation)

        verify { mockView.render(style, clearValue) }
    }
}
