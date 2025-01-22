package com.mapbox.navigation.ui.maps.building.model

import android.graphics.Color
import androidx.annotation.ColorInt

/**
 * The options for highlighting a building upon arrival.
 *
 * @param fillExtrusionColor RGB Color of the extruded building
 * @param fillExtrusionOpacity Opacity of the extruded building [0.0, 1.0]
 */
class MapboxBuildingHighlightOptions private constructor(
    val fillExtrusionColor: Int,
    val fillExtrusionOpacity: Double,
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        fillExtrusionColor(fillExtrusionColor)
        fillExtrusionOpacity(fillExtrusionOpacity)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxBuildingHighlightOptions

        if (fillExtrusionColor != other.fillExtrusionColor) return false
        if (fillExtrusionOpacity != other.fillExtrusionOpacity) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = fillExtrusionColor.hashCode()
        result = 31 * result + fillExtrusionOpacity.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "BuildingArrivalOptions(" +
            "fillExtrusionColor=$fillExtrusionColor, " +
            "fillExtrusionOpacity=$fillExtrusionOpacity" +
            ")"
    }

    /**
     * Build a new [MapboxBuildingHighlightOptions]
     */
    class Builder {
        private var fillExtrusionColor = Color.parseColor("#FC2B14")
        private var fillExtrusionOpacity: Double = 0.6

        /**
         * Fill extrusion color of the 3d building with default set to #FC2B14
         */
        fun fillExtrusionColor(@ColorInt fillExtrusionColor: Int): Builder =
            apply { this.fillExtrusionColor = fillExtrusionColor }

        /**
         * Fill extrusion opacity of the 3d building with default set to 0.6
         */
        fun fillExtrusionOpacity(fillExtrusionOpacity: Double): Builder =
            apply { this.fillExtrusionOpacity = fillExtrusionOpacity }

        /**
         * Build a new instance of [MapboxBuildingHighlightOptions]
         * with [fillExtrusionColor] default set to #FC2B14
         * and [fillExtrusionOpacity] default set to 0.6
         * @return NavigationOptions
         */
        fun build(): MapboxBuildingHighlightOptions {
            return MapboxBuildingHighlightOptions(
                fillExtrusionColor = fillExtrusionColor,
                fillExtrusionOpacity = fillExtrusionOpacity,
            )
        }
    }
}
