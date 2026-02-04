package com.mapbox.navigation.ui.maps.route.arrow.model

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.model.FadingConfig

/**
 * Options for determining the appearance of maneuver arrow(s)
 *
 * @param arrowColor the color of the arrow shaft
 * @param arrowCasingColor the color of the arrow shaft border
 * @param arrowHeadIcon the drawable to represent the arrow head
 * @param arrowHeadIconCasing the drawable to represent the arrow head border
 * @param aboveLayerId indicates the maneuver arrow map layers appear above this layer on the map
 * @param tolerance the tolerance value used when configuring the underlying map source
 * @param arrowShaftScaleExpression an [Expression] governing the scaling of the maneuver arrow shaft
 * @param arrowShaftCasingScaleExpression an [Expression] governing the scaling of the maneuver arrow shaft casing
 * @param arrowHeadScaleExpression an [Expression] governing the scaling of the maneuver arrow head
 * @param arrowHeadCasingScaleExpression an [Expression] governing the scaling of the maneuver arrow head casing
 * @param slotName determines the elevation of the route layers. The route line will default to the MIDDLE slot.
 * To change, add your own custom slot before route arrow initialization and provide the slot name as a option here. see https://docs.mapbox.com/style-spec/reference/slots/ and [StyleManager.getStyleSlots]
 * @param fadeOnHighZoomsConfig configuration of route arrows fading out (see [FadingConfig] for details).
 * NOTE: route arrows disappear at zoom levels 14.0 or lower. Make sure zoom levels specified in [FadingConfig] are greater than 14.0.
 * NOTE: this property guards fading out the arrows on transition from a lower to a higher zoom level,
 * meaning that [FadingConfig.startFadingZoom] must be less than or equal to [FadingConfig.finishFadingZoom].
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteArrowOptions private constructor(
    @ColorInt val arrowColor: Int,
    @ColorInt val arrowCasingColor: Int,
    private val arrowHeadIconDrawable: Int,
    private val arrowHeadIconCasingDrawable: Int,
    val arrowHeadIcon: Drawable,
    val arrowHeadIconCasing: Drawable,
    @Deprecated("Use slotName option.") val aboveLayerId: String,
    val tolerance: Double,
    val arrowShaftScaleExpression: Expression,
    val arrowShaftCasingScaleExpression: Expression,
    val arrowHeadScaleExpression: Expression,
    val arrowHeadCasingScaleExpression: Expression,
    val slotName: String,
    val fadeOnHighZoomsConfig: FadingConfig?,
) {

    /**
     * @param context a valid context
     *
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(context: Context): Builder {
        return Builder(
            context,
            arrowColor,
            arrowCasingColor,
            arrowHeadIconDrawable,
            arrowHeadIconCasingDrawable,
            aboveLayerId,
            tolerance,
            arrowShaftScaleExpression,
            arrowShaftCasingScaleExpression,
            arrowHeadScaleExpression,
            arrowHeadCasingScaleExpression,
            slotName,
            fadeOnHighZoomsConfig,
        )
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteArrowOptions

        if (arrowColor != other.arrowColor) return false
        if (arrowCasingColor != other.arrowCasingColor) return false
        if (arrowHeadIconDrawable != other.arrowHeadIconDrawable) return false
        if (arrowHeadIconCasingDrawable != other.arrowHeadIconCasingDrawable) return false
        if (arrowHeadIcon != other.arrowHeadIcon) return false
        if (arrowHeadIconCasing != other.arrowHeadIconCasing) return false
        if (aboveLayerId != other.aboveLayerId) return false
        if (tolerance != other.tolerance) return false
        if (arrowShaftScaleExpression != other.arrowShaftScaleExpression) return false
        if (arrowShaftCasingScaleExpression != other.arrowShaftCasingScaleExpression) return false
        if (arrowHeadScaleExpression != other.arrowHeadScaleExpression) return false
        if (arrowHeadCasingScaleExpression != other.arrowHeadCasingScaleExpression) return false
        if (slotName != other.slotName) return false
        if (fadeOnHighZoomsConfig != other.fadeOnHighZoomsConfig) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = arrowColor
        result = 31 * result + arrowCasingColor
        result = 31 * result + arrowHeadIconDrawable
        result = 31 * result + arrowHeadIconCasingDrawable
        result = 31 * result + arrowHeadIcon.hashCode()
        result = 31 * result + arrowHeadIconCasing.hashCode()
        result = 31 * result + aboveLayerId.hashCode()
        result = 31 * result + (tolerance.hashCode())
        result = 31 * result + arrowShaftScaleExpression.hashCode()
        result = 31 * result + arrowShaftCasingScaleExpression.hashCode()
        result = 31 * result + arrowHeadScaleExpression.hashCode()
        result = 31 * result + arrowHeadCasingScaleExpression.hashCode()
        result = 31 * result + slotName.hashCode()
        result = 31 * result + fadeOnHighZoomsConfig.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteArrowOptions(arrowColor=$arrowColor, " +
            "arrowCasingColor=$arrowCasingColor, " +
            "arrowHeadIconDrawable=$arrowHeadIconDrawable, " +
            "arrowHeadIconCasingDrawable=$arrowHeadIconCasingDrawable, " +
            "arrowHeadIcon=$arrowHeadIcon, " +
            "arrowHeadIconCasing=$arrowHeadIconCasing, " +
            "aboveLayerId='$aboveLayerId', " +
            "tolerance=$tolerance, " +
            "arrowShaftScaleExpression=$arrowShaftScaleExpression, " +
            "arrowShaftCasingScaleExpression=$arrowShaftCasingScaleExpression, " +
            "arrowHeadScaleExpression=$arrowHeadScaleExpression, " +
            "arrowHeadCasingScaleExpression=$arrowHeadCasingScaleExpression, " +
            "slotName=$slotName, " +
            "fadingConfig=$fadeOnHighZoomsConfig" +
            ")"
    }

    /**
     * Used for instantiating the RouteArrowOptions class.
     */
    class Builder internal constructor(
        private val context: Context,
        private var arrowColor: Int,
        private var arrowCasingColor: Int,
        private var arrowHeadIconDrawable: Int,
        private var arrowHeadIconCasingDrawable: Int,
        private var aboveLayerId: String?,
        private var tolerance: Double,
        private var arrowShaftScaleExpression: Expression?,
        private var arrowShaftCasingScaleExpression: Expression?,
        private var arrowHeadScaleExpression: Expression?,
        private var arrowHeadCasingScaleExpression: Expression?,
        private var slotName: String?,
        private var fadeOnHighZoomsConfig: FadingConfig?,
    ) {

        /**
         * Used for instantiating the RouteArrowOptions class.
         */
        constructor(context: Context) : this(
            context,
            RouteLayerConstants.MANEUVER_ARROW_COLOR,
            RouteLayerConstants.MANEUVER_ARROW_CASING_COLOR,
            RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE,
            RouteLayerConstants.MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE,
            null,
            RouteLayerConstants.DEFAULT_ROUTE_SOURCES_TOLERANCE,
            null,
            null,
            null,
            null,
            null,
            null,
        )

        private val defaultDensityContext = context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                densityDpi = DisplayMetrics.DENSITY_DEFAULT
            },
        )

        /**
         * Indicates the color of the arrow shaft.
         *
         * @param color the color to be used
         */
        fun withArrowColor(@ColorInt color: Int): Builder =
            apply { this.arrowColor = color }

        /**
         * Indicates the color of the arrow shaft border.
         *
         * @param color the color to be used
         */
        fun withArrowCasingColor(@ColorInt color: Int): Builder =
            apply { this.arrowCasingColor = color }

        /**
         * Indicates the drawable of the arrow head.
         *
         * @param drawable the drawable to be used
         */
        fun withArrowHeadIconDrawable(@DrawableRes drawable: Int): Builder =
            apply { this.arrowHeadIconDrawable = drawable }

        /**
         * Indicates the drawable of the arrow head border.
         *
         * @param drawable the drawable to be used
         */
        fun withArrowHeadIconCasingDrawable(@DrawableRes drawable: Int): Builder =
            apply { this.arrowHeadIconCasingDrawable = drawable }

        /**
         * Indicates the maneuver arrow map layers appear above this layer on the map. A good
         * starting point for this is [TOP_LEVEL_ROUTE_LINE_LAYER_ID].
         *
         * @param layerId the map layer ID
         */
        @Deprecated("Use slotName")
        fun withAboveLayerId(layerId: String): Builder =
            apply { this.aboveLayerId = layerId }

        /**
         * The slot name to use for route arrow position in the layer stack.
         *
         * @param name the name of the slot to use.
         * @return the builder
         */
        fun withSlotName(name: String): Builder = apply { this.slotName = name }

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
         * An expression that will define the scaling behavior of the maneuver arrow shafts.
         *
         * @param expression the expression governing the scaling of the maneuver arrow shafts
         *
         * @return the builder
         */
        fun withArrowShaftScalingExpression(expression: Expression): Builder =
            apply { this.arrowShaftScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the maneuver arrow shaft casing.
         *
         * @param expression the expression governing the scaling of the maneuver arrow shaft casing
         *
         * @return the builder
         */
        fun withArrowShaftCasingScalingExpression(expression: Expression): Builder =
            apply { this.arrowShaftCasingScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the maneuver arrow head.
         *
         * @param expression the expression governing the scaling of the maneuver arrow head
         *
         * @return the builder
         */
        fun withArrowheadScalingExpression(expression: Expression): Builder =
            apply { this.arrowHeadScaleExpression = expression }

        /**
         * An expression that will define the scaling behavior of the maneuver arrow head casing.
         *
         * @param expression the expression governing the scaling of the maneuver arrow head casing
         *
         * @return the builder
         */
        fun withArrowheadCasingScalingExpression(expression: Expression): Builder =
            apply { this.arrowHeadCasingScaleExpression = expression }

        /**
         * Configuration of route arrows fading out (see [FadingConfig] for details).
         * NOTE: route arrows disappear at zoom levels 14.0 or lower. Make sure zoom levels specified in [FadingConfig] are greater than 14.0.
         * NOTE: this property guards fading out the arrows on transition from a lower to a higher zoom level,
         * meaning that [FadingConfig.startFadingZoom] must be less than or equal to [FadingConfig.finishFadingZoom].
         *
         * @param config [FadingConfig]
         * @return the builder
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun withFadeOnHighZoomsConfig(config: FadingConfig?): Builder = apply {
            this.fadeOnHighZoomsConfig = config
        }

        /**
         * Applies the supplied parameters and instantiates a RouteArrowOptions
         *
         * @return a RouteArrowOptions object
         */
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        fun build(): RouteArrowOptions {
            val arrowHeadIcon = AppCompatResources.getDrawable(
                defaultDensityContext,
                arrowHeadIconDrawable,
            )
            val arrowHeadCasingIcon = AppCompatResources.getDrawable(
                defaultDensityContext,
                arrowHeadIconCasingDrawable,
            )
            val routeArrowAboveLayerId: String = aboveLayerId ?: TOP_LEVEL_ROUTE_LINE_LAYER_ID
            val routeArrowSlotName: String = slotName ?: RouteLayerConstants.DEFAULT_ROUTE_LINE_SLOT

            fadeOnHighZoomsConfig?.let {
                if (it.startFadingZoom <= RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL) {
                    throw IllegalArgumentException(
                        "FadingConfig#startFadingZoom must be greater than or equal to" +
                            " ${RouteLayerConstants.ARROW_HIDDEN_ZOOM_LEVEL}, " +
                            "but was: ${it.startFadingZoom}.",
                    )
                }
                if (it.startFadingZoom > it.finishFadingZoom) {
                    throw IllegalArgumentException(
                        "FadingConfig#startFadingZoom (${it.startFadingZoom}) must be less than " +
                            "or equal to FadingConfig#finishFadingZoom (${it.finishFadingZoom}).",
                    )
                }
            }

            val arrowShaftScalingExpression: Expression = arrowShaftScaleExpression
                ?: Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                        literal(RouteLayerConstants.MIN_ZOOM_ARROW_SHAFT_SCALE)
                    }
                    stop {
                        literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                        literal(RouteLayerConstants.MAX_ZOOM_ARROW_SHAFT_SCALE)
                    }
                }

            val arrowShaftCasingScaleExpression: Expression = arrowShaftCasingScaleExpression
                ?: Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                        literal(RouteLayerConstants.MIN_ZOOM_ARROW_SHAFT_CASING_SCALE)
                    }
                    stop {
                        literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                        literal(RouteLayerConstants.MAX_ZOOM_ARROW_SHAFT_CASING_SCALE)
                    }
                }

            val arrowHeadScalingExpression: Expression = arrowHeadScaleExpression
                ?: Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                        literal(RouteLayerConstants.MIN_ZOOM_ARROW_HEAD_SCALE)
                    }
                    stop {
                        literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                        literal(RouteLayerConstants.MAX_ZOOM_ARROW_HEAD_SCALE)
                    }
                }

            val arrowHeadCasingScalingExpression: Expression = arrowHeadCasingScaleExpression
                ?: Expression.interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(RouteLayerConstants.MIN_ARROW_ZOOM)
                        literal(RouteLayerConstants.MIN_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                    stop {
                        literal(RouteLayerConstants.MAX_ARROW_ZOOM)
                        literal(RouteLayerConstants.MAX_ZOOM_ARROW_HEAD_CASING_SCALE)
                    }
                }

            return RouteArrowOptions(
                arrowColor,
                arrowCasingColor,
                arrowHeadIconDrawable,
                arrowHeadIconCasingDrawable,
                arrowHeadIcon!!,
                arrowHeadCasingIcon!!,
                routeArrowAboveLayerId,
                tolerance,
                arrowShaftScalingExpression,
                arrowShaftCasingScaleExpression,
                arrowHeadScalingExpression,
                arrowHeadCasingScalingExpression,
                routeArrowSlotName,
                fadeOnHighZoomsConfig,
            )
        }
    }
}
