package com.mapbox.navigation.dropin.map.logo

import androidx.core.graphics.Insets
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.attribution.AttributionPlugin
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.attribution.generated.AttributionSettings
import com.mapbox.maps.plugin.logo.LogoPlugin
import com.mapbox.maps.plugin.logo.generated.LogoSettings
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class LogoAttributionComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: LogoAttributionComponent

    private lateinit var systemBarsInsets: MutableStateFlow<Insets?>
    private lateinit var mockLogoPlugin: LogoPlugin
    private lateinit var mockAttributionPlugin: AttributionPlugin

    @Before
    fun setUp() {
        mockLogoPlugin = mockk(relaxed = true)
        mockAttributionPlugin = mockk(relaxed = true)
        val mockMapView = mockk<MapView> {
            every { logo } returns mockLogoPlugin
            every { attribution } returns mockAttributionPlugin
        }
        systemBarsInsets = MutableStateFlow(null)
        sut = LogoAttributionComponent(mockMapView, systemBarsInsets)
    }

    @Test
    fun `onAttached - should update map logo and attribution margins`() {
        val logoSettings = LogoSettings()
        val attributionSettings = AttributionSettings()
        val initialAttributionSettings = AttributionSettings().apply {
            marginLeft = 123f
        }
        every { mockAttributionPlugin.getSettings() } returns initialAttributionSettings
        captureUpdatedSettings(
            logoSettings = logoSettings,
            attributionSettings = attributionSettings
        )

        val bottomInset = 123.0f
        val leftInset = 10.0f
        val rightInset = 20.0f
        systemBarsInsets.value =
            Insets.of(leftInset.toInt(), 0, rightInset.toInt(), bottomInset.toInt())

        sut.onAttached(mockk())

        assertEquals(bottomInset, logoSettings.marginBottom)
        assertEquals(leftInset, logoSettings.marginLeft)
        assertEquals(rightInset, logoSettings.marginRight)
        assertEquals(bottomInset, attributionSettings.marginBottom)
        assertEquals(
            initialAttributionSettings.marginLeft + leftInset,
            attributionSettings.marginLeft
        )
        assertEquals(rightInset, attributionSettings.marginRight)
    }

    private fun captureUpdatedSettings(
        logoSettings: LogoSettings,
        attributionSettings: AttributionSettings
    ) {
        val logoLambdaCapture = slot<LogoSettings.() -> Unit>()
        val attributionLambdaCapture = slot<AttributionSettings.() -> Unit>()
        every { mockLogoPlugin.updateSettings(capture(logoLambdaCapture)) } answers {
            logoSettings.apply(logoLambdaCapture.captured)
        }
        every { mockAttributionPlugin.updateSettings(capture(attributionLambdaCapture)) } answers {
            attributionSettings.apply(attributionLambdaCapture.captured)
        }
    }
}
