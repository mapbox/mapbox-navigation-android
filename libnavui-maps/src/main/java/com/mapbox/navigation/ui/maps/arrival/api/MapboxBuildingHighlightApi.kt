package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
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
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.not
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.zoom
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.navigation.ui.maps.arrival.model.MapboxBuildingHighlightOptions
import com.mapbox.navigation.utils.internal.LoggerProvider.logger

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
     * Access the current state of the extruded buildings layer.
     */
    var extrudeBuildings: Boolean = false
        private set

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
        callback: (List<QueriedFeature>) -> Unit = { }
    ) {
        mapboxMap.getStyle { style ->
            if (point == null) {
                highlightedBuildings = emptyList()
                updateBuildingsLayer(style)
                callback(highlightedBuildings)
                return@getStyle
            }

            val screenCoordinate = mapboxMap.pixelForCoordinate(point)

            val queryOptions = RenderedQueryOptions(listOf(BUILDING_LAYER_ID), null)
            mapboxMap.queryRenderedFeatures(screenCoordinate, queryOptions) { expected ->
                highlightedBuildings = expected.value ?: emptyList()
                updateBuildingsLayer(style)
                callback(highlightedBuildings)
            }
        }
    }

    /**
     * Show all buildings on the map.
     */
    fun extrudeBuildings(extrudeBuildings: Boolean) = also {
        this.extrudeBuildings = extrudeBuildings
        mapboxMap.getStyle { style ->
            updateBuildingsLayer(style)
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

    private fun updateBuildingsLayer(style: Style) {
        updateHighlightBuildingLayer(style)
        updateFillBuildingsLayer(style)
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
                    .fillExtrusionHeight(heightExpression())
            )
        } else {
            style.getLayerAs<FillExtrusionLayer>(layerId)
                .filter(selectedBuilding)
        }
    }

    private fun updateFillBuildingsLayer(style: Style) {
        val layerId = EXTRUDE_BUILDING_LAYER_ID
        if (!extrudeBuildings) {
            style.removeStyleLayer(layerId)
            return
        }
        val buildingFillColorExpression = buildingFillColor(style) ?: return

        val ids = highlightedBuildings.mapNotNull { it.feature.id()?.toLong() }
        val notSelectedBuildings = not(inExpression(id(), literal(ids)))
        if (!style.styleLayerExists(layerId)) {
            style.addLayer(
                FillExtrusionLayer(layerId, COMPOSITE_SOURCE_ID)
                    .sourceLayer(BUILDING_LAYER_ID)
                    .filter(notSelectedBuildings)
                    .fillExtrusionColor(buildingFillColorExpression)
                    .fillExtrusionOpacity(literal(0.6))
                    .fillExtrusionBase(get("min-height"))
                    .fillExtrusionHeight(heightExpression())
            )
        } else {
            style.getLayerAs<FillExtrusionLayer>(layerId)
                .filter(notSelectedBuildings)
        }
    }

    private fun buildingFillColor(style: Style): Expression? {
        return when (val buildingLayer = style.getLayer(BUILDING_LAYER_ID)) {
            is FillLayer -> buildingLayer.fillColorAsExpression
            else -> {
                logger.e(
                    tag = TAG,
                    msg = Message("$BUILDING_LAYER_ID has unsupported type $buildingLayer")
                )
                null
            }
        }
    }

    private fun heightExpression(): Expression =
        interpolate(
            linear(), zoom(),
            literal(15.0), literal(0),
            literal(15.05), get("height")
        )

    private companion object {
        private val TAG = Tag("MapboxBuildingHighlightApi")

        private const val HIGHLIGHT_BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        private const val EXTRUDE_BUILDING_LAYER_ID = "mapbox-building-extrude-layer"
        private const val BUILDING_LAYER_ID = "building"
        private const val COMPOSITE_SOURCE_ID = "composite"
    }
}
