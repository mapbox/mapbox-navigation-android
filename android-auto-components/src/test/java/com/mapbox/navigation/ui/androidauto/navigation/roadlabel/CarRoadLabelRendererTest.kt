package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.res.Configuration
import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.renderer.widget.Widget
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(MapboxExperimental::class, ExperimentalCoroutinesApi::class)
class CarRoadLabelRendererTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `label margin is scaled with the density`() {
        val widget = slot<Widget>()
        val mapboxCarMapSurface = mockk<MapboxCarMapSurface>(relaxed = true) {
            every { carContext.resources.configuration } returns Configuration().also {
                it.densityDpi = 320
            }
            every { mapSurface.addWidget(capture(widget)) } answers { }
        }
        val sut = CarRoadLabelRenderer()

        sut.onAttached(mapboxCarMapSurface)
        sut.onVisibleAreaChanged(Rect(), EdgeInsets(0.0, 0.0, 5.0, 0.0))

        assertEquals(-25f, widget.captured.getPosition().offsetY)
        sut.onDetached(mapboxCarMapSurface)
    }
}
