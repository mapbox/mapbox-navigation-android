package com.mapbox.navigation.ui.androidauto.map.logo

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import androidx.car.app.CarContext
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.renderer.widget.Widget
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(MapboxExperimental::class)
class CarLogoRendererTest : MapboxRobolectricTestRunner() {

    private val widget = slot<Widget>()
    private val mapboxCarMapSurface = mockk<MapboxCarMapSurface>(relaxed = true) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val configuration = Configuration(context.resources.configuration).also {
            it.densityDpi = 320
        }
        every { carContext } returns mockk<CarContext>(relaxed = true) {
            every { resources } returns context.createConfigurationContext(configuration).resources
        }
        every { mapSurface.addWidget(capture(widget)) } answers { }
    }

    @Test
    fun `logo starts at the density scaled margins`() {
        val sut = CarLogoRenderer()

        sut.onAttached(mapboxCarMapSurface)

        assertEquals(-32f, widget.captured.getPosition().offsetX)
        assertEquals(-16f, widget.captured.getPosition().offsetY)
        sut.onDetached(mapboxCarMapSurface)
    }

    @Test
    fun `logo margins are scaled with the density`() {
        val sut = CarLogoRenderer()

        sut.onAttached(mapboxCarMapSurface)
        sut.onVisibleAreaChanged(Rect(), EdgeInsets(0.0, 0.0, 5.0, 7.0))

        assertEquals(-39f, widget.captured.getPosition().offsetX)
        assertEquals(-21f, widget.captured.getPosition().offsetY)
        sut.onDetached(mapboxCarMapSurface)
    }
}
