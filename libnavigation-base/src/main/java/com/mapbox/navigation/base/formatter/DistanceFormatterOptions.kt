package com.mapbox.navigation.base.formatter

import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import java.util.Locale

/**
 * Gives options to format the distance in the trip notification.
 *
 * @param locale the locale to use for localization of distance resources
 * @param unitType to use, or UNDEFINED to use default for locale country
 * @param roundingIncrement increment by which to round small distances
 */
class DistanceFormatterOptions private constructor(
    val locale: Locale?,
    @VoiceUnit.Type val unitType: String,
    @Rounding.Increment val roundingIncrement: Int
) {

    /**
     * @return the [Builder] that created the [DistanceFormatterOptions]
     */
    fun toBuilder() = Builder()
        .locale(locale)
        .unitType(unitType)
        .roundingIncrement(roundingIncrement)

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DistanceFormatterOptions

        if (locale != other.locale) return false
        if (unitType != other.unitType) return false
        if (roundingIncrement != other.roundingIncrement) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = locale.hashCode()
        result = 31 * result + unitType.hashCode()
        result = 31 * result + roundingIncrement
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "DistanceFormatterOptions(locale=$locale," +
            " unitType='$unitType'," +
            " roundingIncrement=$roundingIncrement)"
    }

    /**
     * Builder of [DistanceFormatterOptions]
     */
    class Builder {
        private var unitType: String = VoiceUnit.UNDEFINED
        private var locale: Locale? = null
        private var roundingIncrement = Rounding.INCREMENT_FIFTY

        /**
         * Policy for the various units of measurement, UNDEFINED uses default for locale country
         *
         * @param unitType String
         * @return Builder
         */
        fun unitType(@VoiceUnit.Type unitType: String) =
            apply { this.unitType = unitType }

        /**
         * Minimal value that distance might be stripped
         *
         * @param roundingIncrement [Rounding.Increment]
         * @return Builder
         */
        fun roundingIncrement(@Rounding.Increment roundingIncrement: Int) =
            apply { this.roundingIncrement = roundingIncrement }

        /**
         * Use a non-default [Locale]. By default, the [Locale] is used from applicationContext
         *
         * @param locale [Locale]
         * @return Builder
         */
        fun locale(locale: Locale?) =
            apply { this.locale = locale }

        /**
         * Build a new instance of [DistanceFormatterOptions]
         *
         * @return [DistanceFormatterOptions]
         */
        fun build(): DistanceFormatterOptions =
            DistanceFormatterOptions(
                locale = locale,
                unitType = unitType,
                roundingIncrement = roundingIncrement
            )
    }
}
