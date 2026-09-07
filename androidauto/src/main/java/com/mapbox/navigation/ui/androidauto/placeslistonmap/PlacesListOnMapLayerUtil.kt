package com.mapbox.navigation.ui.androidauto.placeslistonmap

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.UiThread
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.androidauto.R

@UiThread
class PlacesListOnMapLayerUtil {

    fun initializePlacesListOnMapLayer(style: Style, resources: Resources) {
        if (style.getStyleImage(GENERIC_LOCATION_ICON) == null) {
            style.addImage(
                GENERIC_LOCATION_ICON,
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.marker_icon_20px_red, // todo replace this hardcoded icon
                ),
            )
        }

        if (!style.styleSourceExists(PLACES_LAYER_SOURCE_ID)) {
            geoJsonSource(PLACES_LAYER_SOURCE_ID) {
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (!style.styleLayerExists(PLACES_LAYER_ID)) {
            val placesLayer = SymbolLayer(PLACES_LAYER_ID, PLACES_LAYER_SOURCE_ID)
                .iconAllowOverlap(true)
                .iconIgnorePlacement(true)
                .iconRotationAlignment(IconRotationAlignment.VIEWPORT)
                .iconImage(GENERIC_LOCATION_ICON) // todo support for different place icons?

            style.addLayer(placesLayer)
        }
    }

    fun removePlacesListOnMapLayer(style: Style) {
        style.removeStyleLayer(PLACES_LAYER_ID)
        style.removeStyleSource(PLACES_LAYER_SOURCE_ID)
        style.removeStyleImage(GENERIC_LOCATION_ICON)
    }

    fun updatePlacesListOnMapLayer(style: Style, featureCollection: FeatureCollection) {
        style.getSource(PLACES_LAYER_SOURCE_ID)?.apply {
            (this as GeoJsonSource).featureCollection(featureCollection)
        }
    }

    companion object {
        private const val PLACES_LAYER_ID = "MapboxCarPlacesListLayerId"
        private const val PLACES_LAYER_SOURCE_ID = "MapboxCarPlacesListLayerIdSource"
        private const val GENERIC_LOCATION_ICON = "MapboxGenericLocationIcon"
    }
}
