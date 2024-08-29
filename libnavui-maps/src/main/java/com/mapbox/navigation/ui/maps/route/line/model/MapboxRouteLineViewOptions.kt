package com.mapbox.navigation.ui.maps.route.line.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import kotlin.jvm.Throws

/**
 * Options for configuration of [MapboxRouteLineView].
 *
 * @param context context
 * @param routeLineColorResources an instance of [RouteLineColorResources] containing color information
 * @param scaleExpressions an instance of [RouteLineScaleExpressions] containing custom scaling expressions
 * @param restrictedRoadDashArray the dash array for the [LineLayer] used for displaying restricted roads
 * @param restrictedRoadOpacity the opacity of the restricted road [LineLayer]
 * @param restrictedRoadLineWidth the width of the restricted road [LineLayer]
 * @param displaySoftGradientForTraffic determines if the color transition between traffic congestion
 * changes should use a soft gradient appearance or abrupt color change. This is false by default.
 * @param softGradientTransition influences the length of the color transition when the displaySoftGradientForTraffic
 * parameter is true.
 * @param originWaypointIcon an icon representing the origin point of a route
 * @param destinationWaypointIcon an icon representing the destination point of a route
 * @param waypointLayerIconOffset the list of offset values for waypoint icons
 * @param waypointLayerIconAnchor the anchor value, the default is [IconAnchor.CENTER]
 * @param iconPitchAlignment the pitch alignment value used for waypoint icons. The default is [IconPitchAlignment.MAP]
 * @param displayRestrictedRoadSections indicates if the route line will display restricted
 * road sections with a dashed line. Note that in order for restricted sections to be displayed,
 * you also need to set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections] to true,
 * so that the necessary data is calculated.
 * You can have a set-up when some of your [MapboxRouteLineView]s display the restricted sections and the other don't.
 * I that case set [MapboxRouteLineViewOptions.displayRestrictedRoadSections] to true
 * for those views that will display restricted sections and set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections]
 * to true if at least one of your views will display them.
 * @param routeLineBelowLayerId determines the elevation of the route layers. Note that if you are using Mapbox Standard style,
 * you can only specify a layer id that is added at runtime: static layer ids from the style will not be applied.
 * @param tolerance the tolerance value used when configuring the underlying map source
 * @param shareLineGeometrySources enable route line's GeoJson source data sharing between multiple instances of the map.
 * If this option is enabled for multiple instances of [MapboxRouteLineView]s that are used to draw route lines on multiple maps at the same time,
 * they will all share the GeoJson source to optimize execution time of updates and decrease the memory footprint.
 * **Enable only for instances that should share the geometry of the lines**, leave disabled for instances that should draw geometries distinct from other instances.
 * @param lineDepthOcclusionFactor factor that decreases line layer opacity based on occlusion from 3D objects. Value 0 disables occlusion, value 1 means fully occluded.
 * @param slotName determines the position of the route layers in the map style. The route line will default to the [RouteLayerConstants.DEFAULT_ROUTE_LINE_SLOT] slot.
 * To change, add your own custom slot before route line initialization and provide the slot name as a option here. see https://docs.mapbox.com/style-spec/reference/slots/ and [StyleManager.getStyleSlots]
 * @param fadeOnHighZoomsConfig configuration for fading out of the route line. See [FadingConfig] for details. If not set, the route line will be fully opaque at all zoom levels.
 * NOTE: this property guards fading out the route line on transition from a lower to a higher zoom level,
 * meaning that [FadingConfig.startFadingZoom] must be less than or equal to [FadingConfig.finishFadingZoom].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxRouteLineViewOptions private constructor(
    private val context: Context,
    val routeLineColorResources: RouteLineColorResources,
    val scaleExpressions: RouteLineScaleExpressions,
    val restrictedRoadDashArray: List<Double>,
    val restrictedRoadOpacity: Double,
    val restrictedRoadLineWidth: Double,
    val displaySoftGradientForTraffic: Boolean,
    val softGradientTransition: Double,
    @DrawableRes
    internal val originIconId: Int,
    val originWaypointIcon: Drawable,
    @DrawableRes
    internal val destinationIconId: Int,
    val destinationWaypointIcon: Drawable,
    val waypointLayerIconOffset: List<Double>,
    val waypointLayerIconAnchor: IconAnchor,
    val iconPitchAlignment: IconPitchAlignment,
    val displayRestrictedRoadSections: Boolean,
    @Deprecated("Use slotName option.") val routeLineBelowLayerId: String?,
    val tolerance: Double,
    val shareLineGeometrySources: Boolean,
    @FloatRange(from = 0.0, to = 1.0)
    val lineDepthOcclusionFactor: Double,
    val slotName: String,
    val fadeOnHighZoomsConfig: FadingConfig?,
) {

    /**
     * Creates a builder matching this instance.
     */
    fun toBuilder(): Builder {
        return Builder(context)
            .routeLineColorResources(routeLineColorResources)
            .scaleExpressions(scaleExpressions)
            .restrictedRoadDashArray(restrictedRoadDashArray)
            .restrictedRoadOpacity(restrictedRoadOpacity)
            .restrictedRoadLineWidth(restrictedRoadLineWidth)
            .displaySoftGradientForTraffic(displaySoftGradientForTraffic)
            .softGradientTransition(softGradientTransition)
            .originWaypointIcon(originIconId)
            .destinationWaypointIcon(destinationIconId)
            .waypointLayerIconOffset(waypointLayerIconOffset)
            .waypointLayerIconAnchor(waypointLayerIconAnchor)
            .iconPitchAlignment(iconPitchAlignment)
            .displayRestrictedRoadSections(displayRestrictedRoadSections)
            .routeLineBelowLayerId(routeLineBelowLayerId)
            .tolerance(tolerance)
            .shareLineGeometrySources(shareLineGeometrySources)
            .lineDepthOcclusionFactor(lineDepthOcclusionFactor)
            .slotName(slotName)
            .fadeOnHighZoomsConfig(fadeOnHighZoomsConfig)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteLineViewOptions

        if (context != other.context) return false
        if (routeLineColorResources != other.routeLineColorResources) return false
        if (scaleExpressions != other.scaleExpressions) return false
        if (restrictedRoadDashArray != other.restrictedRoadDashArray) return false
        if (restrictedRoadOpacity != other.restrictedRoadOpacity) return false
        if (restrictedRoadLineWidth != other.restrictedRoadLineWidth) return false
        if (displaySoftGradientForTraffic != other.displaySoftGradientForTraffic) return false
        if (softGradientTransition != other.softGradientTransition) return false
        if (originIconId != other.originIconId) return false
        if (originWaypointIcon != other.originWaypointIcon) return false
        if (destinationIconId != other.destinationIconId) return false
        if (destinationWaypointIcon != other.destinationWaypointIcon) return false
        if (waypointLayerIconOffset != other.waypointLayerIconOffset) return false
        if (waypointLayerIconAnchor != other.waypointLayerIconAnchor) return false
        if (iconPitchAlignment != other.iconPitchAlignment) return false
        if (displayRestrictedRoadSections != other.displayRestrictedRoadSections) return false
        if (routeLineBelowLayerId != other.routeLineBelowLayerId) return false
        if (tolerance != other.tolerance) return false
        if (shareLineGeometrySources != other.shareLineGeometrySources) return false
        if (lineDepthOcclusionFactor != other.lineDepthOcclusionFactor) return false
        if (slotName != other.slotName) return false
        if (fadeOnHighZoomsConfig != other.fadeOnHighZoomsConfig) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + routeLineColorResources.hashCode()
        result = 31 * result + scaleExpressions.hashCode()
        result = 31 * result + restrictedRoadDashArray.hashCode()
        result = 31 * result + restrictedRoadOpacity.hashCode()
        result = 31 * result + restrictedRoadLineWidth.hashCode()
        result = 31 * result + displaySoftGradientForTraffic.hashCode()
        result = 31 * result + softGradientTransition.hashCode()
        result = 31 * result + originIconId
        result = 31 * result + originWaypointIcon.hashCode()
        result = 31 * result + destinationIconId
        result = 31 * result + destinationWaypointIcon.hashCode()
        result = 31 * result + waypointLayerIconOffset.hashCode()
        result = 31 * result + waypointLayerIconAnchor.hashCode()
        result = 31 * result + iconPitchAlignment.hashCode()
        result = 31 * result + displayRestrictedRoadSections.hashCode()
        result = 31 * result + (routeLineBelowLayerId?.hashCode() ?: 0)
        result = 31 * result + tolerance.hashCode()
        result = 31 * result + shareLineGeometrySources.hashCode()
        result = 31 * result + lineDepthOcclusionFactor.hashCode()
        result = 31 * result + slotName.hashCode()
        result = 31 * result + fadeOnHighZoomsConfig.hashCode()

        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteLineDynamicOptions(" +
            "context=$context, " +
            "routeLineColorResources=$routeLineColorResources, " +
            "scaleExpressions=$scaleExpressions, " +
            "restrictedRoadDashArray=$restrictedRoadDashArray, " +
            "restrictedRoadOpacity=$restrictedRoadOpacity, " +
            "restrictedRoadLineWidth=$restrictedRoadLineWidth, " +
            "displaySoftGradientForTraffic=$displaySoftGradientForTraffic, " +
            "softGradientTransition=$softGradientTransition, " +
            "originIconId=$originIconId, " +
            "originWaypointIcon=$originWaypointIcon, " +
            "destinationIconId=$destinationIconId, " +
            "destinationWaypointIcon=$destinationWaypointIcon, " +
            "waypointLayerIconOffset=$waypointLayerIconOffset, " +
            "waypointLayerIconAnchor=$waypointLayerIconAnchor, " +
            "iconPitchAlignment=$iconPitchAlignment, " +
            "displayRestrictedRoadSections=$displayRestrictedRoadSections, " +
            "routeLineBelowLayerId=$routeLineBelowLayerId, " +
            "tolerance=$tolerance, " +
            "shareLineGeometrySources=$shareLineGeometrySources, " +
            "lineDepthOcclusionFactor=$lineDepthOcclusionFactor, " +
            "slotName=$slotName, " +
            "fadingConfig=$fadeOnHighZoomsConfig" +
            ")"
    }

    /**
     * A builder used to create instance of [MapboxRouteLineViewOptions].
     *
     * @param context context
     */
    class Builder(private val context: Context) {

        private var routeLineColorResources: RouteLineColorResources =
            RouteLineColorResources.Builder().build()
        private var scaleExpressions: RouteLineScaleExpressions =
            RouteLineScaleExpressions.Builder().build()
        private var restrictedRoadDashArray: List<Double> =
            RouteLayerConstants.RESTRICTED_ROAD_DASH_ARRAY
        private var restrictedRoadOpacity: Double = RouteLayerConstants.RESTRICTED_ROAD_LINE_OPACITY
        private var restrictedRoadLineWidth: Double = RouteLayerConstants.RESTRICTED_ROAD_LINE_WIDTH
        private var displaySoftGradientForTraffic: Boolean = false
        private var softGradientTransition: Double =
            RouteLayerConstants.SOFT_GRADIENT_STOP_GAP_METERS

        @DrawableRes
        private var originWaypointIcon: Int = RouteLayerConstants.ORIGIN_WAYPOINT_ICON

        @DrawableRes
        private var destinationWaypointIcon: Int = RouteLayerConstants.DESTINATION_WAYPOINT_ICON
        private var waypointLayerIconOffset: List<Double> = listOf(0.0, 0.0)
        private var waypointLayerIconAnchor: IconAnchor = IconAnchor.CENTER
        private var iconPitchAlignment: IconPitchAlignment = IconPitchAlignment.MAP
        private var displayRestrictedRoadSections: Boolean = false
        private var routeLineBelowLayerId: String? = null
        private var tolerance: Double = RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE
        private var shareLineGeometrySources: Boolean = false

        @FloatRange(from = 0.0, to = 1.0)
        private var lineDepthOcclusionFactor: Double = 0.0
        private var slotName: String = RouteLayerConstants.DEFAULT_ROUTE_LINE_SLOT
        private var fadeOnHighZoomsConfig: FadingConfig? = null

        /**
         * An instance of [RouteLineColorResources].
         * Contains information about colors used for route line.
         *
         * @param routeLineColorResources an instance of [RouteLineColorResources].
         * @return the same builder
         */
        fun routeLineColorResources(
            routeLineColorResources: RouteLineColorResources,
        ): Builder = apply {
            this.routeLineColorResources = routeLineColorResources
        }

        /**
         * An instance of [RouteLineScaleExpressions].
         * Contains information about custom scaling expressions.
         *
         * @param scaleExpressions an instance of [RouteLineScaleExpressions].
         * @return the same builder
         */
        fun scaleExpressions(scaleExpressions: RouteLineScaleExpressions): Builder = apply {
            this.scaleExpressions = scaleExpressions
        }

        /**
         * The dash array for the [LineLayer] used for displaying restricted roads.
         *
         * @param restrictedRoadDashArray dash array
         * @return the builder
         */
        fun restrictedRoadDashArray(restrictedRoadDashArray: List<Double>): Builder = apply {
            this.restrictedRoadDashArray = restrictedRoadDashArray
        }

        /**
         * The opacity of the restricted road [LineLayer].
         *
         * @param restrictedRoadOpacity opacity
         * @return the builder
         */
        fun restrictedRoadOpacity(restrictedRoadOpacity: Double): Builder = apply {
            this.restrictedRoadOpacity = restrictedRoadOpacity
        }

        /**
         * The width of the restricted road [LineLayer].
         *
         * @param restrictedRoadLineWidth line width
         * @return the builder
         */
        fun restrictedRoadLineWidth(restrictedRoadLineWidth: Double): Builder = apply {
            this.restrictedRoadLineWidth = restrictedRoadLineWidth
        }

        /**
         * Determines if the color transition between traffic congestion changes should use a
         * soft gradient appearance or abrupt color change. This is false by default.
         *
         * @param displaySoftGradientForTraffic whether soft gradient transition should be enabled
         * @return the builder
         */
        fun displaySoftGradientForTraffic(displaySoftGradientForTraffic: Boolean): Builder = apply {
            this.displaySoftGradientForTraffic = displaySoftGradientForTraffic
        }

        /**
         * Influences the length of the color transition when the displaySoftGradientForTraffic
         * parameter is true.
         *
         * @param softGradientTransition transition
         * @return the builder
         */
        fun softGradientTransition(softGradientTransition: Double): Builder = apply {
            this.softGradientTransition = softGradientTransition
        }

        /**
         * The drawable id for representing the origin icon.
         *
         * @param originWaypointIcon the id of an origin icon
         * @return the builder
         */
        fun originWaypointIcon(@DrawableRes originWaypointIcon: Int): Builder = apply {
            this.originWaypointIcon = originWaypointIcon
        }

        /**
         * The drawable id for representing the destination icon.

         * @param destinationWaypointIcon the id of a destination icon
         * @return the builder
         */
        fun destinationWaypointIcon(@DrawableRes destinationWaypointIcon: Int): Builder = apply {
            this.destinationWaypointIcon = destinationWaypointIcon
        }

        /**
         * The list of offset values for waypoint icons.
         *
         * @param waypointLayerIconOffset offset
         * @return the builder
         */
        fun waypointLayerIconOffset(waypointLayerIconOffset: List<Double>): Builder = apply {
            this.waypointLayerIconOffset = waypointLayerIconOffset
        }

        /**
         * The anchor value, the default is [IconAnchor.CENTER].
         *
         * @param waypointLayerIconAnchor icon anchor
         * @return the builder
         */
        fun waypointLayerIconAnchor(waypointLayerIconAnchor: IconAnchor): Builder = apply {
            this.waypointLayerIconAnchor = waypointLayerIconAnchor
        }

        /**
         * The pitch alignment value used for waypoint icons. The default is [IconPitchAlignment.MAP].
         *
         * @param iconPitchAlignment alignment
         * @return the builder
         */
        fun iconPitchAlignment(iconPitchAlignment: IconPitchAlignment): Builder = apply {
            this.iconPitchAlignment = iconPitchAlignment
        }

        /**
         * Indicates if the route line will display restricted
         * road sections with a dashed line. Note that in order for restricted sections to be displayed,
         * you also need to set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections] to true,
         * so that the necessary data is calculated.
         * You can have a set-up when some of your [MapboxRouteLineView]s display the restricted sections and the other don't.
         * I that case set [MapboxRouteLineViewOptions.displayRestrictedRoadSections] to true
         * for those views that will display restricted sections and set [MapboxRouteLineApiOptions.calculateRestrictedRoadSections]
         * to true if at least one of your views will display them.
         *
         * @param displayRestrictedRoadSections whether restricted road sections should be displayed
         * @return the builder
         */
        fun displayRestrictedRoadSections(displayRestrictedRoadSections: Boolean): Builder = apply {
            this.displayRestrictedRoadSections = displayRestrictedRoadSections
        }

        /**
         * Determines the elevation of the route layers. Note that if you are using Mapbox Standard style,
         * you can only specify a layer id that is added at runtime: static layer ids from the style will not be applied.
         *
         * @param layerId layer id below which the route line layers will be placed
         * @return the builder
         */
        fun routeLineBelowLayerId(layerId: String?): Builder =
            apply { this.routeLineBelowLayerId = layerId }

        /**
         * The tolerance value used when configuring the underlying map source.
         *
         * @param tolerance tolerance
         * @return the builder
         */
        fun tolerance(tolerance: Double): Builder = apply {
            this.tolerance = tolerance
        }

        /**
         * Enable route line's GeoJson source data sharing between multiple instances of the map.
         * If this option is enabled for multiple instances of [MapboxRouteLineView]s that are used to draw route lines on multiple maps at the same time,
         * they will all share the GeoJson source to optimize execution time of updates and decrease the memory footprint.
         * **Enable only for instances that should share the geometry of the lines**, leave disabled for instances that should draw geometries distinct from other instances.
         *
         * @param shareLineGeometrySources whether geometry sources should be shared
         * @return the builder
         */
        fun shareLineGeometrySources(shareLineGeometrySources: Boolean): Builder =
            apply { this.shareLineGeometrySources = shareLineGeometrySources }

        /**
         * Factor that decreases line layer opacity based on occlusion from 3D objects.
         * Value 0 disables occlusion, value 1 means fully occluded.
         *
         * @param lineDepthOcclusionFactor occlusion factor
         * @return the builder
         */
        fun lineDepthOcclusionFactor(
            @FloatRange(from = 0.0, to = 1.0) lineDepthOcclusionFactor: Double,
        ): Builder = apply {
            if (lineDepthOcclusionFactor !in 0.0..1.0) {
                throw IllegalArgumentException(
                    "lineDepthOcclusionFactor should be in range [0.0; 1.0]",
                )
            }
            this.lineDepthOcclusionFactor = lineDepthOcclusionFactor
        }

        /**
         * The slot name to use for route line position in the layer stack.
         *
         * ```
         * style.addPersistentLayer(slotLayer("runtimeSlotId") {
         *   slot("middle")
         * })
         * ```
         * The slot layer needs to be added persistently, because so are the route line layers, and they
         * require the slot reference immediately on style reload to be correctly recreated.
         *
         * @param name the name of the slot to use.
         * @return the builder
         */
        fun slotName(name: String): Builder = apply {
            this.slotName = name
        }

        /**
         * Configuration for fading out of the route line. See [FadingConfig] for details.
         * If not set or set it null, the route line will be fully opaque at all zoom levels.
         * NOTE: this property guards fading out the route line on transition from a lower to a higher zoom level,
         * meaning that [FadingConfig.startFadingZoom] must be less than or equal to [FadingConfig.finishFadingZoom].
         *
         * @param config [FadingConfig]
         * @return the builder
         */
        fun fadeOnHighZoomsConfig(config: FadingConfig?) = apply {
            this.fadeOnHighZoomsConfig = config
        }

        /**
         * Creates an instance of [MapboxRouteLineViewOptions].
         *
         * @return an instance of [MapboxRouteLineViewOptions].
         * @throws IllegalArgumentException if provided originIconId or destinationIconId are not valid drawable resources.
         */
        @Throws(IllegalArgumentException::class)
        fun build(): MapboxRouteLineViewOptions {
            val originIcon = AppCompatResources.getDrawable(
                context,
                originWaypointIcon,
            )

            val destinationIcon = AppCompatResources.getDrawable(
                context,
                destinationWaypointIcon,
            )
            if (originIcon == null) {
                throw IllegalArgumentException("Could not find origin icon resource")
            }
            if (destinationIcon == null) {
                throw IllegalArgumentException("Could not find destination icon resource")
            }
            fadeOnHighZoomsConfig?.let {
                if (it.startFadingZoom > it.finishFadingZoom) {
                    throw IllegalArgumentException(
                        "FadingConfig#startFadingZoom (${it.startFadingZoom}) must be less than " +
                            "or equal to FadingConfig#finishFadingZoom (${it.finishFadingZoom}).",
                    )
                }
            }
            return MapboxRouteLineViewOptions(
                context,
                routeLineColorResources,
                scaleExpressions,
                restrictedRoadDashArray,
                restrictedRoadOpacity,
                restrictedRoadLineWidth,
                displaySoftGradientForTraffic,
                softGradientTransition,
                originWaypointIcon,
                originIcon,
                destinationWaypointIcon,
                destinationIcon,
                waypointLayerIconOffset,
                waypointLayerIconAnchor,
                iconPitchAlignment,
                displayRestrictedRoadSections,
                routeLineBelowLayerId,
                tolerance,
                shareLineGeometrySources,
                lineDepthOcclusionFactor,
                slotName,
                fadeOnHighZoomsConfig,
            )
        }
    }
}
