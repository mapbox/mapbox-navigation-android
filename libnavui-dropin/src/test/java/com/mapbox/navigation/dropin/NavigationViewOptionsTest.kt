package com.mapbox.navigation.dropin

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
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
        }
}
