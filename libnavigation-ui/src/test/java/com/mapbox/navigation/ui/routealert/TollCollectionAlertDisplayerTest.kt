package com.mapbox.navigation.ui.routealert

import android.graphics.drawable.Drawable
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class TollCollectionAlertDisplayerTest {

    @Before
    fun setUp() {
        mockkObject(RouteAlertDisplayerProvider)
        val tollCollectionsSource = mockk<GeoJsonSource>(relaxed = true)
        val tollCollectionsLayer = mockk<SymbolLayer>(relaxed = true)
        every {
            RouteAlertDisplayerProvider.getGeoJsonSource(any())
        } returns tollCollectionsSource
        every {
            RouteAlertDisplayerProvider.getSymbolLayer(any(), any())
        } returns tollCollectionsLayer
    }

    @After
    fun tearDown() {
        unmockkObject(RouteAlertDisplayerProvider)
    }

    @Test
    fun onStyleLoaded_beforeOnNewRouteAlerts() {
        val tollCollectionAlertDisplayer = createTollCollectionAlertDisplayer()
        val style = mockk<Style>(relaxed = true)

        tollCollectionAlertDisplayer.onStyleLoaded(style)

        style.run {
            verify {
                addImage(any(), any<Drawable>())
                addSource(any())
                addLayer(any())
            }
        }
        verify { tollCollectionAlertDisplayer.onNewRouteTollAlerts(emptyList()) }
    }

    private fun createTollCollectionAlertDisplayer(): TollCollectionAlertDisplayer {
        val tollCollectionAlertDisplayerOptions =
            mockk<TollCollectionAlertDisplayerOptions>(relaxed = true)
        every { tollCollectionAlertDisplayerOptions.drawable } returns mockk(relaxed = true)
        return TollCollectionAlertDisplayer(tollCollectionAlertDisplayerOptions)
    }
}
