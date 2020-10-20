package com.mapbox.navigation.ui.routealert

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.alert.RouteAlert
import com.mapbox.navigation.base.trip.model.alert.TollCollectionAlert
import com.mapbox.navigation.ui.routealert.MapboxRouteAlertsDisplayer.Companion.getMapboxRouteAlertSymbolLayerProperties

/**
 * Handle [TollCollectionAlert] to add an icon on the map to indicate where a toll is presented.
 *
 * @param tollCollectionAlertDisplayerOptions the options to build a route alert toll view
 */

class TollCollectionAlertDisplayer(
    private val tollCollectionAlertDisplayerOptions: TollCollectionAlertDisplayerOptions
) {
    private var tollCollectionAlertUiModels: List<TollCollectionAlertUiModel> = emptyList()
    private lateinit var tollCollectionsSource: GeoJsonSource
    private lateinit var tollCollectionsLayer: SymbolLayer

    private fun initialize() {
        tollCollectionsSource = RouteAlertDisplayerProvider.getGeoJsonSource(
            MAPBOX_TOLL_COLLECTIONS_SOURCE
        )
        tollCollectionsLayer = RouteAlertDisplayerProvider.getSymbolLayer(
            MAPBOX_TOLL_COLLECTIONS_LAYER,
            MAPBOX_TOLL_COLLECTIONS_SOURCE
        )
            .withProperties(
                *tollCollectionAlertDisplayerOptions.properties.let {
                    if (it.isEmpty()) {
                        getMapboxRouteAlertSymbolLayerProperties()
                    } else {
                        it
                    }
                },
                PropertyFactory.iconImage(MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID),
                PropertyFactory.textField(
                    Expression.get(Expression.literal(MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID))
                )
            )
    }

    /**
     * When [Style] changes, re-add the toll route alerts to the new style.
     *
     * @param style the latest [Style]
     */
    fun onStyleLoaded(style: Style) {
        initialize()

        style.apply {
            addImage(
                MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID,
                tollCollectionAlertDisplayerOptions.drawable
            )
            addSource(tollCollectionsSource)
            addLayer(tollCollectionsLayer)
        }
        onNewRouteTollAlerts(tollCollectionAlertUiModels)
    }

    /**
     * Display toll type route alerts on the map.
     * The [TollCollectionAlertUiModel.tollDescription] is the text shown under the icon.
     *
     * @param tollCollectionAlertUiModels a list of toll alert models
     */
    fun onNewRouteTollAlerts(tollCollectionAlertUiModels: List<TollCollectionAlertUiModel>) {
        this.tollCollectionAlertUiModels = tollCollectionAlertUiModels
        val tollCollectionFeatures = mutableListOf<Feature>()
        tollCollectionAlertUiModels.forEach {
            val feature = Feature.fromGeometry(it.coordinate)
            if (it.tollDescription.isNotEmpty()) {
                feature.addStringProperty(
                    MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID,
                    it.tollDescription
                )
            }
            tollCollectionFeatures.add(feature)
        }

        if (::tollCollectionsSource.isInitialized) {
            tollCollectionsSource.setGeoJson(
                FeatureCollection.fromFeatures(tollCollectionFeatures)
            )
        }
    }

    /**
     * Display [TollCollectionAlert] on the map with the default toll description provided by Mapbox.
     *
     * @param routeAlerts a list of route alerts, it may or may not contain [TollCollectionAlert]
     */
    fun onNewRouteAlerts(routeAlerts: List<RouteAlert>) {
        onNewRouteTollAlerts(
            routeAlerts.filterIsInstance<TollCollectionAlert>().map {
                TollCollectionAlertUiModel.Builder(it.coordinate).build()
            }
        )
    }

    companion object {
        private const val MAPBOX_TOLL_COLLECTIONS_SOURCE = "mapbox_toll_collections_source"
        private const val MAPBOX_TOLL_COLLECTIONS_LAYER = "mapbox_toll_collections_layer"
        private const val MAPBOX_TOLL_COLLECTIONS_TEXT_PROPERTY_ID =
            "mapbox_toll_collections_text_property_id"
        private const val MAPBOX_TOLL_COLLECTIONS_IMAGE_PROPERTY_ID =
            "mapbox_toll_collections_image_property_id"
    }
}
