package com.mapbox.navigation.ui.internal.route

import com.mapbox.navigation.ui.route.RouteStyleDescriptor
import org.junit.Assert.assertEquals
import org.junit.Test

class MapboxRouteLayerProviderTest {

    @Test
    fun getRouteLineColorExpressions() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"==\", [\"get\", \"myRouteId\"], true], [\"rgba\", 0.0, 2.0, 43.0, 0.0], [\"==\", [\"get\", \"anotherRouteId\"], true], [\"rgba\", 0.0, 0.0, 111.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf(
                RouteStyleDescriptor("myRouteId", 555, 999),
                RouteStyleDescriptor("anotherRouteId", 111, 222)
            )
        }

        val result = layerProvider.getRouteLineColorExpressions(2121)

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getRouteLineColorExpressionsWhenDescriptorsEmpty() {
        val expectedResult = "[[\"==\", [\"get\", \"mapboxDescriptorPlaceHolderUnused\"], true], [\"rgba\", 0.0, 8.0, 73.0, 0.0], [\"rgba\", 0.0, 8.0, 73.0, 0.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
        }

        val result = layerProvider.getRouteLineColorExpressions(2121)

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getRouteLineWidthExpressions() {
        val expectedResult = "[\"interpolate\", [\"exponential\", 1.5], [\"zoom\"], 4.0, [\"*\", 3.0, 0.75], 10.0, [\"*\", 4.0, 0.75], 13.0, [\"*\", 6.0, 0.75], 16.0, [\"*\", 10.0, 0.75], 19.0, [\"*\", 14.0, 0.75], 22.0, [\"*\", 18.0, 0.75]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
        }

        val result = layerProvider.getRouteLineWidthExpressions(0.75f)

        assertEquals(expectedResult, result.toString())
    }

    @Test
    fun getShieldLineWidthExpression() {
        val expectedResult = "[\"interpolate\", [\"exponential\", 1.5], [\"zoom\"], 10.0, 7.0, 14.0, [\"*\", 10.5, 1.0], 16.5, [\"*\", 15.5, 1.0], 19.0, [\"*\", 24.0, 1.0], 22.0, [\"*\", 29.0, 1.0]]"
        val layerProvider = object : MapboxRouteLayerProvider {
            override val routeStyleDescriptors: List<RouteStyleDescriptor> = listOf()
        }

        val result = layerProvider.getShieldLineWidthExpression(1f)

        assertEquals(expectedResult, result.toString())
    }
}
