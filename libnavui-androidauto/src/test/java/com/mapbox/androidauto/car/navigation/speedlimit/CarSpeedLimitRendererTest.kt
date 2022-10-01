package com.mapbox.androidauto.car.navigation.speedlimit

import com.mapbox.androidauto.car.MapboxCarContext
import com.mapbox.androidauto.testing.CarAppTestRule
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class)
class CarSpeedLimitRendererTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val speedLimitWidget: SpeedLimitWidget = mockk()
    private val services: CarSpeedLimitServices = mockk {
        every { speedLimitWidget(any()) } returns speedLimitWidget
    }
    private val options = MutableStateFlow(SpeedLimitOptions.Builder().build())
    private val ctx: MapboxCarContext = mockk {
        every { speedLimitOptions } returns options
    }
    private val sut = CarSpeedLimitRenderer(services, ctx)

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
}
