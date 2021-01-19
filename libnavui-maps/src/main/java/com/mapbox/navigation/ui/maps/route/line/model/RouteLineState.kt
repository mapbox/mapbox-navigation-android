package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.MapboxState
import com.mapbox.navigation.ui.base.UIMode

/**
 * Represents side effects which can be rendered to change the appearance of the routes displayed
 * on the map.
 */
sealed class RouteLineState : MapboxState {
    /**
     * A state representing the side effects for updating the route visibility.
     *
     * @param layerVisibilityModifications a collection of visibility modifications
     * @param uiMode a value indicating a light or dark UI mode
     */
    class UpdateLayerVisibilityState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>,
        private val uiMode: UIMode
    ) : RouteLineState() {
        /**
         * @return a collection of visibility modifications
         */
        fun getLayerVisibilityChanges(): List<Pair<String, Visibility>> =
            layerVisibilityModifications

        /**
         * @return a value indicating a light or dark UI mode
         */
        fun getUIMode(): UIMode = uiMode
    }

    // todo docs
    class UpdateColorPropertiesState(
        private val colorUpdates: List<Pair<String, List<Expression>>>,
        private val uiMode: UIMode
    ) : RouteLineState() {

        /**
         * @return a list of color updates consisting of pairs of layer Ids and [Expression] objects
         */
        fun getColorUpdates() = colorUpdates

        /**
         * @return a value indicating a light or dark UI mode
         */
        fun getUIMode(): UIMode = uiMode
    }

    /**
     * Represents the index of a route found by searching for the nearest route to to a map
     * click point. The index corresponds to the MapboxRouteArrowApi's collection of routes.
     *
     * @param routeIndex the index of the route in the collection
     */
    class ClosestRouteState(private val routeIndex: Int) : RouteLineState() {
        /**
         * @return the route index
         */
        fun getRouteIndex() = routeIndex
    }

    /**
     * Represents the side effects for drawing routes on a map.
     *
     * @param primaryRouteSource the feature collection for the primary route line
     * @param trafficLineExpression the expression for the primary route traffic line
     * @param routeLineExpression the expression for the primary route line
     * @param casingLineExpression the expression for the primary route casing line
     * @param altRoute1TrafficExpression the expression for an alternative route traffic line
     * @param altRoute2TrafficExpression the expression for an alternative route traffic line
     * @param altRoute1Source the feature collection for an alternative route line
     * @param altRoute2Source the feature collection for an alternative route line
     * @param waypointsSource the feature collection for the origin and destination icons
     * * @param uiMode a value indicating a light or dark UI mode
     */
    class RouteSetState(
        private val primaryRouteSource: FeatureCollection,
        private val trafficLineExpression: Expression,
        private val routeLineExpression: Expression,
        private val casingLineExpression: Expression,
        private val altRoute1TrafficExpression: Expression,
        private val altRoute2TrafficExpression: Expression,
        private val altRoute1Source: FeatureCollection,
        private val altRoute2Source: FeatureCollection,
        private val waypointsSource: FeatureCollection,
        private val uiMode: UIMode
    ) : RouteLineState() {
        /**
         * @return the feature collection for the primary route
         */
        fun getPrimaryRouteSource(): FeatureCollection = primaryRouteSource

        /**
         * @return the expression for the primary route traffic line
         */
        fun getTrafficLineExpression(): Expression = trafficLineExpression

        /**
         * @return the expression for the primary route line
         */
        fun getRouteLineExpression(): Expression = routeLineExpression
        /**
         * @return the expression for the primary route casing line
         */
        fun getCasingLineExpression(): Expression = casingLineExpression

        /**
         * @return the expression for an alternative route line
         */
        fun getAlternativeRoute1TrafficExpression(): Expression = altRoute1TrafficExpression

        /**
         * @return the expression for an alternative route line
         */
        fun getAlternativeRoute2TrafficExpression(): Expression = altRoute2TrafficExpression

        /**
         * @return the feature collection for an alternative route line
         */
        fun getAlternativeRoute1Source(): FeatureCollection = altRoute1Source
        /**
         * @return the feature collection for an alternative route line
         */
        fun getAlternativeRoute2Source(): FeatureCollection = altRoute2Source

        /**
         * @return a feature collection for the origin and destination icons
         */
        fun getOriginAndDestinationPointsSource(): FeatureCollection = waypointsSource

        /**
         * @return a value indicating a light or dark UI mode
         */
        fun getUIMode(): UIMode = uiMode
    }

    /**
     * Represents data for updating the appearance of the route line.
     *
     * @param trafficLineExpression the expression for the primary route traffic line
     * @param routeLineExpression the expression for the primary route line
     * @param casingLineExpression the expression for the primary route casing line
     * @param uiMode a value indicating a light or dark UI mode
     */
    class VanishingRouteLineUpdateState(
        private val trafficLineExpression: Expression,
        private val routeLineExpression: Expression,
        private val casingLineExpression: Expression,
        private val uiMode: UIMode
    ) : RouteLineState() {
        /**
         * @return the expression for the primary route traffic line
         */
        fun getTrafficLineExpression(): Expression = trafficLineExpression
        /**
         * @return the expression for the primary route line
         */
        fun getRouteLineExpression(): Expression = routeLineExpression
        /**
         * @return the expression for the primary route casing line
         */
        fun getCasingLineExpression(): Expression = casingLineExpression

        /**
         * @return a value indicating a light or dark UI mode
         */
        fun getUIMode(): UIMode = uiMode
    }

    /**
     * Represents data used to remove the route line(s) from the map.
     *
     * @param primaryRouteSource a feature collection representing the primary route
     * @param altRoute1Source a feature collection representing an alternative route
     * @param altRoute2Source a feature collection representing an alternative route
     * @param waypointsSource a feature collection representing the origin and destination icons
     * @param uiMode a value indicating a light or dark UI mode
     */
    class ClearRouteLineState(
        private val primaryRouteSource: FeatureCollection,
        private val altRoute1Source: FeatureCollection,
        private val altRoute2Source: FeatureCollection,
        private val waypointsSource: FeatureCollection,
        private val uiMode: UIMode
    ) : RouteLineState() {
        /**
         * @return the primary route feature collection
         */
        fun getPrimaryRouteSource(): FeatureCollection = primaryRouteSource
        /**
         * @return an alternative route feature collection
         */
        fun getAlternativeRoute1Source(): FeatureCollection = altRoute1Source
        /**
         * @return an alternative route feature collection
         */
        fun getAlternativeRoute2Source(): FeatureCollection = altRoute2Source
        /**
         * @return a feature collection for displaying the origin and destination points
         */
        fun getOriginAndDestinationPointsSource(): FeatureCollection = waypointsSource

        /**
         * @return a value indicating a light or dark UI mode
         */
        fun getUIMode(): UIMode = uiMode
    }
}
