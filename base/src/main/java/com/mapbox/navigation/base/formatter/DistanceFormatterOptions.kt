package com.mapbox.navigation.base.formatter

import android.content.Context
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import java.util.Locale

/**
 * Gives options to format the distance in the trip notification.
 *
 * If not provided, the object will infer device language and unit type using the device locale from the context.
 *
 * @param applicationContext from which to get localized strings from
 * @param locale the locale to use for localization of distance resources
 * @param unitType to use for voice and visual information.
 * @param roundingIncrement increment by which to round small distances
 */
class DistanceFormatterOptions private constructor(
    val applicationContext: Context,
    val locale: Locale,
    val unitType: UnitType,
    @Rounding.Increment val roundingIncrement: Int,
) {

    /**
     * @return the [Builder] that created the [DistanceFormatterOptions]
     */
    fun toBuilder(): Builder = Builder(applicationContext)
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

        if (applicationContext != other.applicationContext) return false
        if (locale != other.locale) return false
        if (unitType != other.unitType) return false
        if (roundingIncrement != other.roundingIncrement) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + locale.hashCode()
        result = 31 * result + unitType.hashCode()
        result = 31 * result + roundingIncrement
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "DistanceFormatterOptions(" +
            "applicationContext=$applicationContext, " +
            "locale=$locale, " +
            "unitType='$unitType', " +
            "roundingIncrement=$roundingIncrement" +
            ")"
    }

    /**
     * Builder of [DistanceFormatterOptions]
     * @param applicationContext converted to applicationContext to save memory leaks
     */
    class Builder(applicationContext: Context) {
        private val applicationContext: Context = applicationContext.applicationContext
        private var locale: Locale = applicationContext.inferDeviceLocale()
        private var unitType: UnitType? = null
        private var roundingIncrement = Rounding.INCREMENT_DISTANCE_DEPENDENT

        /**
         * Policy for the various units of measurement.
         * If null, default unit for locale country will be used.
         *
         * @param unitType UnitType
         * @return Builder
         */
        fun unitType(unitType: UnitType?): Builder =
            apply { this.unitType = unitType }

        /**
         * Minimal value that distance might be stripped
         *
         * @param roundingIncrement [Rounding.Increment]
         * @return Builder
         */
        fun roundingIncrement(@Rounding.Increment roundingIncrement: Int): Builder =
            apply { this.roundingIncrement = roundingIncrement }

        /**
         * Use a non-default [Locale]. By default, the [Locale] is used from applicationContext
         *
         * @param locale [Locale]
         * @return Builder
         */
        fun locale(locale: Locale): Builder =
            apply { this.locale = locale }

        /**
         * Build a new instance of [DistanceFormatterOptions]
         *
         * @return [DistanceFormatterOptions]
         */
        fun build() = DistanceFormatterOptions(
            applicationContext = applicationContext,
            locale = locale,
            unitType = unitType ?: locale.getUnitTypeForLocale(),
            roundingIncrement = roundingIncrement,
        )
    }
}
