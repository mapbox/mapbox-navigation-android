package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LineDashConfigTest {

    @Test
    fun defaults() {
        val config = LineDashConfig.Builder().build()

        assertEquals(0.0, config.dashLength, 0.0)
        assertEquals(0.0, config.dashGap, 0.0)
    }

    @Test
    fun customValues() {
        val config = LineDashConfig.Builder()
            .dashLength(1.0)
            .dashGap(2.0)
            .build()

        assertEquals(1.0, config.dashLength, 0.0)
        assertEquals(2.0, config.dashGap, 0.0)
        assertEquals(listOf(1.0, 2.0), config.toList())
    }
}
