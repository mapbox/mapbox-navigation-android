package com.mapbox.navigation.ui.maps.building.view

import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions

/**
 * Mapbox default view that adds/updates/removes building layer and building highlight
 * for a specific feature geometry and applies appropriate properties to the building layer
 * on top of [MapboxMap] using [MapboxBuildingHighlightOptions]
 */
class MapboxBuildingView {

    /**
     * Access the expression used to create the building's height.
     */
    private val buildingHeightExpression: Expression =
        Expression.interpolate(
            Expression.linear(), Expression.zoom(),
            literal(15.0), literal(0),
            literal(15.05), Expression.get(HEIGHT)
        )

    /**
     * Highlight all [buildings] obtained by querying the [MapboxMap]. If the list of buildings
     * is empty, none of them are highlighted.
     *
     * @param style a valid [Style] instance
     * @param buildings list of buildings to highlight
     * @param options defines additional options to apply to building layer
     */
    @JvmOverloads
    fun highlightBuilding(
        style: Style,
        buildings: List<QueriedFeature>,
        options: MapboxBuildingHighlightOptions = MapboxBuildingHighlightOptions.Builder().build()
    ) {
        updateBuildingLayer(style, buildings, options)
    }

    /**
     * Remove building highlight from an already highlighted building
     *
     * @param style a valid [Style] instance
     * @param options defines additional options to apply to building layer
     */
    @JvmOverloads
    fun removeBuildingHighlight(
        style: Style,
        options: MapboxBuildingHighlightOptions = MapboxBuildingHighlightOptions.Builder().build()
    ) {
        updateBuildingLayer(style, emptyList(), options)
    }

    /**
     * Remove the building layer and source.
     */
    fun clear(style: Style) {
        style.removeStyleLayer(HIGHLIGHT_BUILDING_LAYER_ID)
    }

    private fun updateBuildingLayer(
        style: Style,
        buildings: List<QueriedFeature>,
        options: MapboxBuildingHighlightOptions
    ) {
        val ids = buildings.mapNotNull { it.feature.id()?.toLong() }
        val selectedBuilding = Expression.inExpression(Expression.id(), literal(ids))
        if (!style.styleLayerExists(HIGHLIGHT_BUILDING_LAYER_ID)) {
            style.addLayer(
                FillExtrusionLayer(HIGHLIGHT_BUILDING_LAYER_ID, COMPOSITE_SOURCE_ID)
                    .sourceLayer(BUILDING_LAYER_ID)
                    .filter(selectedBuilding)
                    .fillExtrusionColor(options.fillExtrusionColor)
                    .fillExtrusionOpacity(options.fillExtrusionOpacity)
                    .fillExtrusionBase(Expression.get(MIN_HEIGHT))
                    .fillExtrusionHeight(buildingHeightExpression)
            )
        } else {
            style.getLayerAs<FillExtrusionLayer>(HIGHLIGHT_BUILDING_LAYER_ID)
                .filter(selectedBuilding)
        }
    }

    private companion object {
        /**
         * Layer_id used for highlighting a building.
         */
        private const val HIGHLIGHT_BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        /**
         * Source_id used for highlighting a building.
         */
        private const val COMPOSITE_SOURCE_ID = "composite"
        /**
         * Source layer that includes building height.
         */
        private const val BUILDING_LAYER_ID = "building"
        private const val MIN_HEIGHT = "min-height"
        private const val HEIGHT = "height"
    }
}
