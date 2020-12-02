package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.ui.base.MapboxState

/**
 *
 */
sealed class RouteLineState : MapboxState {

    /**
     *
     * @param primaryRouteSource
     * @param altRoutesSource
     * @param waypointsSource
     */
    class ClearRouteDataState(
        private val primaryRouteSource: FeatureCollection,
        private val altRoutesSource: FeatureCollection,
        private val waypointsSource: FeatureCollection
    ) : RouteLineState() {

        /**
         *
         */
        fun getPrimaryRouteSource(): FeatureCollection = primaryRouteSource

        /**
         *
         */
        fun getAlternateRoutesSource(): FeatureCollection = altRoutesSource

        /**
         *
         */
        fun getWaypointsSource(): FeatureCollection = waypointsSource
    }

    /**
     *
     */
    sealed class TraveledRouteLineUpdateState : RouteLineState() {

        /**
         *
         * @param trafficLineExp
         * @param routeLineExp
         * @param casingLineEx
         */
        class TraveledRouteLineUpdate(
            private val trafficLineExp: Expression,
            private val routeLineExp: Expression,
            private val casingLineEx: Expression
        ) : TraveledRouteLineUpdateState() {

            /**
             *
             */
            fun getTrafficExpression(): Expression = trafficLineExp

            /**
             *
             */
            fun getRouteLineExpression(): Expression = routeLineExp

            /**
             *
             */
            fun getCasingLineExpression(): Expression = casingLineEx
        }

        /**
         *
         */
        class TraveledRouteLineNoUpdate() : TraveledRouteLineUpdateState()
    }

    /**
     *
     */
    class UnitState : RouteLineState()

    /**
     *
     * @param primaryRouteSource
     * @param trafficLineExp
     * @param routeLineExp
     * @param casingLineEx
     * @param altRoutesSource
     * @param waypointsSource
     */
    class DrawRouteState(
        private val primaryRouteSource: FeatureCollection,
        private val trafficLineExp: Expression,
        private val routeLineExp: Expression,
        private val casingLineEx: Expression,
        private val altRoutesSource: FeatureCollection,
        private val waypointsSource: FeatureCollection
    ) : RouteLineState() {

        /**
         *
         */
        fun getPrimaryRouteSource(): FeatureCollection = primaryRouteSource

        /**
         *
         */
        fun getTrafficLineExpression(): Expression = trafficLineExp

        /**
         *
         */
        fun getWaypointsSource(): FeatureCollection = waypointsSource

        /**
         *
         */
        fun getRouteLineExpression(): Expression = routeLineExp

        /**
         *
         */
        fun getCasingLineExpression(): Expression = casingLineEx

        /**
         *
         */
        fun getAlternateRoutesSource(): FeatureCollection = altRoutesSource
    }

    /**
     *
     */
    sealed class RouteProgressChangeState : RouteLineState() {

        /**
         *
         * @param primaryRouteSource
         * @param altRoutesSource
         * @param waypointsSource
         * @param trafficLineExp
         * @param routeLineExp
         * @param casingLineEx
         */
        class ReInitializeRouteLineState(
            private val primaryRouteSource: FeatureCollection,
            private val altRoutesSource: FeatureCollection,
            private val waypointsSource: FeatureCollection,
            private val trafficLineExp: Expression,
            private val routeLineExp: Expression,
            private val casingLineEx: Expression
        ) : RouteProgressChangeState() {

            /**
             *
             */
            fun getPrimaryRouteSource(): FeatureCollection = primaryRouteSource

            /**
             *
             */
            fun getTrafficLineExpression(): Expression = trafficLineExp

            /**
             *
             */
            fun getWaypointsSource(): FeatureCollection = waypointsSource

            /**
             *
             */
            fun getRouteLineExpression(): Expression = routeLineExp

            /**
             *
             */
            fun getCasingLineExpression(): Expression = casingLineEx

            /**
             *
             */
            fun getAlternateRoutesSource(): FeatureCollection = altRoutesSource
        }

        /**
         *
         * @param drawRouteState
         */
        class RedrawRouteState(
            private val drawRouteState: DrawRouteState
        ) : RouteProgressChangeState() {

            /**
             *
             */
            fun getDrawRouteState(): DrawRouteState = drawRouteState
        }

        /**
         *
         */
        class RouteProgressUpdatedState() : RouteProgressChangeState()
    }

    /**
     *
     * @param layerVisibilityModifications
     */
    class UpdateLayerVisibilityState(
        private val layerVisibilityModifications: List<Pair<String, Visibility>>
    ) : RouteLineState() {

        /**
         *
         */
        fun getLayerVisibilityChanges(): List<Pair<String, Visibility>> =
            layerVisibilityModifications
    }

    /**
     *
     * @param route
     */
    class PrimaryRouteState(private val route: DirectionsRoute?) : RouteLineState() {

        /**
         *
         */
        fun getPrimaryRoute(): DirectionsRoute? = route
    }

    /**
     *
     * @param state
     */
    class UpdateVanishingPointState(private val state: VanishingPointState) : RouteLineState() {

        /**
         *
         */
        fun getVanishingPointState(): VanishingPointState = state
    }

    /**
     *
     * @param style
     */
    class UpdateViewStyleState(private val style: Style) : RouteLineState() {

        /**
         *
         */
        fun getStyle() = style
    }
}
