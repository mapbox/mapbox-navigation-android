package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LineConfigTest {

    @Test
    fun defaults() {
        val config = LineConfig.Builder().build()

        assertEquals(LineCap.ROUND, config.lineCap)
        assertEquals(LineJoin.ROUND, config.lineJoin)
        assertNull(config.lineDashConfig)
        assertEquals(emptyList<Double>(), config.toDashArray())
    }

    @Test
    fun toDashWithNoDashArrayConfigReturnsEmptyList() {
        val config = LineConfig.Builder().lineDashConfig(null).build()

        assertEquals(emptyList<Double>(), config.toDashArray())
    }

    @Test
    fun toDashWithDashConfigReturnsDashArrayLengthAndGap() {
        val dashConfig = LineDashConfig.Builder().dashLength(1.0).dashGap(2.0).build()
        val config = LineConfig.Builder().lineDashConfig(dashConfig).build()

        assertEquals(listOf(1.0, 2.0), config.toDashArray())
    }

    @Test
    fun customCapAndJoin() {
        val config = LineConfig.Builder()
            .lineCap(LineCap.SQUARE)
            .lineJoin(LineJoin.BEVEL)
            .build()

        assertEquals(LineCap.SQUARE, config.lineCap)
        assertEquals(LineJoin.BEVEL, config.lineJoin)
    }
}
