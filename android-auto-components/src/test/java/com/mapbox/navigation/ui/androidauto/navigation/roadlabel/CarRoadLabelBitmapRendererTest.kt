package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CarRoadLabelBitmapRendererTest : MapboxRobolectricTestRunner() {

    private val sut = CarRoadLabelBitmapRenderer()
    private val road = listOf(
        mockk<RoadComponent>(relaxed = true) {
            every { text } returns "Main Street"
            every { shield } returns null
            every { imageBaseUrl } returns null
        },
    )

    @Test
    fun `label is scaled with the density`() {
        val baseline = sut.render(resources(densityDpi = 160), road, emptyList())!!
        val doubled = sut.render(resources(densityDpi = 320), road, emptyList())!!

        // Text bounds are rounded separately at each size, so allow a pixel of rounding.
        assertEquals(baseline.width * 2f, doubled.width.toFloat(), 2f)
        assertEquals(baseline.height * 2f, doubled.height.toFloat(), 2f)
    }

    private fun resources(densityDpi: Int): Resources {
        val context: Context = ApplicationProvider.getApplicationContext()
        val configuration = Configuration(context.resources.configuration).also {
            it.densityDpi = densityDpi
        }
        return context.createConfigurationContext(configuration).resources
    }
}
