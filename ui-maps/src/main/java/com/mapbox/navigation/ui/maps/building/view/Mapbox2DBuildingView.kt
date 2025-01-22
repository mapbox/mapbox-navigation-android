package com.mapbox.navigation.ui.maps.building.view

import com.mapbox.maps.QueriedRenderedFeature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions

internal class Mapbox2DBuildingView : BuildingView {

    /**
     * Access the expression used to create the building's height.
     */
    private val buildingHeightExpression: Expression =
        Expression.interpolate(
            Expression.linear(),
            Expression.zoom(),
            literal(15.0),
            literal(0),
            literal(15.05),
            Expression.get(HEIGHT),
        )

    override fun highlightBuilding(
        style: Style,
        buildings: List<QueriedRenderedFeature>,
        options: MapboxBuildingHighlightOptions,
    ) {
        updateBuildingLayer(style, buildings, options)
    }

    override fun removeBuildingHighlight(style: Style, options: MapboxBuildingHighlightOptions) {
        updateBuildingLayer(style, emptyList(), options)
    }

    override fun clear(style: Style) {
        style.removeStyleLayer(HIGHLIGHT_BUILDING_LAYER_ID)
    }

    private fun updateBuildingLayer(
        style: Style,
        buildings: List<QueriedRenderedFeature>,
        options: MapboxBuildingHighlightOptions,
    ) {
        val ids = buildings.mapNotNull { it.queriedFeature.feature.id()?.toLong() }
        val selectedBuilding = Expression.inExpression(Expression.id(), literal(ids))
        if (!style.styleLayerExists(HIGHLIGHT_BUILDING_LAYER_ID)) {
            style.addPersistentLayer(
                FillExtrusionLayer(HIGHLIGHT_BUILDING_LAYER_ID, COMPOSITE_SOURCE_ID)
                    .sourceLayer(BUILDING_LAYER_ID)
                    .filter(selectedBuilding)
                    .fillExtrusionColor(options.fillExtrusionColor)
                    .fillExtrusionOpacity(options.fillExtrusionOpacity)
                    .fillExtrusionBase(Expression.get(MIN_HEIGHT))
                    .fillExtrusionHeight(buildingHeightExpression),
            )
        } else {
            style
                .getLayerAs<FillExtrusionLayer>(HIGHLIGHT_BUILDING_LAYER_ID)
                ?.filter(selectedBuilding)
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
