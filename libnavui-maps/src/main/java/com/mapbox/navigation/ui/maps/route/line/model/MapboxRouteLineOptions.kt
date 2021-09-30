package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLayerProvider
import com.mapbox.navigation.ui.maps.route.line.api.VanishingRouteLine
import kotlin.math.abs

/**
 * Options for the configuration and appearance of the route line.
 *
 * @param resourceProvider an instance of RouteLineResources
 * @param routeLayerProvider an instance of RouteLayerProvider
 * @param originIcon the drawable for representing the origin icon
 * @param destinationIcon the drawable for representing the destination icon
 * @param routeLineBelowLayerId determines the elevation of the route layers
 * @param vanishingRouteLine an instance of the VanishingRouteLine
 * @param tolerance the tolerance value used when configuring the underlying map source
 * @param displayRestrictedRoadSections indicates if the route line will display restricted
 * road sections with a dashed line
 * @param styleInactiveRouteLegsIndependently enabling this feature will change the color of the route
 * legs that aren't currently being navigated. See [RouteLineColorResources] to specify the color
 * used.
 * @param displaySoftGradientForTraffic determines if the color transition between traffic congestion
 * changes should use a soft gradient appearance or abrupt color change. This is false by default.
 * @param softGradientTransition influences the length of the color transition when the displaySoftGradientForTraffic
 * parameter is true.
 */
class MapboxRouteLineOptions private constructor(
    val resourceProvider: RouteLineResources,
    internal val routeLayerProvider: MapboxRouteLayerProvider,
    val originIcon: Drawable,
    val destinationIcon: Drawable,
    val routeLineBelowLayerId: String?,
    internal var vanishingRouteLine: VanishingRouteLine? = null,
    val tolerance: Double,
    val displayRestrictedRoadSections: Boolean = false,
    val styleInactiveRouteLegsIndependently: Boolean = false,
    val displaySoftGradientForTraffic: Boolean = false,
    val softGradientTransition: Double = RouteLayerConstants.SOFT_GRADIENT_STOP_GAP_METERS
) {

    /**
     * @param context a valid context
     *
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(context: Context): Builder {
        val vanishingRouteLineEnabled = vanishingRouteLine != null
        return Builder(
            context,
            resourceProvider,
            routeLineBelowLayerId,
            routeLayerProvider.routeStyleDescriptors,
            vanishingRouteLineEnabled,
            tolerance,
            displayRestrictedRoadSections,
            styleInactiveRouteLegsIndependently,
            displaySoftGradientForTraffic,
            softGradientTransition
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteLineOptions

        if (resourceProvider != other.resourceProvider) return false
        if (routeLayerProvider != other.routeLayerProvider) return false
        if (originIcon != other.originIcon) return false
        if (destinationIcon != other.destinationIcon) return false
        if (routeLineBelowLayerId != other.routeLineBelowLayerId) return false
        if (vanishingRouteLine != other.vanishingRouteLine) return false
        if (tolerance != other.tolerance) return false
        if (displayRestrictedRoadSections != other.displayRestrictedRoadSections) return false
        if (styleInactiveRouteLegsIndependently != other.styleInactiveRouteLegsIndependently)
            return false
        if (displaySoftGradientForTraffic != other.displayRestrictedRoadSections) return false
        if (softGradientTransition != other.softGradientTransition) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = resourceProvider.hashCode()
        result = 31 * result + routeLayerProvider.hashCode()
        result = 31 * result + originIcon.hashCode()
        result = 31 * result + destinationIcon.hashCode()
        result = 31 * result + routeLineBelowLayerId.hashCode()
        result = 31 * result + vanishingRouteLine.hashCode()
        result = 31 * result + (tolerance.hashCode())
        result = 31 * result + (displayRestrictedRoadSections.hashCode())
        result = 31 * result + (styleInactiveRouteLegsIndependently.hashCode())
        result = 31 * result + (displaySoftGradientForTraffic.hashCode())
        result = 31 * result + (softGradientTransition.hashCode())
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteLineOptions(resourceProvider=$resourceProvider, " +
            "routeLayerProvider=$routeLayerProvider, " +
            "originIcon=$originIcon, " +
            "destinationIcon=$destinationIcon, " +
            "routeLineBelowLayerId=$routeLineBelowLayerId, " +
            "vanishingRouteLine=$vanishingRouteLine, " +
            "tolerance=$tolerance, " +
            "displayRestrictedRoadSections=$displayRestrictedRoadSections, " +
            "styleInactiveRouteLegsIndependently=$styleInactiveRouteLegsIndependently," +
            "displaySoftGradientForTraffic=$displaySoftGradientForTraffic," +
            "softGradientTransition=$softGradientTransition" +
            ")"
    }

    /**
     * Responsible for instantiating an instance of MapboxRouteLineOptions
     *
     * @param context an instance of Context
     * @param routeLineResources an instance of RouteLineResources
     * @param routeLineBelowLayerId determines the elevation of the route layers
     * @param routeStyleDescriptors a collection of RouteStyleDescriptor objects
     * @param vanishingRouteLineEnabled indicates if the vanishing route line feature is enabled
     * @param displayRestrictedRoadSections indicates if the route line will display restricted
     * road sections with a dashed line
     * @param styleInactiveRouteLegsIndependently enabling this feature will change the color of the route
     * legs that aren't currently being navigated. See [RouteLineColorResources] to specify the color
     * used.
     * @param displaySoftGradientForTraffic enabling this will display the traffic color transitions
     * with a gradual gradient color blend. If false the color transitions will abruptly transition from
     * one color to the next.
     * @param softGradientTransition this value influences the length of the color transition when
     * the displaySoftGradientForTraffic param is set to true
     */
    class Builder internal constructor(
        private val context: Context,
        private var routeLineResources: RouteLineResources?,
        private var routeLineBelowLayerId: String?,
        private var routeStyleDescriptors: List<RouteStyleDescriptor>,
        private var vanishingRouteLineEnabled: Boolean,
        private var tolerance: Double,
        private var displayRestrictedRoadSections: Boolean,
        private var styleInactiveRouteLegsIndependently: Boolean,
        private var displaySoftGradientForTraffic: Boolean,
        private var softGradientTransition: Double
    ) {

        /**
         * Responsible for instantiating an instance of MapboxRouteLineOptions
         *
         * @param context an instance of Context
         */
        constructor(context: Context) : this(
            context,
            null,
            null,
            listOf(),
            false,
            DEFAULT_ROUTE_SOURCES_TOLERANCE,
            false,
            false,
            false,
            RouteLayerConstants.SOFT_GRADIENT_STOP_GAP_METERS
        )

        /**
         * An instance of RouteLineResources
         *
         * @return the builder
         */
        fun withRouteLineResources(resourceProvider: RouteLineResources): Builder =
            apply { this.routeLineResources = resourceProvider }

        /**
         * Indicates the elevation of the route line related layers. A good starting point is
         * [LocationComponentConstants.LOCATION_INDICATOR_LAYER] for 2D location puck and
         * [LocationComponentConstants.MODEL_LAYER] for 3D location puck.
         *
         * If no value is provided the route line related layers will be placed at the top of the [Map] layer stack.
         *
         * @return the builder
         */
        fun withRouteLineBelowLayerId(layerId: String): Builder =
            apply { this.routeLineBelowLayerId = layerId }

        /**
         * Determines if the vanishing route line feature is enabled. It is not enabled by default.
         *
         * @return the builder
         */
        fun withVanishingRouteLineEnabled(isEnabled: Boolean): Builder =
            apply { this.vanishingRouteLineEnabled = isEnabled }

        /**
         * Douglas-Peucker simplification tolerance (higher means simpler geometries and faster performance)
         * for the GeoJsonSources created to display the route line.
         *
         * Defaults to 0.375.
         *
         * @return the builder
         * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/sources/#geojson-tolerance">The online documentation</a>
         */
        fun withTolerance(tolerance: Double): Builder {
            this.tolerance = tolerance
            return this
        }

        /**
         * [RouteStyleDescriptor] is an override of an alternative route line coloring based on
         * a provided identifier. Setting one or more [RouteStyleDescriptor] objects here
         * will configure the layer such that any route set with a matching identifier will get
         * colored according to the values provided in the [RouteStyleDescriptor].
         *
         * @param routeStyleDescriptors a collection of [RouteStyleDescriptor] objects
         * @return the builder
         */
        fun withRouteStyleDescriptors(routeStyleDescriptors: List<RouteStyleDescriptor>): Builder =
            apply { this.routeStyleDescriptors = routeStyleDescriptors }

        /**
         * Indicates if the route line will display restricted
         * road sections with a dashed line. False by default.
         *
         * @return the builder
         */
        fun displayRestrictedRoadSections(displayRestrictedRoadSections: Boolean): Builder =
            apply { this.displayRestrictedRoadSections = displayRestrictedRoadSections }

        /**
         * Enabling this feature will result in route legs that aren't currently being navigated
         * to be color differently than the active leg. See [RouteLineColorResources] for the
         * color option.
         *
         * @return the builder
         */
        fun styleInactiveRouteLegsIndependently(enable: Boolean): Builder =
            apply { this.styleInactiveRouteLegsIndependently = enable }

        /**
         * Influences the appearance of the gradient used for the traffic line. By default
         * there is an abrupt transition between different colors representing traffic congestion.
         * If this value is set to true there is a smoother transition between the colors. This
         * value is false by default.
         *
         * @return the builder
         */
        fun displaySoftGradientForTraffic(enable: Boolean): Builder =
            apply { this.displaySoftGradientForTraffic = enable }

        /**
         * When [displayRestrictedRoadSections] is set to true this value will influence the
         * length of the gradient transition between traffic congestion colors. Larger values
         * will result in the color transition occurring over a longer length of the line.
         * Values between 5 and 75 are recommended. The default value is 30.
         *
         * @param transitionDistance a value above zero used for the gradient transition distance
         * @return the builder
         */
        fun softGradientTransition(transitionDistance: Int): Builder {
            if (transitionDistance == 0) {
                throw IllegalArgumentException("A value above zero was expected.")
            }
            this.softGradientTransition = abs(transitionDistance).toDouble()
            return this
        }

        /**
         * @return an instance of [MapboxRouteLineOptions]
         */
        fun build(): MapboxRouteLineOptions {
            val resourceProvider: RouteLineResources = routeLineResources
                ?: RouteLineResources.Builder().build()

            val routeLineLayerProvider = MapboxRouteLayerProvider(
                routeStyleDescriptors,
                resourceProvider.routeLineScaleExpression,
                resourceProvider.routeCasingLineScaleExpression,
                resourceProvider.routeTrafficLineScaleExpression,
                resourceProvider.alternativeRouteLineScaleExpression,
                resourceProvider.alternativeRouteCasingLineScaleExpression,
                resourceProvider.alternativeRouteTrafficLineScaleExpression
            )

            val originIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.originWaypointIcon
            )

            val destinationIcon = AppCompatResources.getDrawable(
                context,
                resourceProvider.destinationWaypointIcon
            )

            val vanishingRouteLine = if (vanishingRouteLineEnabled) {
                VanishingRouteLine()
            } else {
                null
            }

            return MapboxRouteLineOptions(
                resourceProvider,
                routeLineLayerProvider,
                originIcon!!,
                destinationIcon!!,
                routeLineBelowLayerId,
                vanishingRouteLine,
                tolerance,
                displayRestrictedRoadSections,
                styleInactiveRouteLegsIndependently,
                displaySoftGradientForTraffic,
                softGradientTransition
            )
        }
    }
}
