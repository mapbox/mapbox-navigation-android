package com.mapbox.navigation.ui.route

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.ThemeSwitcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapboxRouteLayerProviderFactoryTest {

    lateinit var ctx: Context
    var styleRes: Int = 0

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )
    }

    @Test
    fun getLayerProvider() {
        val descriptors = listOf<RouteStyleDescriptor>()

        val result = MapboxRouteLayerProviderFactory.getLayerProvider(descriptors, ctx, styleRes)

        assertEquals(descriptors, result.routeStyleDescriptors)
    }

    @Test
    fun getLayerProviderBuildsRouteLineScaleValues() {
        val layerProvider = MapboxRouteLayerProviderFactory.getLayerProvider(
            listOf(),
            ctx,
            styleRes
        )

        assertEquals(6, layerProvider.routeLineScaleValues.size)
        assertEquals(4.0f, layerProvider.routeLineScaleValues[0].scaleStop)
        assertEquals(3.0f, layerProvider.routeLineScaleValues[0].scaleMultiplier)
        assertEquals(1.0f, layerProvider.routeLineScaleValues[0].scale)
    }

    @Test
    fun getLayerProviderBuildsRouteCasingLineScaleValues() {
        val layerProvider = MapboxRouteLayerProviderFactory.getLayerProvider(
            listOf(),
            ctx,
            styleRes
        )

        assertEquals(5, layerProvider.routeLineCasingScaleValues.size)
        assertEquals(10.0f, layerProvider.routeLineCasingScaleValues[0].scaleStop)
        assertEquals(7.0f, layerProvider.routeLineCasingScaleValues[0].scaleMultiplier)
        assertEquals(1.0f, layerProvider.routeLineCasingScaleValues[0].scale)
    }

    @Test
    fun getLayerProviderBuildsRouteLineTrafficScaleValues() {
        val layerProvider = MapboxRouteLayerProviderFactory.getLayerProvider(
            listOf(),
            ctx,
            styleRes
        )

        assertEquals(6, layerProvider.routeLineTrafficScaleValues.size)
        assertEquals(4.0f, layerProvider.routeLineTrafficScaleValues[0].scaleStop)
        assertEquals(3.0f, layerProvider.routeLineTrafficScaleValues[0].scaleMultiplier)
        assertEquals(1.0f, layerProvider.routeLineTrafficScaleValues[0].scale)
    }
}
