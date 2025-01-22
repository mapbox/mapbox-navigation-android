package com.mapbox.navigation.ui.maps.route.callout.model

import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.R

/**
 * Options for configuration appearance of route callouts.
 *
 * @param backgroundColor to style the background color of route callout
 * @param selectedBackgroundColor to style the background color of route callout attaching to
 * the primary route in [RouteCalloutType.RouteDurations] state
 * @param textColor to style the text color of route callout
 * @param selectedTextColor to style the text color of route callout attaching to the primary
 * route in [RouteCalloutType.RouteDurations] state
 * @param fasterTextColor to style the text color of route callout attaching to alternative route
 * which duration is faster in comparison with the primary one in
 * [RouteCalloutType.RelativeDurationsOnAlternative] state
 * @param slowerTextColor to style the text color of route callout attaching to alternative route
 * which duration is slower in comparison with the primary one in
 * [RouteCalloutType.RelativeDurationsOnAlternative] state
 * @param durationTextAppearance to style the text appearance of route callout. Note that textColor
 * parameter will be overridden by other parameters from [MapboxRouteCalloutViewOptions]
 */
@ExperimentalPreviewMapboxNavigationAPI
class MapboxRouteCalloutViewOptions private constructor(
    @ColorRes val backgroundColor: Int,
    @ColorRes val selectedBackgroundColor: Int,
    @ColorRes val textColor: Int,
    @ColorRes val selectedTextColor: Int,
    @ColorRes val fasterTextColor: Int,
    @ColorRes val slowerTextColor: Int,
    @StyleRes val durationTextAppearance: Int,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder() = Builder().apply {
        backgroundColor(backgroundColor)
        selectedBackgroundColor(selectedBackgroundColor)
        textColor(textColor)
        selectedTextColor(selectedTextColor)
        fasterTextColor(fasterTextColor)
        slowerTextColor(slowerTextColor)
        durationTextAppearance(durationTextAppearance)
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxRouteCalloutViewOptions

        if (backgroundColor != other.backgroundColor) return false
        if (selectedBackgroundColor != other.selectedBackgroundColor) return false
        if (textColor != other.textColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (fasterTextColor != other.fasterTextColor) return false
        if (slowerTextColor != other.slowerTextColor) return false
        if (durationTextAppearance != other.durationTextAppearance) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + selectedBackgroundColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + fasterTextColor.hashCode()
        result = 31 * result + slowerTextColor.hashCode()
        result = 31 * result + durationTextAppearance.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxRouteCalloutViewOptions(" +
            "backgroundColor=$backgroundColor," +
            "selectedBackgroundColor=$selectedBackgroundColor," +
            "textColor=$textColor," +
            "selectedTextColor=$selectedTextColor," +
            "fasterTextColor=$fasterTextColor," +
            "slowerTextColor=$slowerTextColor," +
            "durationTextAppearance=$durationTextAppearance," +
            ")"
    }

    class Builder {
        @ColorRes
        private var backgroundColor: Int = R.color.mapbox_route_callout_background

        @ColorRes
        private var selectedBackgroundColor: Int = R.color.mapbox_selected_route_callout_background

        @ColorRes
        private var textColor: Int = R.color.mapbox_route_callout_text

        @ColorRes
        private var selectedTextColor: Int = R.color.mapbox_selected_route_callout_text

        @ColorRes
        private var fasterTextColor: Int = R.color.mapbox_faster_route_callout_text

        @ColorRes
        private var slowerTextColor: Int = R.color.mapbox_slower_route_callout_text

        @StyleRes
        private var durationTextAppearance: Int = R.style.MapboxStyleRouteCalloutDuration

        /**
         * to style the background color of route callout
         */
        fun backgroundColor(@ColorRes value: Int) = this.apply {
            backgroundColor = value
        }

        /**
         * to style the background color of route callout attaching to the primary route
         * in [RouteCalloutType.RouteDurations] state
         */
        fun selectedBackgroundColor(@ColorRes value: Int) = this.apply {
            selectedBackgroundColor = value
        }

        /**
         * to style the text color of route callout
         */
        fun textColor(@ColorRes value: Int) = this.apply {
            textColor = value
        }

        /**
         * to style the text color of route callout attaching to the primary route
         * in [RouteCalloutType.RouteDurations] state
         */
        fun selectedTextColor(@ColorRes value: Int) = this.apply {
            selectedTextColor = value
        }

        /**
         * to style the text color of route callout attaching to alternative route which duration
         * is faster in comparison with the primary one in
         * [RouteCalloutType.RelativeDurationsOnAlternative] state
         */
        fun fasterTextColor(@ColorRes value: Int) = this.apply {
            fasterTextColor = value
        }

        /**
         * to style the text color of route callout attaching to alternative route which duration
         * is slower in comparison with the primary one in
         * [RouteCalloutType.RelativeDurationsOnAlternative] state
         */
        fun slowerTextColor(@ColorRes value: Int) = this.apply {
            slowerTextColor = value
        }

        /**
         * to style the text appearance of route callout. Note that textColor
         * parameter will be overridden by other parameters from [MapboxRouteCalloutViewOptions]
         */
        fun durationTextAppearance(@StyleRes value: Int) = this.apply {
            durationTextAppearance = value
        }

        /**
         * Build the [MapboxRouteCalloutViewOptions]
         */
        fun build() = MapboxRouteCalloutViewOptions(
            backgroundColor,
            selectedBackgroundColor,
            textColor,
            selectedTextColor,
            fasterTextColor,
            slowerTextColor,
            durationTextAppearance,
        )
    }
}
