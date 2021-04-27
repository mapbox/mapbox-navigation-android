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
import com.mapbox.maps.extension.style.layers.getLayer
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

            val queryOptions = RenderedQueryOptions(listOf(BUILDING_LAYER_ID), null)
            mapboxMap.queryRenderedFeatures(screenCoordinate, queryOptions) { expected ->
                val queriedFeature: List<QueriedFeature> = expected.value ?: emptyList()
                updateBuildingLayer(style, queriedFeature)
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

    private fun updateBuildingLayer(
        style: Style,
        queriedFeature: List<QueriedFeature>
    ) {
        val layerId = HIGHLIGHT_BUILDING_LAYER_ID

        val ids = queriedFeature.mapNotNull { it.feature.id()?.toLong() }

        val selectedBuilding = inExpression(id(), literal(ids))
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                FillExtrusionLayer(layerId, COMPOSITE_SOURCE_ID)
                    .sourceLayer(BUILDING_LAYER_ID)
                    .filter(selectedBuilding)
                    .fillExtrusionColor(options.fillExtrusionColor)
                    .fillExtrusionOpacity(options.fillExtrusionOpacity)
                    .fillExtrusionBase(get("min-height"))
                    .fillExtrusionHeight(heightExpression())
            )
        } else {
            (style.getLayer(layerId) as FillExtrusionLayer)
                .filter(selectedBuilding)
        }
    }

    private fun heightExpression(): Expression =
        interpolate(
            linear(), zoom(),
            literal(15.0), literal(0),
            literal(15.05), get("height")
        )

    private companion object {
        private const val HIGHLIGHT_BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        private const val BUILDING_LAYER_ID = "building"
        private const val COMPOSITE_SOURCE_ID = "composite"
    }
}
