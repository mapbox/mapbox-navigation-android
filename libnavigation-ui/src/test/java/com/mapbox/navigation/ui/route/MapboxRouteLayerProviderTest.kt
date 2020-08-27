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
class MapboxRouteLayerProviderTest {

    lateinit var ctx: Context
    var styleRes: Int = 0
    private lateinit var routeLineScaleValuesList: List<RouteLineScaleValue>
    private lateinit var routeLineTrafficScaleValuesList: List<RouteLineScaleValue>
    private lateinit var routeLineCasingScaleValuesList: List<RouteLineScaleValue>

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        styleRes = ThemeSwitcher.retrieveAttrResourceId(
            ctx,
            R.attr.navigationViewRouteStyle,
            R.style.MapboxStyleNavigationMapRoute
        )

        routeLineScaleValuesList = MapRouteLine.MapRouteLineSupport.getRouteLineScalingValues(
            styleRes,
            ctx,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleStops,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScaleMultipliers,
            R.styleable.MapboxStyleNavigationMapRoute_routeLineScales,
            R.styleable.MapboxStyleNavigationMapRoute
        )

        routeLineTrafficScaleValuesList =
            MapRouteLine.MapRouteLineSupport.getRouteLineScalingValues(
                styleRes,
                ctx,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleStops,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleMultipliers,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScales,
                R.styleable.MapboxStyleNavigationMapRoute
            )

        routeLineCasingScaleValuesList =
            MapRouteLine.MapRouteLineSupport.getRouteLineScalingValues(
                styleRes,
                ctx,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleStops,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScaleMultipliers,
                R.styleable.MapboxStyleNavigationMapRoute_routeLineTrafficScales,
                R.styleable.MapboxStyleNavigationMapRoute
            )
    }

    @Test
    fun getRouteLineColorExpressions() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], " +
            "true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"==\", [\"get\", \"myRouteId\"], true], " +
            "[\"rgba\", 0.0, 2.0, 43.0, 0.0], [\"==\", [\"get\", \"anotherRouteId\"], true], " +
            "[\"rgba\", 0.0, 0.0, 111.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf(
                RouteStyleDescriptor("myRouteId", 555, 999),
                RouteStyleDescriptor("anotherRouteId", 111, 222)
            )
            override val routeLineScaleValues: List<RouteLineScaleValue> = routeLineScaleValuesList
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                routeLineTrafficScaleValuesList
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                routeLineCasingScaleValuesList
        }

        val result =
            layerProvider.getRouteLineColorExpressions(
                2121,
                RouteStyleDescriptor::lineColorResourceId
            )

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getRouteLineColorExpressionsWhenDescriptorsEmpty() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], " +
            "true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
            override val routeLineScaleValues: List<RouteLineScaleValue> = routeLineScaleValuesList
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                routeLineTrafficScaleValuesList
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                routeLineCasingScaleValuesList
        }

        val result = layerProvider.getRouteLineColorExpressions(
            2121,
            RouteStyleDescriptor::lineColorResourceId
        )

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getRouteLineCasingColorExpressions() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], " +
            "true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"==\", [\"get\", \"myRouteId\"], true], " +
            "[\"rgba\", 0.0, 3.0, 231.0, 0.0], [\"==\", [\"get\", \"anotherRouteId\"], true], " +
            "[\"rgba\", 0.0, 0.0, 222.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf(
                RouteStyleDescriptor("myRouteId", 555, 999),
                RouteStyleDescriptor("anotherRouteId", 111, 222)
            )
            override val routeLineScaleValues: List<RouteLineScaleValue> = routeLineScaleValuesList
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                routeLineTrafficScaleValuesList
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                routeLineCasingScaleValuesList
        }

        val result = layerProvider.getRouteLineColorExpressions(
            2121,
            RouteStyleDescriptor::lineShieldColorResourceId
        )

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getRouteLineCasingColorExpressionsWhenDescriptorsEmpty() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], " +
            "true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
            override val routeLineScaleValues: List<RouteLineScaleValue> = routeLineScaleValuesList
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                routeLineTrafficScaleValuesList
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                routeLineCasingScaleValuesList
        }

        val result = layerProvider.getRouteLineColorExpressions(
            2121,
            RouteStyleDescriptor::lineShieldColorResourceId
        )

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun buildScalingExpressionTest() {
        val expectedResult = "[\"interpolate\", [\"exponential\", 1.5], [\"zoom\"], 4.0, " +
            "[\"*\", 3.0, 0.75], 10.0, [\"*\", 4.0, 0.75], 13.0, [\"*\", 6.0, 0.75], 16.0, " +
            "[\"*\", 10.0, 0.75], 19.0, [\"*\", 14.0, 0.75], 22.0, [\"*\", 18.0, 0.75]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
            override val routeLineScaleValues: List<RouteLineScaleValue> = routeLineScaleValuesList
            override val routeLineTrafficScaleValues: List<RouteLineScaleValue> =
                routeLineTrafficScaleValuesList
            override val routeLineCasingScaleValues: List<RouteLineScaleValue> =
                routeLineCasingScaleValuesList
        }

        val scalingValues = listOf(
            RouteLineScaleValue(4f, 3f, 0.75f),
            RouteLineScaleValue(10f, 4f, 0.75f),
            RouteLineScaleValue(13f, 6f, 0.75f),
            RouteLineScaleValue(16f, 10f, 0.75f),
            RouteLineScaleValue(19f, 14f, 0.75f),
            RouteLineScaleValue(22f, 18f, 0.75f)
        )

        val expression = layerProvider.buildScalingExpression(scalingValues)

        assertEquals(expectedResult, expression.toString())
    }
}
