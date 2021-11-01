package com.mapbox.navigation.ui.maps.route.line.model

import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteLineUpdateValueTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineUpdateValue(
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk()),
            listOf()
        )

        val result = original.toMutableValue()

        assertEquals(original.primaryRouteLineDynamicData, result.primaryRouteLineDynamicData)
        assertEquals(
            original.alternativeRouteLinesDynamicData,
            result.alternativeRouteLinesDynamicData
        )
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineUpdateValue(
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk()),
            listOf()
        )
        val replacementPrimaryRouteLineDynamicData =
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk())
        val replacementList = listOf<RouteLineDynamicData>()
        val mutable = original.toMutableValue()

        mutable.primaryRouteLineDynamicData = replacementPrimaryRouteLineDynamicData
        mutable.alternativeRouteLinesDynamicData = replacementList
        val result = mutable.toImmutableValue()

        assertEquals(replacementPrimaryRouteLineDynamicData, result.primaryRouteLineDynamicData)
        assertEquals(replacementList, result.alternativeRouteLinesDynamicData)
    }
}
