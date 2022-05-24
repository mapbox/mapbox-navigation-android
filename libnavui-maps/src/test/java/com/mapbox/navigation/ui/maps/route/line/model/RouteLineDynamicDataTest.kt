package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.ui.maps.internal.RouteLineTrimOffset
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RouteLineDynamicDataTest {

    @Test
    fun toMutableValue() {
        val original = RouteLineDynamicData(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            RouteLineTrimOffset(9.9)
        )

        val result = original.toMutableValue()

        assertEquals(original.baseExpressionProvider, result.baseExpressionProvider)
        assertEquals(original.casingExpressionProvider, result.casingExpressionProvider)
        assertEquals(
            original.restrictedSectionExpressionProvider,
            result.restrictedSectionExpressionProvider
        )
        assertEquals(original.trafficExpressionProvider, result.trafficExpressionProvider)
        assertEquals(original.trimOffset, result.trimOffset)
    }

    @Test
    fun toImmutableValue() {
        val original = RouteLineDynamicData(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            RouteLineTrimOffset(9.9)
        )
        val replacementBaseProvider = mockk<RouteLineExpressionProvider>()
        val replacementCasingProvider = mockk<RouteLineExpressionProvider>()
        val replacementTrafficProvider = mockk<RouteLineExpressionProvider>()
        val replacementSectionProvider = mockk<RouteLineExpressionProvider>()
        val mutable = original.toMutableValue()

        mutable.baseExpressionProvider = replacementBaseProvider
        mutable.casingExpressionProvider = replacementCasingProvider
        mutable.trafficExpressionProvider = replacementTrafficProvider
        mutable.restrictedSectionExpressionProvider = replacementSectionProvider
        val result = mutable.toImmutableValue()

        assertEquals(replacementBaseProvider, result.baseExpressionProvider)
        assertEquals(replacementCasingProvider, result.casingExpressionProvider)
        assertEquals(replacementTrafficProvider, result.trafficExpressionProvider)
        assertEquals(replacementSectionProvider, result.restrictedSectionExpressionProvider)
        assertEquals(original.trimOffset, result.trimOffset)
    }
}
