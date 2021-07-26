package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.id
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.inExpression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.interpolate
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.linear
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.zoom
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
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
     * Access the currently highlighted buildings.
     */
    var highlightedBuildings: List<QueriedFeature> = emptyList()
        private set

    /**
     * Access the expression used to create the building's height.
     */
    val buildingHeightExpression: Expression =
        interpolate(
            linear(), zoom(),
            literal(15.0), literal(0),
            literal(15.05), get("height")
        )

    /**
     * Highlight a building whose feature geometry that contains the [point]. If
     * no building contains this point, a building is not highlighted.
     *
     * @param point for finding a building to highlight,
     *   null will clear any highlighted building
     * @param callback optional callback with the queried feature results.
     */
    fun highlightBuilding(
        point: Point?,
        callback: BuildingHighlightObserver
    ) {
        mapboxMap.getStyle { style ->
            if (point == null) {
                highlightedBuildings = emptyList()
                updateHighlightBuildingLayer(style)
                callback.onBuildingHighlight(highlightedBuildings)
                return@getStyle
            }

            val screenCoordinate = mapboxMap.pixelForCoordinate(point)

            val queryOptions = RenderedQueryOptions(listOf(BUILDING_LAYER_ID), null)
            mapboxMap.queryRenderedFeatures(screenCoordinate, queryOptions) { expected ->
                highlightedBuildings = expected.value ?: emptyList()
                updateHighlightBuildingLayer(style)
                callback.onBuildingHighlight(highlightedBuildings)
            }
        }
    }

    /**
     * Remove the building layer and source.
     */
    fun clear() {
        mapboxMap.getStyle { style ->
            style.removeStyleLayer(HIGHLIGHT_BUILDING_LAYER_ID)
        }
    }

    private fun updateHighlightBuildingLayer(style: Style) {
        val layerId = HIGHLIGHT_BUILDING_LAYER_ID
        val ids = highlightedBuildings.mapNotNull { it.feature.id()?.toLong() }
        val selectedBuilding = inExpression(id(), literal(ids))
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                FillExtrusionLayer(layerId, COMPOSITE_SOURCE_ID)
                    .sourceLayer(BUILDING_LAYER_ID)
                    .filter(selectedBuilding)
                    .fillExtrusionColor(options.fillExtrusionColor)
                    .fillExtrusionOpacity(options.fillExtrusionOpacity)
                    .fillExtrusionBase(get("min-height"))
                    .fillExtrusionHeight(buildingHeightExpression)
            )
        } else {
            style.getLayerAs<FillExtrusionLayer>(layerId)
                .filter(selectedBuilding)
        }
    }

    companion object {
        /**
         * Layer_id used for highlighting a building.
         */
        const val HIGHLIGHT_BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        /**
         * Source_id used for highlighting a building.
         */
        const val COMPOSITE_SOURCE_ID = "composite"
        /**
         * Source layer that includes building height.
         */
        const val BUILDING_LAYER_ID = "building"
    }
}
