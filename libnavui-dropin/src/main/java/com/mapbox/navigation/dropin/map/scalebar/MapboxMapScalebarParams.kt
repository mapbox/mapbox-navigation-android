package com.mapbox.navigation.dropin.map.scalebar

import android.content.Context
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale

/**
 * Params describing the map scalebar config.
 *
 * @param enabled true if the scalebar should be shown on the map, false otherwise
 * @param isMetricUnits true if the scale bar is using metric system,
 *  false if the scale bar is using imperial units.
 */
class MapboxMapScalebarParams private constructor(
    val enabled: Boolean,
    val isMetricUnits: Boolean,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxMapScalebarParams

        if (enabled != other.enabled) return false
        if (isMetricUnits != other.isMetricUnits) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + isMetricUnits.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "MapboxMapScalebarParams(" +
            "enabled=$enabled, " +
            "isMetricUnits=$isMetricUnits" +
            ")"
    }

    /**
     * Builder for [MapboxMapScalebarParams].
     */
    class Builder(context: Context) {

        private val applicationContext = context.applicationContext
        private var enabled: Boolean = false
        private var isMetricsUnits: Boolean? = null

        /**
         * Whether to show the scalebar on the map.
         * @param enabled true if the scalebar should be shown on the map, false otherwise
         * @return the same builder object
         */
        fun enabled(enabled: Boolean): Builder = apply {
            this.enabled = enabled
        }

        /**
         * Whether the scale bar is using metric unit.
         * @param isMetricUnits true if the scale bar is using metric system,
         *  false if the scale bar is using imperial units.
         *  @return the same builder object
         */
        fun isMetricsUnits(isMetricUnits: Boolean?): Builder = apply {
            this.isMetricsUnits = isMetricUnits
        }

        /**
         * Create a [MapboxMapScalebarParams] object.
         * @return [MapboxMapScalebarParams]
         */
        fun build(): MapboxMapScalebarParams {
            return MapboxMapScalebarParams(
                enabled,
                isMetricsUnits ?: getDefaultIsMetricUnits()
            )
        }

        private fun getDefaultIsMetricUnits(): Boolean {
            val unitType = applicationContext.inferDeviceLocale().getUnitTypeForLocale()
            return when (unitType) {
                UnitType.IMPERIAL -> false
                UnitType.METRIC -> true
            }
        }
    }
}
