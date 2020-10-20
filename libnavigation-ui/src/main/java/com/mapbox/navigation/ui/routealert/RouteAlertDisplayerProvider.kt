package com.mapbox.navigation.ui.routealert

import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource

/**
 * An internal object to initiate [GeoJsonSource]/[SymbolLayer] to help with unit test.
 */
internal object RouteAlertDisplayerProvider {
    fun getGeoJsonSource(sourceId: String): GeoJsonSource {
        return GeoJsonSource(sourceId)
    }

    fun getSymbolLayer(layerId: String, sourceId: String): SymbolLayer {
        return SymbolLayer(layerId, sourceId)
    }
}
