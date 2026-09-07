package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
}
