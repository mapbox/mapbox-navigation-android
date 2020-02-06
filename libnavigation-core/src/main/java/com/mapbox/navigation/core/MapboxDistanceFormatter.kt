package com.mapbox.navigation.core

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.extensions.getUnitTypeForLocale
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.METRIC
import com.mapbox.navigation.base.typedef.RoundingIncrement
import com.mapbox.navigation.base.typedef.VoiceUnit
import com.mapbox.navigation.trip.notification.R
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.turf.TurfConstants.UNIT_FEET
import com.mapbox.turf.TurfConstants.UNIT_KILOMETERS
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfConstants.UNIT_MILES
import com.mapbox.turf.TurfConversion
import java.text.NumberFormat
import java.util.Locale

/**
 * Creates an instance of DistanceFormatter, which can format distances in meters
 * based on a language format and unit type.
 *
 *
 * This constructor will infer device language and unit type using the device locale.
 *
 * @param language for which language
 * @param unitType to use, or UNDEFINED to use default for locale country
 * @param context from which to get localized strings from
 * @param roundingIncrement increment by which to round small distances
 */
class MapboxDistanceFormatter(
    private val context: Context,
    language: String?,
    @VoiceUnit unitType: String,
    @RoundingIncrement private val roundingIncrement: Int
) : DistanceFormatter {

    private val unitStrings = hashMapOf<String, String>(
        UNIT_KILOMETERS to context.getString(R.string.kilometers),
        UNIT_METERS to context.getString(R.string.meters),
        UNIT_MILES to context.getString(R.string.miles),
        UNIT_FEET to context.getString(R.string.feet)
    )
    private val largeUnit = when (IMPERIAL == unitType) {
        true -> UNIT_MILES
        false -> UNIT_KILOMETERS
    }
    private val smallUnit = when (IMPERIAL == unitType) {
        true -> UNIT_FEET
        false -> UNIT_METERS
    }
    private val language: String
    @VoiceUnit
    private val unitType: String
    private val numberFormat: NumberFormat

    init {
        val locale = when (language == null) {
            true -> context.inferDeviceLocale()
            false -> Locale(language)
        }
        this.language = locale.language
        this.unitType = when (IMPERIAL != unitType && METRIC != unitType) {
            true -> context.inferDeviceLocale().getUnitTypeForLocale()
            false -> unitType
        }
        numberFormat = NumberFormat.getNumberInstance(locale)
    }

    /**
     * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
     *
     * @param distance in meters
     * @return SpannableString representation which has a bolded number and units which have a
     * relative size of .65 times the size of the number
     */
    override fun formatDistance(distance: Double): SpannableString {
        val distanceSmallUnit =
            TurfConversion.convertLength(distance, UNIT_METERS, smallUnit)
        val distanceLargeUnit =
            TurfConversion.convertLength(distance, UNIT_METERS, largeUnit)

        return when {
            // If the distance is greater than 10 miles/kilometers, then round to nearest mile/kilometer
            distanceLargeUnit > LARGE_UNIT_THRESHOLD -> {
                getDistanceString(roundToDecimalPlace(distanceLargeUnit, 0), largeUnit)
            }
            // If the distance is less than 401 feet/meters, round by fifty feet/meters
            distanceSmallUnit < SMALL_UNIT_THRESHOLD -> {
                getDistanceString(roundToClosestIncrement(distanceSmallUnit), smallUnit)
            }
            // If the distance is between 401 feet/meters and 10 miles/kilometers, then round to one decimal place
            else -> {
                getDistanceString(roundToDecimalPlace(distanceLargeUnit, 1), largeUnit)
            }
        }
    }

    /**
     * Returns number rounded to closest specified rounding increment, unless the number is less than
     * the rounding increment, then the rounding increment is returned
     *
     * @param distance to round to closest specified rounding increment
     * @return number rounded to closest rounding increment, or rounding increment if distance is less
     */
    private fun roundToClosestIncrement(distance: Double): String {
        val roundedNumber = Math.round(distance).toInt() / roundingIncrement * roundingIncrement

        return (if (roundedNumber < roundingIncrement) roundingIncrement else roundedNumber).toString()
    }

    /**
     * Rounds given number to the given decimal place
     *
     * @param distance to round
     * @param decimalPlace number of decimal places to round
     * @return distance rounded to given decimal places
     */
    private fun roundToDecimalPlace(distance: Double, decimalPlace: Int): String {
        numberFormat.maximumFractionDigits = decimalPlace

        return numberFormat.format(distance)
    }

    /**
     * Takes in a distance and units and returns a formatted SpannableString where the number is bold
     * and the unit is shrunked to .65 times the size
     *
     * @param distance formatted with appropriate decimal places
     * @param unit string from TurfConstants. This will be converted to the abbreviated form.
     * @return String with bolded distance and shrunken units
     */
    private fun getDistanceString(distance: String, unit: String): SpannableString {
        val spannableString = SpannableString("$distance ${unitStrings[unit]}")

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            distance.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
            RelativeSizeSpan(0.65f), distance.length + 1,
            spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    companion object {
        private const val LARGE_UNIT_THRESHOLD = 10
        private const val SMALL_UNIT_THRESHOLD = 401
    }
}
