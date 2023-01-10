package com.mapbox.navigation.dropin.navigationview

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.ViewStyleCustomization
import com.mapbox.navigation.ui.maneuver.model.ManeuverViewOptions
import com.mapbox.navigation.ui.maps.puck.LocationPuckOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationViewStylesTest {

    private lateinit var ctx: Context
    private lateinit var sut: NavigationViewStyles

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = NavigationViewStyles(ctx)
    }

    @Test
    fun `applyCustomization should update NON NULL values`() {
        val c = customization()

        sut.applyCustomization(c)

        assertEquals(c.infoPanelPeekHeight, sut.infoPanelPeekHeight.value)
        assertEquals(c.infoPanelMarginStart, sut.infoPanelMarginStart.value)
        assertEquals(c.infoPanelMarginEnd, sut.infoPanelMarginEnd.value)
        assertEquals(c.infoPanelBackground, sut.infoPanelBackground.value)
        assertEquals(c.infoPanelGuidelineMaxPosPercent, sut.infoPanelGuidelineMaxPosPercent.value)
        assertEquals(c.poiNameTextAppearance, sut.poiNameTextAppearance.value)
        assertEquals(c.tripProgressStyle, sut.tripProgressStyle.value)
        assertEquals(c.compassButtonStyle, sut.compassButtonStyle.value)
        assertEquals(c.recenterButtonStyle, sut.recenterButtonStyle.value)
        assertEquals(c.audioGuidanceButtonStyle, sut.audioGuidanceButtonStyle.value)
        assertEquals(c.cameraModeButtonStyle, sut.cameraModeButtonStyle.value)
        assertEquals(c.routePreviewButtonStyle, sut.routePreviewButtonStyle.value)
        assertEquals(c.endNavigationButtonStyle, sut.endNavigationButtonStyle.value)
        assertEquals(c.startNavigationButtonStyle, sut.startNavigationButtonStyle.value)
        assertEquals(c.speedLimitStyle, sut.speedLimitStyle.value)
        assertEquals(c.maneuverViewOptions, sut.maneuverViewOptions.value)
        assertEquals(c.speedLimitTextAppearance, sut.speedLimitTextAppearance.value)
        assertEquals(
            c.destinationMarkerAnnotationOptions,
            sut.destinationMarkerAnnotationOptions.value
        )
        assertEquals(c.roadNameBackground, sut.roadNameBackground.value)
        assertEquals(c.roadNameTextAppearance, sut.roadNameTextAppearance.value)
        assertEquals(c.arrivalTextAppearance, sut.arrivalTextAppearance.value)
        assertEquals(c.locationPuckOptions, sut.locationPuckOptions.value)
    }

    @Test
    fun `applyCustomization should clamp infoPanelGuidelineMaxPosPercent value`() {
        sut.applyCustomization(
            ViewStyleCustomization().apply {
                infoPanelGuidelineMaxPosPercent = -0.1f
            }
        )
        assertEquals(0.0f, sut.infoPanelGuidelineMaxPosPercent.value)

        sut.applyCustomization(
            ViewStyleCustomization().apply {
                infoPanelGuidelineMaxPosPercent = 1.1f
            }
        )
        assertEquals(1.0f, sut.infoPanelGuidelineMaxPosPercent.value)
    }

    private fun customization() = ViewStyleCustomization().apply {
        infoPanelPeekHeight = 1
        infoPanelMarginStart = 2
        infoPanelMarginEnd = 3
        infoPanelBackground = android.R.drawable.spinner_background
        infoPanelGuidelineMaxPosPercent = 0.25f
        poiNameTextAppearance = android.R.style.TextAppearance_DeviceDefault_Large
        tripProgressStyle = R.style.MapboxStyleTripProgressView
        compassButtonStyle = R.style.MapboxStyleExtendableButton_Circle
        recenterButtonStyle = R.style.MapboxStyleExtendableButton_Circle
        audioGuidanceButtonStyle = R.style.MapboxStyleAudioGuidanceButton_Circle
        cameraModeButtonStyle = R.style.MapboxStyleCameraModeButton_Circle
        routePreviewButtonStyle = R.style.MapboxStyleExtendableButton_Circle
        endNavigationButtonStyle = R.style.MapboxStyleExtendableButton_Circle
        startNavigationButtonStyle = R.style.MapboxStyleExtendableButton_Circle
        speedLimitStyle = R.style.MapboxStyleSpeedLimit
        maneuverViewOptions = ManeuverViewOptions.Builder().build()
        speedLimitTextAppearance = android.R.style.TextAppearance_DeviceDefault_Large
        destinationMarkerAnnotationOptions = PointAnnotationOptions()
        roadNameBackground = android.R.drawable.spinner_background
        roadNameTextAppearance = android.R.style.TextAppearance_DeviceDefault_Large
        arrivalTextAppearance = android.R.style.TextAppearance_DeviceDefault_Large
        locationPuckOptions = LocationPuckOptions
            .Builder(ctx)
            .freeDrivePuck(
                LocationPuck2D(
                    bearingImage = ContextCompat.getDrawable(
                        ctx,
                        android.R.drawable.ic_media_play
                    )
                )
            )
            .build()
    }
}
