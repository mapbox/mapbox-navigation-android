package com.mapbox.navigation.ui.map.building

import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.Property.NONE
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import com.mapbox.navigation.ui.internal.utils.MapUtils
import java.lang.ref.WeakReference

/**
 * This layer handles the creation and customization of a [FillExtrusionLayer]
 * to show 3D buildings.
 */
class BuildingExtrusionHighlightLayer(private val mapboxMap: MapboxMap) {

    /**
     * This [LatLng] is used to determine what associated extrusion
     * should be highlighted.
     */
    var queryLatLng: LatLng? = null
        /**
         * Set the [LatLng] location of the building extrusion highlight layer.
         * The [LatLng] passed through this method is used to see whether its within the
         * footprint of a specific building. If so, that building's footprint is used for the 3D
         * highlighted extrusion.
         *
         * @param queryLatLng the new coordinates to use in querying the building layer
         * to get the associated [Polygon] to eventually highlight.
         */
        set(value) {
            field = value
            mapboxMap.getStyle { stl ->
                value?.let { newLatLng ->
                    val buildingExtrusionLayer =
                        stl
                            .getLayerAs<FillExtrusionLayer>(HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID)
                    buildingExtrusionLayer?.setFilter(
                        buildingLayerSupport.getBuildingFilterExpression(
                            buildingLayerSupport.getBuildingId(mapboxMap, newLatLng)
                        )
                    )
                }
                val k = WeakReference<Any>(Any())
                k.get()
            }
        }

    /**
     * Retrieve the latest set color of the highlighted extrusion layer.
     *
     * @return the color Integer
     */
    var color = BuildingLayerSupport.DEFAULT_HIGHLIGHT_COLOR
        /**
         * Set the color of the highlighted extrusion layer.
         *
         * @param newColor the new color value
         */
        set(value) {
            field = value
            buildingLayerSupport.updateLayerProperty(
                fillExtrusionColor(value),
                mapboxMap,
                HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID
            )
        }

    /**
     * Retrieve the latest set opacity of the highlighted extrusion layer.
     *
     * @return the opacity Float
     */
    var opacity = BuildingLayerSupport.DEFAULT_HIGHLIGHT_OPACITY
        /**
         * Set the opacity of the highlighted extrusion layer.
         *
         * @param newOpacity the new opacity value
         */
        set(value) {
            field = value
            buildingLayerSupport.updateLayerProperty(
                fillExtrusionOpacity(value),
                mapboxMap,
                HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID
            )
        }

    /**
     * Toggles the visibility of the highlighted extrusion layer.
     *
     * @param visible true if the layer should be displayed. False if it should be hidden.
     */
    fun updateVisibility(visible: Boolean) {
        mapboxMap.getStyle { stl ->
            if (visible &&
                stl.getLayerAs<FillExtrusionLayer>(HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID) == null
            ) {
                addHighlightExtrusionLayerToMap(queryLatLng)
            } else {
                buildingLayerSupport.updateLayerProperty(
                    visibility(
                        if (visible) VISIBLE else NONE
                    ),
                    mapboxMap,
                    HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID
                )
            }
        }
    }

    /**
     * Customize and add a [FillExtrusionLayer] to the map to show a
     * highlighted building extrusion.
     */
    private fun addHighlightExtrusionLayerToMap(queryLatLng: LatLng?) {
        mapboxMap.getStyle { style ->
            val fillExtrusionLayer = FillExtrusionLayer(
                HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID,
                BuildingLayerSupport.COMPOSITE_SOURCE_ID
            )
            fillExtrusionLayer.apply {
                sourceLayer = BuildingLayerSupport.BUILDING_LAYER_ID
                queryLatLng?.let {
                    setFilter(
                        buildingLayerSupport.getBuildingFilterExpression(
                            buildingLayerSupport.getBuildingId(mapboxMap, it)
                        )
                    )
                }
                withProperties(
                    fillExtrusionColor(color),
                    fillExtrusionOpacity(opacity),
                    fillExtrusionHeight(get("height"))
                )
            }
            MapUtils.addLayerToMap(style, fillExtrusionLayer, null)
        }
    }

    companion object {

        private val buildingLayerSupport = BuildingLayerSupport()

        /**
         * A constant String that serves as a layer id for the [FillExtrusionLayer] that
         * this class adds to the [MapboxMap]'s Style object.
         */
        const val HIGHLIGHTED_BUILDING_EXTRUSION_LAYER_ID =
            "highlighted-building-extrusion-layer-id"
    }
}
