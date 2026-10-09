package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.content.res.Configuration
import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.renderer.widget.WidgetPosition
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class, ExperimentalCoroutinesApi::class)
class CarSpeedLimitRendererTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val speedLimitWidget: SpeedLimitWidget = mockk()
    private val services: CarSpeedLimitServices = mockk {
        every { speedLimitWidget(any(), any()) } returns speedLimitWidget
    }
    private val sutOptions = MutableStateFlow(SpeedLimitOptions.Builder().build())
    private val sut = CarSpeedLimitRenderer(services, sutOptions)

    @Test
    fun `verify speed limit widget is created`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        assertNotNull(sut.speedLimitWidget)
        verify { services.speedLimitWidget(any(), any()) }
    }

    @Test
    fun `speed limit widget is scaled to the head unit density`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true) {
            every { carContext.resources.configuration } returns Configuration().apply {
                densityDpi = 320
            }
        }

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        verify { services.speedLimitWidget(any(), 2f) }
    }

    @Test
    fun `verify speed limit widget is null map is detached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        sut.onDetached(mapboxCarMapSurface)

        assertNull(sut.speedLimitWidget)
    }

    @Test
    fun `widget offsets use the scaled margins`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)
        val position = slot<WidgetPosition>()
        every { speedLimitWidget.update(any(), any()) } just Runs
        every { speedLimitWidget.marginXPx } returns 28f
        every { speedLimitWidget.marginYPx } returns 60f
        every { speedLimitWidget.getPosition() } returns WidgetPosition {}
        every { speedLimitWidget.setPosition(capture(position)) } just Runs

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        sut.onVisibleAreaChanged(Rect(), EdgeInsets(0.0, 0.0, 10.0, 5.0))

        assertEquals(-33f, position.captured.offsetX)
        assertEquals(-70f, position.captured.offsetY)
    }
}
