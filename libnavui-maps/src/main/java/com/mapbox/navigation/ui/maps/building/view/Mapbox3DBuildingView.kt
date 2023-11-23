package com.mapbox.navigation.ui.maps.building.view

import androidx.annotation.UiThread
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions
import java.util.concurrent.ConcurrentHashMap

@UiThread
internal class Mapbox3DBuildingView : BuildingView {

    private val originalFilters = ConcurrentHashMap<Style, Expression>()

    override fun highlightBuilding(
        style: Style,
        buildings: List<QueriedFeature>,
        options: MapboxBuildingHighlightOptions
    ) {
        updateBuildingLayer(style, buildings, options)
    }

    override fun removeBuildingHighlight(style: Style, options: MapboxBuildingHighlightOptions) {
        updateBuildingLayer(style, emptyList(), options)
    }

    override fun clear(style: Style) {
        style.removeStyleLayer(HIGHLIGHT_BUILDING_LAYER_ID)
        val originalLayer = style.getLayerAs<FillExtrusionLayer>(BUILDING_EXTRUSION_LAYER_ID) ?: return

        originalFilters[style]?.let {
            originalLayer.filter(it)
        }
        originalFilters.remove(style)
    }

    private fun updateBuildingLayer(
        style: Style,
        buildings: List<QueriedFeature>,
        options: MapboxBuildingHighlightOptions
    ) {
        val ids = buildings.mapNotNull { it.feature.id()?.toLong() }
        val selectedBuilding = Expression.inExpression(Expression.id(), literal(ids))

        if (!style.styleLayerExists(HIGHLIGHT_BUILDING_LAYER_ID)) {
            val originalLayer = style.getLayerAs<FillExtrusionLayer>(BUILDING_EXTRUSION_LAYER_ID) ?: return
            val originalLayerProperties = style.getStyleLayerProperties(BUILDING_EXTRUSION_LAYER_ID).value ?: return

            val originalLayerFilter = originalLayer.filter

            val sourceLayer = originalLayer.sourceLayer ?: return

            val fillExtrusionLayer = FillExtrusionLayer(HIGHLIGHT_BUILDING_LAYER_ID, originalLayer.sourceId)
            style.addPersistentLayer(
                fillExtrusionLayer.sourceLayer(sourceLayer),
                LayerPosition(BUILDING_EXTRUSION_LAYER_ID, null, null)
            )

            style.setStyleLayerProperties(HIGHLIGHT_BUILDING_LAYER_ID, originalLayerProperties)

            fillExtrusionLayer
                .filter(selectedBuilding)
                .fillExtrusionColor(options.fillExtrusionColor)
                .fillExtrusionOpacity(options.fillExtrusionOpacity)

            originalLayerFilter?.let {
                originalFilters[style] = it
                // Filter-out buildings from the original layer. All that are not selected + original buildings
                originalLayer.filter(Expression.all(Expression.not(selectedBuilding), it))
            }
        } else {
            style
                .getLayerAs<FillExtrusionLayer>(HIGHLIGHT_BUILDING_LAYER_ID)
                ?.filter(selectedBuilding)

            originalFilters[style]?.let {
                // Remove selected buildings from the original layer
                style
                    .getLayerAs<FillExtrusionLayer>(BUILDING_EXTRUSION_LAYER_ID)
                    ?.filter(Expression.all(Expression.not(selectedBuilding), it))
            }
        }
    }

    private companion object {
        const val HIGHLIGHT_BUILDING_LAYER_ID = "mapbox-building-highlight-layer"
        const val BUILDING_EXTRUSION_LAYER_ID = "building-extrusion"
    }
}
