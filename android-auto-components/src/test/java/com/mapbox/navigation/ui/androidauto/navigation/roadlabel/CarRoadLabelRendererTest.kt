package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.renderer.widget.BitmapWidget
import com.mapbox.maps.renderer.widget.Widget
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

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

        assertEquals(-49f, widget.captured.getPosition().offsetY)
        sut.onDetached(mapboxCarMapSurface)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `label is rendered again within a narrower visible area`() {
        val sut = attach()
        sendRoad()
        val unlimited = bitmaps.last()

        // 220dp are reserved for the bottom corners, so 300dp are left for the label.
        sut.onVisibleAreaChanged(Rect(0, 0, 520, 300), NO_INSETS)

        val limited = bitmaps.last()
        assertTrue("limited=${limited.width}", limited.width in 201..300)
        assertTrue(limited.width < unlimited.width)
        assertEquals(unlimited.height, limited.height)
        detach(sut)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `same visible area does not render the label again`() {
        val sut = attach()
        sendRoad()
        sut.onVisibleAreaChanged(Rect(0, 0, 520, 300), NO_INSETS)
        val renderCount = bitmaps.size

        sut.onVisibleAreaChanged(Rect(0, 0, 520, 300), EdgeInsets(0.0, 0.0, 10.0, 0.0))

        assertEquals(renderCount, bitmaps.size)
        detach(sut)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `label is hidden when the corners leave too little space`() {
        val sut = attach()
        sendRoad()

        // 200dp minus the 220dp corner reserve leaves no room for the label.
        sut.onVisibleAreaChanged(Rect(0, 0, 200, 300), NO_INSETS)

        assertEquals(1, bitmaps.last().width)
        detach(sut)
    }

    // The legacy graphics shadows measure any text as empty.
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun `width limit is reset when the surface is attached again`() {
        val sut = attach()
        sendRoad()
        val unlimitedWidth = bitmaps.last().width
        sut.onVisibleAreaChanged(Rect(0, 0, 520, 300), NO_INSETS)
        sut.onDetached(mapboxCarMapSurface)

        sut.onAttached(mapboxCarMapSurface)
        sendRoad()

        assertEquals(unlimitedWidth, bitmaps.last().width)
        detach(sut)
    }

    private val bitmaps = mutableListOf<Bitmap>()
    private val locationObserver = slot<LocationObserver>()
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
        every { registerLocationObserver(capture(locationObserver)) } just runs
    }
    private val mapboxCarMapSurface = mockk<MapboxCarMapSurface>(relaxed = true) {
        every { carContext.resources.configuration } returns Configuration().also {
            it.densityDpi = 160
        }
    }

    private fun attach(): CarRoadLabelRenderer {
        mockkConstructor(BitmapWidget::class)
        every { anyConstructed<BitmapWidget>().updateBitmap(capture(bitmaps)) } just runs
        carAppTestRule.onAttached(mapboxNavigation)
        return CarRoadLabelRenderer().also { it.onAttached(mapboxCarMapSurface) }
    }

    private fun detach(sut: CarRoadLabelRenderer) {
        sut.onDetached(mapboxCarMapSurface)
        carAppTestRule.onDetached(mapboxNavigation)
    }

    private fun sendRoad() {
        locationObserver.captured.onNewLocationMatcherResult(
            mockk(relaxed = true) {
                every { road.components } returns listOf(
                    mockk<RoadComponent>(relaxed = true) {
                        every { text } returns "A Very Long Street Name That Does Not Fit"
                        every { shield } returns null
                        every { imageBaseUrl } returns null
                    },
                )
            },
        )
    }

    private companion object {
        private val NO_INSETS = EdgeInsets(0.0, 0.0, 0.0, 0.0)
    }
}
