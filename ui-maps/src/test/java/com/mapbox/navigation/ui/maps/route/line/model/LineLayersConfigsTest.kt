package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class LineLayersConfigsTest {

    @Test
    fun defaults() {
        val configs = LineLayersConfigs.Builder().build()

        assertEquals(LineConfig.Builder().build(), configs.lineConfig)
    }

    @Test
    fun lineLayersConfigsBuilderTest() {
        val lineConfig = LineConfig.Builder().lineCap(LineCap.SQUARE).build()

        val configs = LineLayersConfigs.Builder()
            .lineConfig(lineConfig)
            .build()

        assertEquals(lineConfig, configs.lineConfig)
    }
}
