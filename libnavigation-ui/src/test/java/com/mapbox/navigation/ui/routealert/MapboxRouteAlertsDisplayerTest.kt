package com.mapbox.navigation.ui.routealert

import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class MapboxRouteAlertsDisplayerTest {
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
    fun onStyleLoaded() {
        val mockedTollCollectionAlertDisplayer = mockk<TollCollectionAlertDisplayer>(relaxed = true)
        val displayer = createMapboxRouteAlertsDisplayer(
            mockedTollCollectionAlertDisplayer = mockedTollCollectionAlertDisplayer
        )
        val style = mockk<Style>()

        displayer.onStyleLoaded(style)

        verify { mockedTollCollectionAlertDisplayer.onStyleLoaded(eq(style)) }
    }

    @Test
    fun onStyleLoaded_nullStyle() {
        val mockedTollCollectionAlertDisplayer = mockk<TollCollectionAlertDisplayer>(relaxed = true)
        val displayer = createMapboxRouteAlertsDisplayer(
            mockedTollCollectionAlertDisplayer = mockedTollCollectionAlertDisplayer
        )

        displayer.onStyleLoaded(null)

        verify(exactly = 0) { mockedTollCollectionAlertDisplayer.onStyleLoaded(any()) }
    }

    @Test
    fun onNewRouteAlerts() {
        val mockedTollCollectionAlertDisplayer = mockk<TollCollectionAlertDisplayer>(relaxed = true)
        val displayer = createMapboxRouteAlertsDisplayer(
            mockedTollCollectionAlertDisplayer = mockedTollCollectionAlertDisplayer
        )

        val list = mockk<List<RouteAlert>>()

        displayer.onNewRouteAlerts(list)

        verify { mockedTollCollectionAlertDisplayer.onNewRouteAlerts(eq(list)) }
    }

    @Test
    fun onNewRouteAlerts_withOptionShowTollIsFalse() {
        val mockedTollCollectionAlertDisplayer = mockk<TollCollectionAlertDisplayer>(relaxed = true)
        val displayer = createMapboxRouteAlertsDisplayer(false, mockedTollCollectionAlertDisplayer)

        val list = mockk<List<RouteAlert>>()

        displayer.onNewRouteAlerts(list)

        verify(exactly = 0) { mockedTollCollectionAlertDisplayer.onNewRouteAlerts(any()) }
    }

    private fun createMapboxRouteAlertsDisplayer(
        showToll: Boolean = true,
        mockedTollCollectionAlertDisplayer: TollCollectionAlertDisplayer = mockk(relaxed = true)
    ):
        MapboxRouteAlertsDisplayer {
            val options = mockk<MapboxRouteAlertsDisplayerOptions>(relaxed = true)
            every { options.showToll } returns showToll

            return MapboxRouteAlertsDisplayer(options, mockedTollCollectionAlertDisplayer)
        }
}
