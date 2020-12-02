package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.Feature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.MapboxState

/**
 *
 */
sealed class RouteArrowState : MapboxState {

    /**
     *
     * @param layerVisibilityModifications
     */
    class UpdateRouteArrowVisibilityState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>
    ) : RouteArrowState() {

        /**
         *
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications
    }

    /**
     *
     * @param layerVisibilityModifications
     * @param arrowShaftFeature
     * @param arrowHeadFeature
     */
    class UpdateManeuverArrowState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>,
        private val arrowShaftFeature: Feature?,
        private val arrowHeadFeature: Feature?
    ) : RouteArrowState() {

        /**
         *
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications

        /**
         *
         */
        fun getArrowHeadFeature(): Feature? = arrowHeadFeature

        /**
         *
         */
        fun getArrowShaftFeature(): Feature? = arrowShaftFeature
    }

    /**
     *
     * @param style
     */
    class UpdateViewStyleState(private val style: Style) : RouteArrowState() {

        /**
         *
         */
        fun getStyle() = style
    }
}
