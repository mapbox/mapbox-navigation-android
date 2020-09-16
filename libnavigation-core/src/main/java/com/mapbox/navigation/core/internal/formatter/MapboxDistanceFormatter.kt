package com.mapbox.navigation.core.internal.formatter

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.core.R
import com.mapbox.navigation.core.Rounding
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConversion
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Creates an instance of DistanceFormatter, which can format distances in meters
 * based on a language format and unit type.
 *
 * This constructor will infer device language and unit type using the device locale.
 *
 * @param applicationContext from which to get localized strings from
 * @param locale the locale to use for localization of distance resources
 * @param unitType to use, or UNDEFINED to use default for locale country
 * @param roundingIncrement increment by which to round small distances
 */
class MapboxDistanceFormatter private constructor(
    private val applicationContext: Context,
    private val locale: Locale,
    @VoiceUnit.Type private val unitType: String,
    @Rounding.Increment private val roundingIncrement: Int
) : DistanceFormatter {

    private val smallUnit = when (unitType) {
        VoiceUnit.IMPERIAL -> TurfConstants.UNIT_FEET
        else -> TurfConstants.UNIT_METERS
    }

    private val largeUnit = when (unitType) {
        VoiceUnit.IMPERIAL -> TurfConstants.UNIT_MILES
        else -> TurfConstants.UNIT_KILOMETERS
    }

    companion object {
        private const val smallDistanceUpperThresholdInMeters = 400.0
        private const val mediumDistanceUpperThresholdInMeters = 10000.0
    }

    /**
     * @return the [Builder] that created the [MapboxDistanceFormatter]
     */
    fun toBuilder() = Builder(applicationContext)
        .locale(locale)
        .unitType(unitType)
        .roundingIncrement(roundingIncrement)

    /**
     * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
     *
     * @param distance in meters
     * @return SpannableString representation which has a bolded number and units which have a
     * relative size of .65 times the size of the number
     */
    override fun formatDistance(distance: Double): SpannableString {
        val distanceAndSuffix = when (distance) {
            !in 0.0..Double.MAX_VALUE -> {
                formatDistanceAndSuffixForSmallUnit(0.0)
            }
            in 0.0..smallDistanceUpperThresholdInMeters -> {
                formatDistanceAndSuffixForSmallUnit(distance)
            }
            in smallDistanceUpperThresholdInMeters..mediumDistanceUpperThresholdInMeters -> {
                formatDistanceAndSuffixForLargeUnit(distance, 1)
            }
            else -> {
                formatDistanceAndSuffixForLargeUnit(distance, 0)
            }
        }
        return getSpannableDistanceString(distanceAndSuffix)
    }

    private fun formatDistanceAndSuffixForSmallUnit(distance: Double): Pair<String, String> {
        val distanceUnit = TurfConversion.convertLength(
            distance,
            TurfConstants.UNIT_METERS,
            smallUnit
        )
        val resources = applicationContext.resourcesWithLocale(locale)
        val unitStringSuffix = getUnitString(resources, smallUnit)
        val roundedNumber = distanceUnit.roundToInt() / roundingIncrement * roundingIncrement
        val roundedValue =
            (if (roundedNumber < roundingIncrement) roundingIncrement else roundedNumber)
                .toString()
        return Pair(roundedValue, unitStringSuffix)
    }

    private fun formatDistanceAndSuffixForLargeUnit(
        distance: Double,
        maxFractionDigits: Int
    ): Pair<String, String> {
        val resources = applicationContext.resourcesWithLocale(locale)
        val unitStringSuffix = getUnitString(resources, largeUnit)
        val distanceUnit =
            TurfConversion.convertLength(distance, TurfConstants.UNIT_METERS, largeUnit)
        val roundedValue = NumberFormat.getNumberInstance(locale).also {
            it.maximumFractionDigits = maxFractionDigits
        }.format(distanceUnit)
        return Pair(roundedValue, unitStringSuffix)
    }

    /**
     * Takes in a distance and units and returns a formatted SpannableString where the number is bold
     * and the unit is shrunked to .65 times the size
     *
     * @param distanceAndSuffix distance formatted with appropriate decimal places and unit string
     * from TurfConstants. This will be converted to the abbreviated form.
     * @return [SpannableString] with bolded distance and shrunken units
     */
    internal fun getSpannableDistanceString(
        distanceAndSuffix: Pair<String, String>
    ): SpannableString {
        val spannableString =
            SpannableString("${distanceAndSuffix.first} ${distanceAndSuffix.second}")

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            distanceAndSuffix.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            RelativeSizeSpan(0.65f),
            distanceAndSuffix.first.length + 1,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    private fun getUnitString(resources: Resources, @TurfConstants.TurfUnitCriteria unit: String) =
        when (unit) {
            TurfConstants.UNIT_KILOMETERS -> resources.getString(R.string.mapbox_unit_kilometers)
            TurfConstants.UNIT_METERS -> resources.getString(R.string.mapbox_unit_meters)
            TurfConstants.UNIT_MILES -> resources.getString(R.string.mapbox_unit_miles)
            TurfConstants.UNIT_FEET -> resources.getString(R.string.mapbox_unit_feet)
            else -> ""
        }

    private fun Context.resourcesWithLocale(locale: Locale?): Resources {
        val config = Configuration(this.resources.configuration).also {
            it.setLocale(locale)
        }
        return this.createConfigurationContext(config).resources
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapboxDistanceFormatter

        if (applicationContext != other.applicationContext) return false
        if (locale != other.locale) return false
        if (unitType != other.unitType) return false
        if (roundingIncrement != other.roundingIncrement) return false
        if (smallUnit != other.smallUnit) return false
        if (largeUnit != other.largeUnit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = applicationContext.hashCode()
        result = 31 * result + locale.hashCode()
        result = 31 * result + unitType.hashCode()
        result = 31 * result + roundingIncrement
        result = 31 * result + smallUnit.hashCode()
        result = 31 * result + largeUnit.hashCode()
        return result
    }

    override fun toString(): String {
        return "MapboxDistanceFormatter(" +
            "applicationContext=$applicationContext, " +
            "locale=$locale, unitType='$unitType', " +
            "roundingIncrement=$roundingIncrement, " +
            "smallUnit='$smallUnit', " +
            "largeUnit='$largeUnit'" +
            ")"
    }

    /**
     * Builder of [MapboxDistanceFormatter]
     * @param applicationContext converted to applicationContext to save memory leaks
     */
    class Builder(applicationContext: Context) {
        private val applicationContext: Context = applicationContext.applicationContext
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
        fun locale(locale: Locale) =
            apply { this.locale = locale }

        /**
         * Build a new instance of [MapboxDistanceFormatter]
         *
         * @return [MapboxDistanceFormatter]
         */
        fun build(): MapboxDistanceFormatter {
            val localeToUse: Locale = locale ?: applicationContext.inferDeviceLocale()
            val unitTypeToUse: String = when (unitType) {
                VoiceUnit.UNDEFINED -> localeToUse.getUnitTypeForLocale()
                else -> unitType
            }

            return MapboxDistanceFormatter(
                applicationContext = applicationContext,
                locale = localeToUse,
                unitType = unitTypeToUse,
                roundingIncrement = roundingIncrement
            )
        }
    }
}
