package com.mapbox.androidauto.placeslistonmap

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.mapbox.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class PlacesListOnMapLayerUtilTest : MapboxRobolectricTestRunner() {

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun removeFavoritesLayer() {
        val style = mockk<Style>(relaxed = true)

        PlacesListOnMapLayerUtil().removePlacesListOnMapLayer(style)

        verify { style.removeStyleLayer("MapboxCarPlacesListLayerId") }
        verify { style.removeStyleSource("MapboxCarPlacesListLayerIdSource") }
        verify { style.removeStyleImage("MapboxGenericLocationIcon") }
    }

    @Test
    fun updateFavoritesLayer() {
        mockkStatic("com.mapbox.maps.extension.style.sources.SourceUtils")
        val source = mockk<GeoJsonSource>(relaxed = true)
        val style = mockk<Style>(relaxed = true) {
            every { getSource("MapboxCarPlacesListLayerIdSource") } returns source
        }
        val featureCollection = FeatureCollection.fromFeatures(listOf())

        PlacesListOnMapLayerUtil().updatePlacesListOnMapLayer(style, featureCollection)

        verify { source.featureCollection(featureCollection) }
    }

    @Test
    fun initializeFavoritesLayer() {
        mockkStatic("com.mapbox.maps.extension.style.layers.LayerUtils")
        mockkObject(GeoJsonSource)
        val style = mockk<Style>(relaxed = true) {
            every { getStyleImage("MapboxGenericLocationIcon") } returns null
            every { styleSourceExists("MapboxCarPlacesListLayerIdSource") } returns false
            every {
                addStyleSource("MapboxCarPlacesListLayerIdSource", any())
            } returns ExpectedFactory.createNone()
            every {
                addLayer(any())
            } returns Unit
        }
        val layerSlot = slot<SymbolLayer>()

        PlacesListOnMapLayerUtil().initializePlacesListOnMapLayer(
            style,
            (ApplicationProvider.getApplicationContext() as Context).resources
        )

        verify { style.addImage("MapboxGenericLocationIcon", any<Bitmap>()) }
        verify { style.addStyleSource("MapboxCarPlacesListLayerIdSource", any()) }
        verify { style.addLayer(capture(layerSlot)) }
        assertEquals("MapboxCarPlacesListLayerIdSource", layerSlot.captured.sourceId)
        assertEquals("MapboxCarPlacesListLayerId", layerSlot.captured.layerId)
    }
}
