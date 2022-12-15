package com.mapbox.androidauto.navigation.speedlimit

import android.graphics.Rect
import com.mapbox.androidauto.MapboxCarOptions
import com.mapbox.androidauto.internal.RendererUtils
import com.mapbox.androidauto.testing.CarAppTestRule
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class)
class CarSpeedLimitRendererTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val speedLimitWidget: SpeedLimitWidget = mockk()
    private val services: CarSpeedLimitServices = mockk {
        every { speedLimitWidget(any()) } returns speedLimitWidget
    }
    private val sutOptions = MutableStateFlow(SpeedLimitOptions.Builder().build())
    private val options: MapboxCarOptions = mockk {
        every { speedLimitOptions } returns sutOptions
    }
    private val sut = CarSpeedLimitRenderer(services, options)

    @Test
    fun `verify speed limit widget is created`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)

        assertNotNull(sut.speedLimitWidget)
        verify { services.speedLimitWidget(any()) }
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
    fun `verify speed limit is hidden when map pan buttons are shown`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        sut.onVisibleAreaChanged(mockRectMapPanShown(), mockEdgeInsetsMapPanShown())

        assertNull(sut.speedLimitWidget)
    }

    @Test
    fun `verify speed limit is shown after map pan buttons are hidden`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk(relaxed = true)

        carAppTestRule.onAttached(mapboxNavigation)
        sut.onAttached(mapboxCarMapSurface)
        sut.onVisibleAreaChanged(mockRectMapPanShown(), mockEdgeInsetsMapPanShown())
        sut.onVisibleAreaChanged(mockRectMapPanHidden(), mockEdgeInsetsMapPanHidden())

        verifyOrder {
            speedLimitWidget.updateBitmap(RendererUtils.EMPTY_BITMAP)
        }
        assertNull(sut.speedLimitWidget)
    }

    private fun mockRectMapPanShown(): Rect = Rect(0, 100, 720, 400)

    private fun mockRectMapPanHidden(): Rect = Rect(0, 32, 800, 400)

    private fun mockEdgeInsetsMapPanShown(): EdgeInsets = mockk {
        every { left } returns 0.0
        every { top } returns 100.0
        every { right } returns 0.0
        every { bottom } returns 80.0
    }

    private fun mockEdgeInsetsMapPanHidden(): EdgeInsets = mockk {
        every { left } returns 0.0
        every { top } returns 100.0
        every { right } returns 0.0
        every { bottom } returns 0.0
    }
}
