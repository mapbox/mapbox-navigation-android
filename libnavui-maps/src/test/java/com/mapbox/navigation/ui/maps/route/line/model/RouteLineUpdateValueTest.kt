package com.mapbox.navigation.ui.maps.route.line.model

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteLineUpdateValueTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineUpdateValue(
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk()),
            listOf(),
            mockk()
        )

        val result = original.toMutableValue()

        assertEquals(original.primaryRouteLineDynamicData, result.primaryRouteLineDynamicData)
        assertEquals(
            original.alternativeRouteLinesDynamicData,
            result.alternativeRouteLinesDynamicData
        )
        assertEquals(
            original.routeLineMaskingLayerDynamicData,
            result.routeLineMaskingLayerDynamicData
        )
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineUpdateValue(
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk()),
            listOf(),
            mockk()
        )
        val replacementPrimaryRouteLineDynamicData =
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk())
        val replacementList = listOf<RouteLineDynamicData>()
        val mutable = original.toMutableValue()
        val replacementMaskingData = mockk<RouteLineDynamicData>()

        mutable.primaryRouteLineDynamicData = replacementPrimaryRouteLineDynamicData
        mutable.alternativeRouteLinesDynamicData = replacementList
        mutable.routeLineMaskingLayerDynamicData = replacementMaskingData
        val result = mutable.toImmutableValue()

        assertEquals(replacementPrimaryRouteLineDynamicData, result.primaryRouteLineDynamicData)
        assertEquals(replacementList, result.alternativeRouteLinesDynamicData)
        assertEquals(replacementMaskingData, result.routeLineMaskingLayerDynamicData)
    }
}
