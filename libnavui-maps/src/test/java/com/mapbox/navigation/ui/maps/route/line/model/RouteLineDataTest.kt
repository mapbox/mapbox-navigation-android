package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.route.NavigationRoute
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RouteLineDataTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineData(
            mockk(),
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk())
        )

        val result = original.toMutableValue()

        assertEquals(original.featureCollection, result.featureCollection)
        assertEquals(original.dynamicData, result.dynamicData)
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineData(
            mockk(),
            RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk())
        )
        val replacementFeatureCollection = mockk<FeatureCollection>()
        val replacementDynamicData = RouteLineDynamicData(mockk(), mockk(), mockk(), mockk(), mockk())
        val replacementNavigationRoute = mockk<NavigationRoute>()
        val mutable = original.toMutableValue()

        mutable.featureCollection = replacementFeatureCollection
        mutable.dynamicData = replacementDynamicData
        val result = mutable.toImmutableValue()

        assertEquals(replacementFeatureCollection, result.featureCollection)
        assertEquals(replacementDynamicData, result.dynamicData)
    }
}
