package com.mapbox.navigation.ui.route

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.style.layers.LineLayer
import com.mapbox.maps.plugin.style.layers.SymbolLayer
import com.mapbox.maps.plugin.style.layers.getLayer
import com.mapbox.maps.plugin.style.sources.GeojsonSource
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.ThemeSwitcher
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M])
class MapRouteLineTest {

    lateinit var ctx: Context
    var styleRes: Int = 0

    lateinit var wayPointSource: GeojsonSource
    lateinit var primaryRouteLineSource: GeojsonSource
    lateinit var primaryRouteCasingSource: GeojsonSource
    lateinit var primaryRouteLineTrafficSource: GeojsonSource
    lateinit var alternativeRouteLineSource: GeojsonSource

    lateinit var layerProvider: RouteLayerProvider
    lateinit var alternativeRouteCasingLayer: LineLayer
    lateinit var alternativeRouteLayer: LineLayer
    lateinit var primaryRouteCasingLayer: LineLayer
    lateinit var primaryRouteLayer: LineLayer
    lateinit var primaryRouteTrafficLayer: LineLayer
    lateinit var waypointLayer: SymbolLayer

    lateinit var style: Style

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )
        alternativeRouteCasingLayer = mockk {
            every { layerId } returns RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
        }

        alternativeRouteLayer = mockk {
            every { layerId } returns RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
        }

        primaryRouteCasingLayer = mockk {
            every { layerId } returns RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
        }

        primaryRouteLayer = mockk {
            every { layerId } returns RouteConstants.PRIMARY_ROUTE_LAYER_ID
        }

        waypointLayer = mockk {
            every { layerId } returns RouteConstants.WAYPOINT_LAYER_ID
        }

        primaryRouteTrafficLayer = mockk {
            every { layerId } returns RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
        }

        style = mockk(relaxUnitFun = true) {
            every { getLayer(RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID) } returns alternativeRouteLayer
            every {
                getLayer(RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID)
            } returns alternativeRouteCasingLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_LAYER_ID) } returns primaryRouteLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) } returns primaryRouteTrafficLayer
            every { getLayer(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID) } returns primaryRouteCasingLayer
            every { getLayer(RouteConstants.WAYPOINT_LAYER_ID) } returns waypointLayer
            every { isFullyLoaded() } returns false
        }

        wayPointSource = mockk(relaxUnitFun = true)
        primaryRouteLineSource = mockk(relaxUnitFun = true)
        primaryRouteCasingSource = mockk(relaxUnitFun = true)
        primaryRouteLineTrafficSource = mockk(relaxUnitFun = true)
        alternativeRouteLineSource = mockk(relaxUnitFun = true)

        layerProvider = mockk {
            every {
                initializeAlternativeRouteCasingLayer(
                    style,
                    1.0,
                    -9273715
                )
            } returns alternativeRouteCasingLayer
            every {
                initializeAlternativeRouteLayer(
                    style,
                    true,
                    1.0,
                    -7957339
                )
            } returns alternativeRouteLayer
            every {
                initializePrimaryRouteCasingLayer(
                    style,
                    1.0,
                    -13665594
                )
            } returns primaryRouteCasingLayer
            every {
                initializePrimaryRouteLayer(
                    style,
                    true,
                    1.0,
                    -11097861
                )
            } returns primaryRouteLayer
            every { initializeWayPointLayer(style, any(), any()) } returns waypointLayer
            every {
                initializePrimaryRouteTrafficLayer(
                    style,
                    true,
                    1.0,
                    -11097861
                )
            } returns primaryRouteTrafficLayer
        }
    }

    @Test
    fun getExpressionAtOffsetWhenExpressionDataEmpty() {
        //every { style.getLayers() } returns listOf(primaryRouteLayer)
        val expectedExpression = "[\"step\", [\"line-progress\"], [\"rgba\", 0.0, 0.0, 0.0, " +
            "0.0], 0.2, [\"rgba\", 86.0, 168.0, 251.0, 1.0]]"
        val mapRouteLine = MapRouteLine(
            ctx,
            style,
            styleRes,
            null,
            layerProvider,
            listOf<RouteFeatureData>(),
            listOf<RouteLineExpressionData>(),
            true,
            false,
            0f,
            null
        )

        val expression = mapRouteLine.getExpressionAtOffset(.2f)

        Assert.assertEquals(expectedExpression, expression.toString())
    }
}
