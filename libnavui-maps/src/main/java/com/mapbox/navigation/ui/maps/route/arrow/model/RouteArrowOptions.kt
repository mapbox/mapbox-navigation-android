package com.mapbox.navigation.ui.maps.route.arrow.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID

/**
 * Options for determining the appearance of maneuver arrow(s)
 *
 * @param arrowColor the color of the arrow shaft
 * @param arrowBorderColor the color of the arrow shaft border
 * @param arrowHeadIcon the drawable to represent the arrow head
 * @param arrowHeadIconBorder the drawable to represent the arrow head border
 * @param aboveLayerId indicates the maneuver arrow map layers appear above this layer on the map
 */
class RouteArrowOptions private constructor(
    @ColorInt val arrowColor: Int,
    @ColorInt val arrowBorderColor: Int,
    private val arrowHeadIconDrawable: Int,
    private val arrowHeadIconCasingDrawable: Int,
    val arrowHeadIcon: Drawable,
    val arrowHeadIconBorder: Drawable,
    val aboveLayerId: String
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
            arrowBorderColor,
            arrowHeadIconDrawable,
            arrowHeadIconCasingDrawable,
            aboveLayerId
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
        if (arrowBorderColor != other.arrowBorderColor) return false
        if (arrowHeadIconDrawable != other.arrowHeadIconDrawable) return false
        if (arrowHeadIconCasingDrawable != other.arrowHeadIconCasingDrawable) return false
        if (arrowHeadIcon != other.arrowHeadIcon) return false
        if (arrowHeadIconBorder != other.arrowHeadIconBorder) return false
        if (aboveLayerId != other.aboveLayerId) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = arrowColor
        result = 31 * result + arrowBorderColor
        result = 31 * result + arrowHeadIconDrawable
        result = 31 * result + arrowHeadIconCasingDrawable
        result = 31 * result + arrowHeadIcon.hashCode()
        result = 31 * result + arrowHeadIconBorder.hashCode()
        result = 31 * result + aboveLayerId.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteArrowOptions(arrowColor=$arrowColor, " +
            "arrowBorderColor=$arrowBorderColor, " +
            "arrowHeadIconDrawable=$arrowHeadIconDrawable, " +
            "arrowHeadIconCasingDrawable=$arrowHeadIconCasingDrawable, " +
            "arrowHeadIcon=$arrowHeadIcon, " +
            "arrowHeadIconBorder=$arrowHeadIconBorder, " +
            "aboveLayerId='$aboveLayerId')"
    }

    /**
     * Used for instantiating the RouteArrowOptions class.
     */
    class Builder internal constructor(
        private val context: Context,
        private var arrowColor: Int,
        private var arrowBorderColor: Int,
        private var arrowHeadIconDrawable: Int,
        private var arrowHeadIconCasingDrawable: Int,
        private var aboveLayerId: String?
    ) {

        /**
         * Used for instantiating the RouteArrowOptions class.
         */
        constructor(context: Context) : this(
            context,
            RouteConstants.MANEUVER_ARROW_COLOR,
            RouteConstants.MANEUVER_ARROW_BORDER_COLOR,
            RouteConstants.MANEUVER_ARROWHEAD_ICON_DRAWABLE,
            RouteConstants.MANEUVER_ARROWHEAD_ICON_CASING_DRAWABLE,
            null
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
        fun withArrowBorderColor(@ColorInt color: Int): Builder =
            apply { this.arrowBorderColor = color }

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
         * Indicates the maneuver arrow map layers appear above this layer on the map.
         *
         * @param layerId the map layer ID
         */
        fun withAboveLayerId(layerId: String): Builder =
            apply { this.aboveLayerId = layerId }

        /**
         * Applies the supplied parameters and instantiates a RouteArrowOptions
         *
         * @return a RouteArrowOptions object
         */
        fun build(): RouteArrowOptions {
            val arrowHeadIcon = AppCompatResources.getDrawable(
                context,
                arrowHeadIconDrawable
            )
            val arrowHeadCasingIcon = AppCompatResources.getDrawable(
                context,
                arrowHeadIconCasingDrawable
            )
            val routeArrowAboveLayerId: String = aboveLayerId ?: PRIMARY_ROUTE_TRAFFIC_LAYER_ID

            return RouteArrowOptions(
                arrowColor,
                arrowBorderColor,
                arrowHeadIconDrawable,
                arrowHeadIconCasingDrawable,
                arrowHeadIcon!!,
                arrowHeadCasingIcon!!,
                routeArrowAboveLayerId
            )
        }
    }
}
