package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteSetValueTest {

    @Test
    fun toMutableValue() {
        val original = RouteSetValue(
            RouteLineData(
                mockk(),
                RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
            ),
            listOf(
                RouteLineData(
                    mockk(),
                    RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
                ),
                RouteLineData(
                    mockk(),
                    RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
                )
            ),
            mockk(),
            mockk()
        )

        val result = original.toMutableValue()

        assertEquals(original.primaryRouteLineData, result.primaryRouteLineData)
        assertEquals(original.alternativeRouteLinesData, result.alternativeRouteLinesData)
        assertEquals(original.waypointsSource, result.waypointsSource)
        assertEquals(
            original.routeLineMaskingLayerDynamicData,
            result.routeLineMaskingLayerDynamicData
        )
    }

    @Test
    fun toImmutableValue() {
        val original = RouteSetValue(
            RouteLineData(
                mockk(),
                RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
            ),
            listOf(
                RouteLineData(
                    mockk(),
                    RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
                ),
                RouteLineData(
                    mockk(),
                    RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
                )
            ),
            mockk(),
            mockk()
        )
        val replacedPrimaryRouteData = RouteLineData(
            mockk(),
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
        )
        val replacedAlternativesRouteData = listOf(
            RouteLineData(
                mockk(),
                RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
            ),
            RouteLineData(
                mockk(),
                RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk()),
            )
        )
        val replacedWaypointsSource = mockk<FeatureCollection>()
        val replacementMaskingData = mockk<RouteLineDynamicData>()
        val mutable = original.toMutableValue()

        mutable.primaryRouteLineData = replacedPrimaryRouteData
        mutable.alternativeRouteLinesData = replacedAlternativesRouteData
        mutable.waypointsSource = replacedWaypointsSource
        mutable.routeLineMaskingLayerDynamicData = replacementMaskingData
        val result = mutable.toImmutableValue()

        assertEquals(replacedPrimaryRouteData, result.primaryRouteLineData)
        assertEquals(replacedAlternativesRouteData, result.alternativeRouteLinesData)
        assertEquals(replacedWaypointsSource, result.waypointsSource)
        assertEquals(replacementMaskingData, result.routeLineMaskingLayerDynamicData)
    }
}
