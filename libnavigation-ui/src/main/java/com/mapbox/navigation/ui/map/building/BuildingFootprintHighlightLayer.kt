package com.mapbox.navigation.ui.map.building

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.navigation.ui.internal.utils.MapUtils

/**
 * This layer handles the creation and customization of a [FillLayer]
 * to highlight the footprint of an individual building. For now, this layer is only
 * compatible with the Mapbox Streets v7 vector tile source because that's what the default
 * Navigation UI SDK styles use.
 */
class BuildingFootprintHighlightLayer(private val mapboxMap: MapboxMap) {

    /**
     * This [LatLng] is used to determine what associated footprint
     * should be highlighted.
     */
    var queryLatLng: LatLng? = null
        /**
         * Set the [LatLng] location of the building footprint highlight layer. The [LatLng] passed
         * through this method is used to see whether its within the footprint of a specific
         * building. If so, that building's footprint is used for a 2D highlighted footprint.
         *
         * @param queryLatLng the new coordinates to use in querying the building layer
         * to get the associated [Polygon] to eventually highlight.
         */
        set(value) {
            field = value
            value?.let { newLatLng ->
                mapboxMap.getStyle { style ->
                    val buildingFootprintLayer = style.getLayerAs<FillLayer>(HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID)
                    buildingFootprintLayer?.setFilter(
                            buildingLayerSupport.getBuildingFilterExpression(newLatLng,
                                    buildingLayerSupport.getBuildingId(mapboxMap, queryLatLng)))
                }
            }
        }

    /**
     * Retrieve the latest set color of the building footprint highlight layer.
     *
     * @return the color Integer
     */
    var color = BuildingLayerSupport.DEFAULT_HIGHLIGHT_COLOR
        /**
         * Set the color of the building footprint highlight layer.
         *
         * @param newColor the new color value
         */
        set(value) {
            field = value
            buildingLayerSupport.updateLayerProperty(fillColor(value), mapboxMap, HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID)
        }

    /**
     * Retrieve the latest set opacity of the building footprint highlight layer.
     *
     * @return the opacity Float
     */
    var opacity = BuildingLayerSupport.DEFAULT_HIGHLIGHT_OPACITY
        /**
         * Set the opacity of the building footprint highlight layer.
         *
         * @param newOpacity the new opacity value
         */
        set(value) {
            field = value
            buildingLayerSupport.updateLayerProperty(fillOpacity(value), mapboxMap, HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID)
        }

    private val buildingLayerSupport = BuildingLayerSupport()

    /**
     * Toggles the visibility of the building footprint highlight layer.
     *
     * @param visible true if the layer should be displayed. False if it should be hidden.
     */
    fun updateVisibility(visible: Boolean) {
        mapboxMap.getStyle { style ->
            val buildingFootprintFillLayer = style.getLayerAs<FillLayer>(HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID)
            if (buildingFootprintFillLayer == null && visible) {
                addFootprintHighlightFillLayerToMap(queryLatLng)
            } else buildingLayerSupport.updateLayerProperty(visibility(
                    if (visible) VISIBLE else NONE), mapboxMap, HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID)
        }
    }

    /**
     * Customize and add a [FillLayer] to the map to show a highlighted
     * building footprint.
     */
    private fun addFootprintHighlightFillLayerToMap(queryLatLng: LatLng?) {
        mapboxMap.getStyle { style ->
            val buildingFootprintFillLayer = FillLayer(
                    HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID, BuildingLayerSupport.COMPOSITE_SOURCE_ID)
            buildingFootprintFillLayer.apply {
                sourceLayer = BuildingLayerSupport.BUILDING_LAYER_ID
                queryLatLng?.let {
                    setFilter(buildingLayerSupport.getBuildingFilterExpression(it,
                            buildingLayerSupport.getBuildingId(mapboxMap, queryLatLng)))
                }
                withProperties(
                        fillColor(color),
                        fillOpacity(opacity)
                )
                MapUtils.addLayerToMap(style, this, BuildingLayerSupport.BUILDING_LAYER_ID)
            }
        }
    }

    companion object {

        /**
         * A constant String that serves as a layer id for the [FillLayer] that
         * this class adds to the [MapboxMap]'s Style object.
         */
        const val HIGHLIGHTED_BUILDING_FOOTPRINT_LAYER_ID = "highlighted-building-footprint-layer-id"
    }
}
