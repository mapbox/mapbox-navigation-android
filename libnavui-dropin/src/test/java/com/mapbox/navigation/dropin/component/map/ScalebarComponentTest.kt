package com.mapbox.navigation.dropin.component.map

import android.content.Context
import android.view.Gravity
import androidx.core.graphics.Insets
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.scalebar.ScaleBarPlugin
import com.mapbox.maps.plugin.scalebar.generated.ScaleBarSettings
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.MapboxMapScalebarParams
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ScalebarComponentTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultMarginTop = 4f
    private val defaultMarginLeft = 4f
    private val settings = mockk<ScaleBarSettings>(relaxed = true) {
        every { marginTop } returns 14f
        every { marginLeft } returns 14f
    }
    private val scalebarMock = mockk<ScaleBarPlugin>(relaxed = true) {
        every { marginTop } returns 14f
        every { marginLeft } returns 14f
        every { updateSettings(any()) } answers {
            (args[0] as ScaleBarSettings.() -> Unit)(settings)
        }
    }
    private val mapView = mockk<MapView>(relaxed = true) {
        every { scalebar } returns scalebarMock
    }
    private val scalebarParamsFlow = MutableStateFlow(
        MapboxMapScalebarParams.Builder(context)
            .isMetricsUnits(false)
            .build()
    )
    private val initialInsetTop = 3
    private val initialInsetLeft = 5
    private val systemBarsFlow = MutableStateFlow(
        Insets.of(initialInsetLeft, initialInsetTop, 0, 0)
    )
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val component = ScalebarComponent(mapView, scalebarParamsFlow, systemBarsFlow)

    @Test
    fun enableFlagIsSetOnInit() {
        clearMocks(settings)
        val enabled = Random.nextBoolean()
        ScalebarComponent(
            mapView,
            MutableStateFlow(MapboxMapScalebarParams.Builder(context).enabled(enabled).build()),
            MutableStateFlow(Insets.NONE)
        )
        verify { settings.enabled = enabled }
    }

    @Test
    fun isMetricsUnitIsSetOnInit() {
        clearMocks(settings)
        val isMetricUnits = Random.nextBoolean()
        ScalebarComponent(
            mapView,
            MutableStateFlow(
                MapboxMapScalebarParams.Builder(context).isMetricsUnits(isMetricUnits).build()
            ),
            MutableStateFlow(Insets.NONE)
        )
        verify { settings.isMetricUnits = isMetricUnits }
    }

    @Test
    fun textBorderWidthIsSetOnInit() {
        clearMocks(settings)
        ScalebarComponent(
            mapView,
            MutableStateFlow(MapboxMapScalebarParams.Builder(context).build()),
            MutableStateFlow(Insets.NONE)
        )
        verify { settings.textBorderWidth = 3f }
    }

    @Test
    fun positionIsSetOnInit() {
        verify { settings.position = Gravity.TOP or Gravity.START }
    }

    @Test
    fun scalebarParamsChangedBeforeOnAttached() {
        clearMocks(settings)
        scalebarParamsFlow.tryEmit(
            MapboxMapScalebarParams.Builder(context)
                .enabled(true)
                .isMetricsUnits(false)
                .build()
        )
        verify(exactly = 0) { settings.enabled = any() }
        verify(exactly = 0) { settings.isMetricUnits = any() }
        verify(exactly = 0) { settings.marginTop = any() }
        verify(exactly = 0) { settings.marginLeft = any() }
    }

    @Test
    fun systemBarInsetsChangedBeforeOnAttached() {
        clearMocks(settings)
        systemBarsFlow.tryEmit(Insets.of(12, 34, 56, 78))
        verify(exactly = 0) { settings.enabled = any() }
        verify(exactly = 0) { settings.isMetricUnits = any() }
        verify(exactly = 0) { settings.marginTop = any() }
        verify(exactly = 0) { settings.marginLeft = any() }
    }

    @Test
    fun enabledFlagSetOnAttached() {
        clearMocks(settings)
        component.onAttached(mapboxNavigation)
        verify(exactly = 1) { settings.enabled = false }
    }

    @Test
    fun isMetricUnitsSetOnAttached() {
        clearMocks(settings)
        component.onAttached(mapboxNavigation)
        verify(exactly = 1) { settings.isMetricUnits = false }
    }

    @Test
    fun marginTopSetOnOnAttached() {
        clearMocks(settings)
        component.onAttached(mapboxNavigation)
        verify(exactly = 1) { settings.marginTop = defaultMarginTop + initialInsetTop }
    }

    @Test
    fun marginLeftSetOnOnAttached() {
        clearMocks(settings)
        component.onAttached(mapboxNavigation)
        verify(exactly = 1) { settings.marginLeft = defaultMarginLeft + initialInsetLeft }
    }

    @Test
    fun scalebarParamsChangedAfterOnAttached() {
        component.onAttached(mapboxNavigation)
        clearMocks(settings)
        scalebarParamsFlow.tryEmit(
            MapboxMapScalebarParams.Builder(context)
                .enabled(true)
                .isMetricsUnits(true)
                .build()
        )
        verify(exactly = 1) { settings.enabled = true }
        verify(exactly = 1) { settings.isMetricUnits = true }
        verify(exactly = 0) { settings.marginTop = any() }
        verify(exactly = 0) { settings.marginLeft = any() }
    }

    @Test
    fun systemBarInsetsChangedAfterOnAttached() {
        component.onAttached(mapboxNavigation)
        clearMocks(settings)
        systemBarsFlow.tryEmit(Insets.of(12, 34, 56, 78))
        verify(exactly = 0) { settings.enabled = any() }
        verify(exactly = 0) { settings.isMetricUnits = any() }
        verify(exactly = 1) { settings.marginTop = defaultMarginTop + 34 }
        verify(exactly = 1) { settings.marginLeft = defaultMarginLeft + 12 }
    }

    @Test
    fun scalebarParamsChangedAfterOnDetached() {
        component.onAttached(mapboxNavigation)
        clearMocks(settings)
        component.onDetached(mapboxNavigation)
        scalebarParamsFlow.tryEmit(
            MapboxMapScalebarParams.Builder(context)
                .enabled(true)
                .isMetricsUnits(true)
                .build()
        )
        verify(exactly = 0) { settings.enabled = any() }
        verify(exactly = 0) { settings.isMetricUnits = any() }
        verify(exactly = 0) { settings.marginTop = any() }
        verify(exactly = 0) { settings.marginLeft = any() }
    }

    @Test
    fun systemBarInsetsChangedAfterOnDetached() {
        component.onAttached(mapboxNavigation)
        clearMocks(settings)
        component.onDetached(mapboxNavigation)
        systemBarsFlow.tryEmit(Insets.of(12, 34, 56, 78))
        verify(exactly = 0) { settings.enabled = any() }
        verify(exactly = 0) { settings.isMetricUnits = any() }
        verify(exactly = 0) { settings.marginTop = any() }
        verify(exactly = 0) { settings.marginLeft = any() }
    }
}
