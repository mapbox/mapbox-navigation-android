package com.mapbox.navigation.ui.maps.building.view

import androidx.annotation.UiThread
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.navigation.ui.maps.building.model.MapboxBuildingHighlightOptions

/**
 * TODO open questions:
 * 1) Is it safe to check if style is 3d by presence of "building-extrusion" layer
 * 2) Do we need to allow customers to choose which layer to modify or that'll always be "building-extrusion" layer
 */

/**
 * Mapbox default view that adds/updates/removes building layer and building highlight
 * for a specific feature geometry and applies appropriate properties to the building layer
 * on top of [MapboxMap] using [MapboxBuildingHighlightOptions]
 *
 * Each [Layer] added to the map by this class is a persistent layer - it will survive style changes.
 * This means that if the data has not changed, it does not have to be manually redrawn after a style change.
 * See [Style.addPersistentStyleLayer].
 */
@UiThread
class MapboxBuildingView {

    private val view2D = Mapbox2DBuildingView()
    private val view3D = Mapbox3DBuildingView()

    // TODO is it correct impl?
    private val Style.is3DStyle: Boolean
        get() = styleLayerExists("building-extrusion")

    private fun viewForStyle(style: Style): BuildingView {
        return if (style.is3DStyle) {
            view3D
        } else {
            view2D
        }
    }

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
        viewForStyle(style).highlightBuilding(style, buildings, options)
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
        viewForStyle(style).removeBuildingHighlight(style, options)
    }

    /**
     * Remove the building layer and source.
     */
    fun clear(style: Style) {
        viewForStyle(style).clear(style)
    }
}
