package com.mapbox.navigation.ui.maps.route.arrow.model

import com.mapbox.geojson.Feature
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.MapboxState

/**
 * A state object used for rendering maneuver arrow side effects.
 */
sealed class RouteArrowState : MapboxState {

    /**
     * A state object representing visibility side effects for rendering.
     */
    class UpdateRouteArrowVisibilityState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>
    ) : RouteArrowState() {
        /**
         * @return visibility modifications to be rendered
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications
    }

    /**
     * A state object representing an update to the maneuver arrow position and/or appearance.
     */
    class UpdateManeuverArrowState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>,
        private val arrowShaftFeature: Feature?,
        private val arrowHeadFeature: Feature?
    ) : RouteArrowState() {
        /**
         * @return visibility modifications to be rendered
         */
        fun getVisibilityChanges(): List<Pair<String, Visibility>> = layerVisibilityModifications

        /**
         * @return a map feature representing the arrow head or null
         */
        fun getArrowHeadFeature(): Feature? = arrowHeadFeature

        /**
         * @return a map feature representing the arrow shaft or null
         */
        fun getArrowShaftFeature(): Feature? = arrowShaftFeature
    }
}
