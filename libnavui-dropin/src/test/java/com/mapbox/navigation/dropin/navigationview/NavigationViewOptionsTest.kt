package com.mapbox.navigation.dropin.navigationview

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.dropin.ViewOptionsCustomization
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalPreviewMapboxNavigationAPI
internal class NavigationViewOptionsTest {

    lateinit var ctx: Context
    lateinit var sut: NavigationViewOptions

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        sut = NavigationViewOptions(ctx)
    }

    @Test
    fun `applyCustomization should update NON NULL values`() {
        val c = customization()

        sut.applyCustomization(c)

        assertEquals(c.mapStyleUriDay, sut.mapStyleUriDay.value)
        assertEquals(c.mapStyleUriNight, sut.mapStyleUriNight.value)
        assertEquals(c.routeLineOptions, sut.routeLineOptions.value)
        assertEquals(c.routeArrowOptions, sut.routeArrowOptions.value)
        assertEquals(c.showInfoPanelInFreeDrive, sut.showInfoPanelInFreeDrive.value)
        assertEquals(c.enableMapLongClickIntercept, sut.enableMapLongClickIntercept.value)
        assertEquals(c.isInfoPanelHideable, sut.isInfoPanelHideable.value)
        assertEquals(c.infoPanelForcedState, sut.infoPanelForcedState.value)
        assertEquals(c.distanceFormatterOptions, sut.distanceFormatterOptions.value)
        assertEquals(c.showManeuver, sut.showManeuver.value)
        assertEquals(c.showSpeedLimit, sut.showSpeedLimit.value)
        assertEquals(c.showRoadName, sut.showRoadName.value)
        assertEquals(c.showActionButtons, sut.showActionButtons.value)
        assertEquals(c.showCompassActionButton, sut.showCompassActionButton.value)
        assertEquals(c.showCameraModeActionButton, sut.showCameraModeActionButton.value)
        assertEquals(c.showToggleAudioActionButton, sut.showToggleAudioActionButton.value)
        assertEquals(c.showRecenterActionButton, sut.showRecenterActionButton.value)
        assertEquals(c.showMapScalebar, sut.showMapScalebar.value)
        assertEquals(c.showTripProgress, sut.showTripProgress.value)
        assertEquals(c.showRoutePreviewButton, sut.showRoutePreviewButton.value)
        assertEquals(c.showStartNavigationButton, sut.showStartNavigationButton.value)
        assertEquals(c.showEndNavigationButton, sut.showEndNavigationButton.value)
    }

    private fun customization() =
        ViewOptionsCustomization().apply {
            mapStyleUriDay = Style.TRAFFIC_DAY
            mapStyleUriNight = Style.SATELLITE
            routeLineOptions = MapboxRouteLineOptions.Builder(ctx)
                .withRouteLineResources(RouteLineResources.Builder().build())
                .withRouteLineBelowLayerId("road-label-navigation")
                .withVanishingRouteLineEnabled(true)
                .displaySoftGradientForTraffic(true)
                .build()
            routeArrowOptions = RouteArrowOptions.Builder(ctx)
                .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                .withArrowColor(Color.YELLOW)
                .build()
            showInfoPanelInFreeDrive = true
            enableMapLongClickIntercept = false
            isInfoPanelHideable = true
            infoPanelForcedState = BottomSheetBehavior.STATE_EXPANDED
            distanceFormatterOptions = DistanceFormatterOptions
                .Builder(ctx)
                .unitType(UnitType.METRIC)
                .build()
            showManeuver = false
            showSpeedLimit = false
            showRoadName = false
            showActionButtons = false
            showCompassActionButton = true
            showCameraModeActionButton = false
            showToggleAudioActionButton = false
            showRecenterActionButton = false
            showMapScalebar = true
            showTripProgress = false
            showRoutePreviewButton = false
            showStartNavigationButton = false
            showEndNavigationButton = false
        }
}
