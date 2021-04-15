package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.linear
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.zoom
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.maps.arrival.model.MapboxBuildingHighlightOptions

/**
 * Map layer to highlight a single building.
 */
class MapboxBuildingHighlightApi(
    private val mapboxMap: MapboxMap,
    private val options: MapboxBuildingHighlightOptions =
        MapboxBuildingHighlightOptions.default
) {
    /**
     * Highlight a building whose feature geometry that contains the [point]. If
     * no building contains this point, a building is not highlighted.
     *
     * @param point for finding a building to highlight,
     *   null will clear any highlighted building
     */
    fun highlightBuilding(
        point: Point?
    ) {
        mapboxMap.getStyle { style ->

            if (point == null) {
                updateBuildingLayer(style, emptyList())
                return@getStyle
            }

            val screenCoordinate = mapboxMap.pixelForCoordinate(point)
            val queryOptions = RenderedQueryOptions(listOf("building"), null)

            mapboxMap.queryRenderedFeatures(screenCoordinate, queryOptions) { expected ->
                val queriedFeature: List<QueriedFeature> = expected.value ?: emptyList()
                if (queriedFeature.isNotEmpty()) {
                    updateBuildingLayer(style, queriedFeature)
                }
            }
        }
    }

    /**
     * Remove the building layer and source.
     */
    fun clear() {
        mapboxMap.getStyle { style ->
            style.removeStyleLayer(BUILDING_LAYER_ID)
            style.removeStyleSource(BUILDING_SOURCE_ID)
        }
    }

    private fun updateBuildingLayer(
        style: Style,
        queriedFeature: List<QueriedFeature>
    ) {
        val layerId = BUILDING_LAYER_ID
        val sourceId = BUILDING_SOURCE_ID

        val features = queriedFeature.map { it.feature }
        if (style.styleSourceExists(sourceId)) {
            (style.getSource(sourceId) as GeoJsonSource)
                .featureCollection(FeatureCollection.fromFeatures(features))
        } else {
            style.addSource(
                GeoJsonSource.Builder(sourceId)
                    .featureCollection(FeatureCollection.fromFeatures(features))
                    .build()
            )
        }

        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                FillExtrusionLayer(layerId, sourceId)
                    .fillExtrusionColor(options.fillExtrusionColor)
                    .fillExtrusionOpacity(options.fillExtrusionOpacity)
                    .fillExtrusionBase(get("min-height"))
                    .fillExtrusionHeight(heightExpression())
            )
        }
    }

    private fun heightExpression(): Expression =
        interpolate(
            linear(), zoom(),
            literal(15.0), literal(0),
            literal(15.05), get("height")
        )

    private companion object {
        private const val BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        private const val BUILDING_SOURCE_ID = "mapbox-building-highlight-source"
    }
}
