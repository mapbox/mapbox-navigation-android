package com.mapbox.navigation.ui.androidauto.preview

import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.navigation.ui.androidauto.internal.extensions.getStyle
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources

/**
 * Defines how [CarRouteLineRenderer] builds the route line and upcoming-maneuver arrow
 * rendering components.
 *
 * Each option is a factory rather than a fixed instance, given the [MapboxCarMapSurface], because
 * [MapboxRouteLineViewOptions] and [RouteArrowOptions] both need a live `CarContext` and/or
 * `Style` to build, and those are only available once the car map surface has attached.
 *
 * [MapboxCarMapSurface] does not carry the loaded style directly; read it through
 * `mapboxCarMapSurface.mapSurface.mapboxMap.style`, which returns `null` until the style has
 * finished loading. [CarRouteLineRenderer] only invokes these providers once its own style
 * observation has already seen the style load, so in practice the style is present by the time a
 * provider runs; a provider should still treat `null` defensively (for example by falling back to
 * a sensible default) rather than assuming the non-null contract another caller might not honor.
 *
 * @see CarRouteLineRenderer
 */
class CarRouteLineRendererOptions private constructor(
    /**
     * Builds the [MapboxRouteLineApiOptions] used to calculate the route line, given the attached
     * [MapboxCarMapSurface]. Defaults to a vanishing route line.
     */
    val routeLineApiOptionsProvider: (MapboxCarMapSurface) -> MapboxRouteLineApiOptions,
    /**
     * Builds the [MapboxRouteLineViewOptions] used to draw the route line, given the attached
     * [MapboxCarMapSurface]. Use this to customize the route line appearance, including its
     * [RouteLineColorResources].
     */
    val routeLineViewOptionsProvider: (MapboxCarMapSurface) -> MapboxRouteLineViewOptions,
    /**
     * Builds the [MapboxRouteArrowApi] used to calculate the upcoming maneuver arrow, given the
     * attached [MapboxCarMapSurface].
     */
    val routeArrowApiProvider: (MapboxCarMapSurface) -> MapboxRouteArrowApi,
    /**
     * Builds the [RouteArrowOptions] used to draw the upcoming maneuver arrow, given the attached
     * [MapboxCarMapSurface]. Use this to customize the arrow icon pack (for example
     * [RouteArrowOptions.Builder.withArrowHeadIconDrawable] and
     * [RouteArrowOptions.Builder.withArrowHeadIconCasingDrawable]) and its colors (for example
     * [RouteArrowOptions.Builder.withArrowColor] and
     * [RouteArrowOptions.Builder.withArrowCasingColor]).
     */
    val routeArrowOptionsProvider: (MapboxCarMapSurface) -> RouteArrowOptions,
) {

    /**
     * Builds [CarRouteLineRendererOptions] instances.
     */
    class Builder {
        private var routeLineApiOptionsProvider:
            (MapboxCarMapSurface) -> MapboxRouteLineApiOptions =
                DEFAULT_ROUTE_LINE_API_OPTIONS_PROVIDER
        private var routeLineViewOptionsProvider:
            (MapboxCarMapSurface) -> MapboxRouteLineViewOptions =
                DEFAULT_ROUTE_LINE_VIEW_OPTIONS_PROVIDER
        private var routeArrowApiProvider: (MapboxCarMapSurface) -> MapboxRouteArrowApi =
            DEFAULT_ROUTE_ARROW_API_PROVIDER
        private var routeArrowOptionsProvider: (MapboxCarMapSurface) -> RouteArrowOptions =
            DEFAULT_ROUTE_ARROW_OPTIONS_PROVIDER

        /**
         * @see CarRouteLineRendererOptions.routeLineApiOptionsProvider
         */
        fun routeLineApiOptionsProvider(
            provider: (MapboxCarMapSurface) -> MapboxRouteLineApiOptions,
        ) = apply {
            this.routeLineApiOptionsProvider = provider
        }

        /**
         * @see CarRouteLineRendererOptions.routeLineViewOptionsProvider
         */
        fun routeLineViewOptionsProvider(
            provider: (MapboxCarMapSurface) -> MapboxRouteLineViewOptions,
        ) = apply {
            this.routeLineViewOptionsProvider = provider
        }

        /**
         * @see CarRouteLineRendererOptions.routeArrowApiProvider
         */
        fun routeArrowApiProvider(
            provider: (MapboxCarMapSurface) -> MapboxRouteArrowApi,
        ) = apply {
            this.routeArrowApiProvider = provider
        }

        /**
         * @see CarRouteLineRendererOptions.routeArrowOptionsProvider
         */
        fun routeArrowOptionsProvider(
            provider: (MapboxCarMapSurface) -> RouteArrowOptions,
        ) = apply {
            this.routeArrowOptionsProvider = provider
        }

        /**
         * Build the [CarRouteLineRendererOptions].
         */
        fun build() = CarRouteLineRendererOptions(
            routeLineApiOptionsProvider = routeLineApiOptionsProvider,
            routeLineViewOptionsProvider = routeLineViewOptionsProvider,
            routeArrowApiProvider = routeArrowApiProvider,
            routeArrowOptionsProvider = routeArrowOptionsProvider,
        )

        // internal (rather than private) so CarRouteLineRendererOptionsTest can exercise the
        // road-label lookup and its null-style fallback directly, without needing a real
        // CarContext to build the full MapboxRouteLineViewOptions the provider returns.
        internal companion object {

            val DEFAULT_ROUTE_LINE_API_OPTIONS_PROVIDER:
                (MapboxCarMapSurface) -> MapboxRouteLineApiOptions = {
                    MapboxRouteLineApiOptions.Builder()
                        .vanishingRouteLineEnabled(true)
                        .build()
                }

            val DEFAULT_ROUTE_LINE_VIEW_OPTIONS_PROVIDER:
                (MapboxCarMapSurface) -> MapboxRouteLineViewOptions = { mapboxCarMapSurface ->
                    MapboxRouteLineViewOptions.Builder(mapboxCarMapSurface.carContext)
                        .routeLineColorResources(RouteLineColorResources.Builder().build())
                        .routeLineBelowLayerId(
                            findRoadLabelsLayerId(mapboxCarMapSurface.getStyle()),
                        )
                        .build()
                }

            val DEFAULT_ROUTE_ARROW_API_PROVIDER: (MapboxCarMapSurface) -> MapboxRouteArrowApi =
                {
                    MapboxRouteArrowApi()
                }

            val DEFAULT_ROUTE_ARROW_OPTIONS_PROVIDER:
                (MapboxCarMapSurface) -> RouteArrowOptions = { mapboxCarMapSurface ->
                    RouteArrowOptions.Builder(mapboxCarMapSurface.carContext)
                        .withAboveLayerId(TOP_LEVEL_ROUTE_LINE_LAYER_ID)
                        .build()
                }

            internal fun findRoadLabelsLayerId(style: Style?): String {
                return style?.styleLayers
                    ?.firstOrNull { layer -> layer.id.contains("road-label") }
                    ?.id ?: "road-label-navigation"
            }
        }
    }
}
