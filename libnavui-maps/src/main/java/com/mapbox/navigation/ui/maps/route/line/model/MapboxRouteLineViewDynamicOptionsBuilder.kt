package com.mapbox.navigation.ui.maps.route.line.model

import androidx.annotation.FloatRange
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.model.FadingConfig

/**
 * A convenience typealias for a lambda acting on a [MapboxRouteLineViewDynamicOptionsBuilder].
 */
typealias MapboxRouteLineViewDynamicOptionsBuilderBlock =
    MapboxRouteLineViewDynamicOptionsBuilder.() -> Unit

/**
 * Class that allows you to modify a subset of [MapboxRouteLineViewOptions] at runtime
 * and set it via [MapboxRouteLineView.updateDynamicOptions] without recreating the instance.
 * Note that just updating the options in [MapboxRouteLineView] will not re-render the route line.
 * In order to do that, invoke:
 * ```
 * mapboxRouteLineView.updateDynamicOptions(style, builderBlock)
 * mapboxRouteLineApi.getRouteDrawData {
 *     mapboxRouteLineView.renderRouteDrawData(style, it)
 * }
 * ```
 */
class MapboxRouteLineViewDynamicOptionsBuilder internal constructor(
    private val builder: MapboxRouteLineViewOptions.Builder,
) {

    /**
     * An instance of [RouteLineColorResources].
     * Contains information about colors used for route line.
     *
     * @param routeLineColorResources an instance of [RouteLineColorResources].
     * @return the same builder
     */
    fun routeLineColorResources(
        routeLineColorResources: RouteLineColorResources,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.routeLineColorResources(routeLineColorResources)
    }

    /**
     * An instance of [RouteLineScaleExpressions].
     * Contains information about custom scaling expressions.
     *
     * @param scaleExpressions an instance of [RouteLineScaleExpressions].
     * @return the same builder
     */
    fun scaleExpressions(
        scaleExpressions: RouteLineScaleExpressions,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.scaleExpressions(scaleExpressions)
    }

    /**
     * Determines if the color transition between traffic congestion changes should use a
     * soft gradient appearance or abrupt color change. This is false by default.
     *
     * @param displaySoftGradientForTraffic whether soft gradient transition should be enabled
     * @return the builder
     */
    fun displaySoftGradientForTraffic(
        displaySoftGradientForTraffic: Boolean,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.displaySoftGradientForTraffic(displaySoftGradientForTraffic)
    }

    /**
     * Influences the length of the color transition when the displaySoftGradientForTraffic
     * parameter is true.
     *
     * @param softGradientTransition transition
     * @return the builder
     */
    fun softGradientTransition(
        softGradientTransition: Double,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.softGradientTransition(softGradientTransition)
    }

    /**
     * Determines the elevation of the route layers. Note that if you are using Mapbox Standard style,
     * you can only specify a layer id that is added at runtime: static layer ids from the style will not be applied.
     *
     * @param layerId layer id below which the route line layers will be placed
     * @return the builder
     */
    @Deprecated("Use slot name")
    fun routeLineBelowLayerId(layerId: String?): MapboxRouteLineViewDynamicOptionsBuilder =
        apply { builder.routeLineBelowLayerId(layerId) }

    /**
     * Determines the position of the route layers in the map style.
     * Adding a slot to the style can be done at runtime with code like:
     *
     * ```
     * style.addPersistentLayer(slotLayer("runtimeSlotId") {
     *   slot("middle")
     * })
     * ```
     * The slot layer needs to be added persistently, because so are the route line layers, and they
     * require the slot reference immediately on style reload to be correctly recreated.
     *
     * See maps SDK for more information.
     * https://docs.mapbox.com/style-spec/reference/slots
     * https://docs.mapbox.com/help/tutorials/aa-standard-in-studio/#place-layer-in-a-slot
     *
     * @param name the name of the slot to use for the route line
     */
    fun slotName(name: String): MapboxRouteLineViewDynamicOptionsBuilder =
        apply { builder.slotName(name) }

    /**
     * Factor that decreases line layer opacity based on occlusion from 3D objects.
     * Value 0 disables occlusion, value 1 means fully occluded.
     *
     * @param lineDepthOcclusionFactor occlusion factor
     * @return the builder
     */
    fun lineDepthOcclusionFactor(
        @FloatRange(from = 0.0, to = 1.0) lineDepthOcclusionFactor: Double,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.lineDepthOcclusionFactor(lineDepthOcclusionFactor)
    }

    /**
     * Configuration for fading out of the route line. See [FadingConfig] for details.
     * If not set or set it null, the route line will be fully opaque at all zoom levels.
     *
     * @param fadingConfig [FadingConfig]
     * @return the builder
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun fadingConfig(
        fadingConfig: FadingConfig?,
    ): MapboxRouteLineViewDynamicOptionsBuilder = apply {
        builder.fadeOnHighZoomsConfig(fadingConfig)
    }

    internal fun build(): MapboxRouteLineViewOptions = builder.build()
}
