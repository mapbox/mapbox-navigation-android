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
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.internal.VoiceUnit
import com.mapbox.navigation.core.R
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
 * @param options to build the [MapboxDistanceFormatter]
 */
class MapboxDistanceFormatter(
    val options: DistanceFormatterOptions
) : DistanceFormatter {

    private val smallUnit = when (options.unitType) {
        VoiceUnit.IMPERIAL -> TurfConstants.UNIT_FEET
        else -> TurfConstants.UNIT_METERS
    }

    private val largeUnit = when (options.unitType) {
        VoiceUnit.IMPERIAL -> TurfConstants.UNIT_MILES
        else -> TurfConstants.UNIT_KILOMETERS
    }

    companion object {
        private const val smallDistanceUpperThresholdInMeters = 400.0
        private const val mediumDistanceUpperThresholdInMeters = 10000.0
    }

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
        val resources = options.applicationContext.resourcesWithLocale(options.locale)
        val unitStringSuffix = getUnitString(resources, smallUnit)

        if (distance <= 0) {
            return Pair("0", unitStringSuffix)
        }

        val distanceUnit = TurfConversion.convertLength(
            distance,
            TurfConstants.UNIT_METERS,
            smallUnit
        )

        val roundedValue = if (options.roundingIncrement > 0) {
            distanceUnit.roundToInt() / options.roundingIncrement * options.roundingIncrement
        } else {
            distanceUnit.roundToInt()
        }.toString()

        return Pair(roundedValue, unitStringSuffix)
    }

    private fun formatDistanceAndSuffixForLargeUnit(
        distance: Double,
        maxFractionDigits: Int
    ): Pair<String, String> {
        val resources = options.applicationContext.resourcesWithLocale(options.locale)
        val unitStringSuffix = getUnitString(resources, largeUnit)
        val distanceUnit =
            TurfConversion.convertLength(distance, TurfConstants.UNIT_METERS, largeUnit)
        val roundedValue = NumberFormat.getNumberInstance(options.locale).also {
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
            RelativeSizeSpan(0.75f),
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
}
