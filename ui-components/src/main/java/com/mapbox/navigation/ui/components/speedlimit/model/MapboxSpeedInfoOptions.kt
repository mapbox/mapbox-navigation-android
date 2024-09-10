package com.mapbox.navigation.ui.components.speedlimit.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import com.mapbox.navigation.ui.components.speedlimit.view.MapboxSpeedInfoView

/**
 * Specifies options for rendering [MapboxSpeedInfoView].
 *
 * @param showUnit If set to true [MapboxSpeedInfoView] will render the unit for posted speed limit
 * for MUTCD convention either in MPH or KM/H based on [UnitType.IMPERIAL] or [UnitType.METRIC].
 * Default value is true.
 * @param showLegend If set to true [MapboxSpeedInfoView] will render the text "Speed Limit" for
 * MUTCD convention. Default value is false.
 * @param speedInfoStyle applies styles to [MapboxSpeedInfoView]
 * @param showSpeedWhenUnavailable If set to true [MapboxSpeedInfoView] will render the posted speed
 * with text "--". Default value is false.
 * @param renderWithSpeedSign [MapboxSpeedInfoView] will always render in Vienna convention if
 * [renderWithSpeedSign] is assigned to [SpeedLimitSign.VIENNA] irrespective of the geography user is
 * currently located in. Similarly, [MapboxSpeedInfoView] will always render in Mutcd convention if
 * [renderWithSpeedSign] is assigned to [SpeedLimitSign.MUTCD] irrespective of the geography user is
 * currently located in. If [renderWithSpeedSign] is assigned to null, [MapboxSpeedInfoView] will
 * render according to users current geography. Default value is null.
 * @param currentSpeedDirection [MapboxSpeedInfoView] will render user's current speed relative to
 * posted speed limit according to the value assigned to [currentSpeedDirection].
 * Default value is set to [CurrentSpeedDirection.BOTTOM]
 */
class MapboxSpeedInfoOptions private constructor(
    val showUnit: Boolean,
    val showLegend: Boolean,
    val speedInfoStyle: SpeedInfoStyle,
    @ExperimentalPreviewMapboxNavigationAPI val showSpeedWhenUnavailable: Boolean,
    @ExperimentalPreviewMapboxNavigationAPI val renderWithSpeedSign: SpeedLimitSign?,
    val currentSpeedDirection: CurrentSpeedDirection,
) {

    /**
     * @return the [Builder] that created the [MapboxSpeedInfoOptions]
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun toBuilder(): Builder {
        return Builder()
            .showUnit(showUnit)
            .showLegend(showLegend)
            .speedInfoStyle(speedInfoStyle)
            .showSpeedWhenUnavailable(showSpeedWhenUnavailable)
            .renderWithSpeedSign(renderWithSpeedSign)
            .currentSpeedDirection(currentSpeedDirection)
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun toString(): String {
        return "MapboxSpeedInfoOptions(" +
            "showUnit=$showUnit, " +
            "showLegend=$showLegend, " +
            "speedInfoStyle=$speedInfoStyle, " +
            "showSpeedWhenUnavailable=$showSpeedWhenUnavailable, " +
            "renderWithSpeedSign=$renderWithSpeedSign, " +
            "currentSpeedDirection=$currentSpeedDirection" +
            ")"
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxSpeedInfoOptions

        if (showUnit != other.showUnit) return false
        if (showLegend != other.showLegend) return false
        if (speedInfoStyle != other.speedInfoStyle) return false
        if (showSpeedWhenUnavailable != other.showSpeedWhenUnavailable) return false
        if (renderWithSpeedSign != other.renderWithSpeedSign) return false
        if (currentSpeedDirection != other.currentSpeedDirection) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun hashCode(): Int {
        var result = showUnit.hashCode()
        result = 31 * result + showLegend.hashCode()
        result = 31 * result + speedInfoStyle.hashCode()
        result = 31 * result + showSpeedWhenUnavailable.hashCode()
        result = 31 * result + renderWithSpeedSign.hashCode()
        result = 31 * result + currentSpeedDirection.hashCode()
        return result
    }

    /**
     * Builder of [MapboxSpeedInfoOptions]
     */
    class Builder {
        private var showUnit: Boolean = true
        private var showLegend: Boolean = false
        private var speedInfoStyle: SpeedInfoStyle = SpeedInfoStyle()
        private var showSpeedWhenUnavailable: Boolean = false
        private var renderWithSpeedSign: SpeedLimitSign? = null
        private var currentSpeedDirection: CurrentSpeedDirection = CurrentSpeedDirection.BOTTOM

        /**
         * Apply the value to show or hide posted speed limit unit for MUTCD based conventions.
         * @param showUnit set true to show the posted speed limit unit and false to hide.
         */
        fun showUnit(showUnit: Boolean): Builder = apply {
            this.showUnit = showUnit
        }

        /**
         * Apply the value to show or hide the text "Speed Limit" for MUTCD based conventions.
         * @param showLegend set true to show the text "Speed Limit" and false to hide.
         */
        fun showLegend(showLegend: Boolean): Builder = apply {
            this.showLegend = showLegend
        }

        /**
         * Apply the value to apply styles to [MapboxSpeedInfoView].
         * @param speedInfoStyle set to apply your custom styles.
         */
        fun speedInfoStyle(speedInfoStyle: SpeedInfoStyle): Builder = apply {
            this.speedInfoStyle = speedInfoStyle
        }

        /**
         * Apply the value to show or hide speed UI when posted speed data is unavailable. Default
         * value is false and speed UI will not be shown
         * @param showSpeedWhenUnavailable set true to show the speed UI and false to hide.
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun showSpeedWhenUnavailable(showSpeedWhenUnavailable: Boolean): Builder = apply {
            this.showSpeedWhenUnavailable = showSpeedWhenUnavailable
        }

        /**
         * Apply to render [MapboxSpeedInfoView] in VIENNA or MUTCD convention irrespective
         * of user's current location by setting it to [SpeedLimitSign.VIENNA] or [SpeedLimitSign.MUTCD].
         * Set it to null to let [MapboxSpeedInfoView] decide the convention based on user's current
         * location.
         * @param renderWithSpeedSign set to [SpeedLimitSign.VIENNA] or [SpeedLimitSign.MUTCD] or null
         */
        @ExperimentalPreviewMapboxNavigationAPI
        fun renderWithSpeedSign(renderWithSpeedSign: SpeedLimitSign?): Builder = apply {
            this.renderWithSpeedSign = renderWithSpeedSign
        }

        /**
         * Apply to render the user's current speed either to the start, top, end or bottom of
         * posted speed limit.
         * @param currentSpeedDirection can be [CurrentSpeedDirection.START],
         * [CurrentSpeedDirection.TOP], [CurrentSpeedDirection.END] or [CurrentSpeedDirection.BOTTOM]
         */
        fun currentSpeedDirection(currentSpeedDirection: CurrentSpeedDirection): Builder = apply {
            this.currentSpeedDirection = currentSpeedDirection
        }

        /**
         * Build a new instance of [MapboxSpeedInfoOptions]
         *
         * @return MapboxSpeedInfoOptions
         */
        fun build(): MapboxSpeedInfoOptions {
            return MapboxSpeedInfoOptions(
                showUnit,
                showLegend,
                speedInfoStyle,
                showSpeedWhenUnavailable,
                renderWithSpeedSign,
                currentSpeedDirection,
            )
        }
    }
}
