package com.mapbox.services.android.navigation.v5.utils

import android.content.Context
import android.graphics.Typeface
import android.location.Location
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.extensions.getUnitTypeForLocale
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.services.android.navigation.R
import com.mapbox.services.android.navigation.v5.internal.navigation.routeprogress.MetricsRouteProgress
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants
import com.mapbox.turf.TurfConstants.UNIT_FEET
import com.mapbox.turf.TurfConstants.UNIT_KILOMETERS
import com.mapbox.turf.TurfConstants.UNIT_METERS
import com.mapbox.turf.TurfConstants.UNIT_MILES
import com.mapbox.turf.TurfConversion
import com.mapbox.turf.TurfMeasurement
import java.text.NumberFormat
import java.util.HashMap
import java.util.Locale

/**
 * Creates an instance of DistanceFormatter, which can format distances in meters
 * based on a language format and unit type.
 *
 *
 * This constructor will infer device language and unit type using the device locale.
 *
 * @param context from which to get localized strings from
 * @param language for which language
 * @param unitType to use, or NONE_SPECIFIED to use default for locale country
 * @param roundingIncrement increment by which to round small distances
 */
class DistanceFormatter(
    context: Context,
    language: String?,
    @DirectionsCriteria.VoiceUnitCriteria unitType: String,
    @param:NavigationConstants.RoundingIncrement @field:NavigationConstants.RoundingIncrement
    private val roundingIncrement: Int
) {
    private val unitStrings = HashMap<String, String>()
    private val numberFormat: NumberFormat
    private val largeUnit: String
    private val smallUnit: String
    private val language: String
    private val unitType: String

    companion object {

        private const val LARGE_UNIT_THRESHOLD = 10
        private const val SMALL_UNIT_THRESHOLD = 401

        @JvmStatic
        fun calculateAbsoluteDistance(
            currentLocation: Location,
            metricProgress: MetricsRouteProgress
        ): Int {
            val currentPoint = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)
            val finalPoint = metricProgress.directionsRouteDestination

            return TurfMeasurement.distance(currentPoint, finalPoint, UNIT_METERS)
                .toInt()
        }
    }

    init {
        unitStrings[UNIT_KILOMETERS] = context.getString(R.string.kilometers)
        unitStrings[UNIT_METERS] = context.getString(R.string.meters)
        unitStrings[UNIT_MILES] = context.getString(R.string.miles)
        unitStrings[UNIT_FEET] = context.getString(R.string.feet)

        val locale = if (language == null) {
            context.inferDeviceLocale()
        } else {
            Locale(language)
        }
        this.language = locale.language
        numberFormat = NumberFormat.getNumberInstance(locale)

        this.unitType =
            if (DirectionsCriteria.IMPERIAL != unitType && DirectionsCriteria.METRIC != unitType) {
                context.inferDeviceLocale().getUnitTypeForLocale()
            } else {
                unitType
            }

        largeUnit = if (DirectionsCriteria.IMPERIAL == unitType) UNIT_MILES else UNIT_KILOMETERS
        smallUnit = if (DirectionsCriteria.IMPERIAL == unitType) UNIT_FEET else UNIT_METERS
    }

    /**
     * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
     *
     * @param distance in meters
     * @return SpannableString representation which has a bolded number and units which have a
     * relative size of .65 times the size of the number
     */
    fun formatDistance(distance: Double): SpannableString {
        val distanceSmallUnit =
            TurfConversion.convertLength(distance, UNIT_METERS, smallUnit)
        val distanceLargeUnit =
            TurfConversion.convertLength(distance, UNIT_METERS, largeUnit)

        // If the distance is greater than 10 miles/kilometers, then round to nearest mile/kilometer
        return if (distanceLargeUnit > LARGE_UNIT_THRESHOLD) {
            getDistanceString(roundToDecimalPlace(distanceLargeUnit, 0), largeUnit)
            // If the distance is less than 401 feet/meters, round by fifty feet/meters
        } else if (distanceSmallUnit < SMALL_UNIT_THRESHOLD) {
            getDistanceString(roundToClosestIncrement(distanceSmallUnit), smallUnit)
            // If the distance is between 401 feet/meters and 10 miles/kilometers, then round to one decimal place
        } else {
            getDistanceString(roundToDecimalPlace(distanceLargeUnit, 1), largeUnit)
        }
    }

    /**
     * Method that can be used to check if an instance of [DistanceFormatter]
     * needs to be updated based on the passed language / unitType.
     *
     * @param language to check against the current formatter language
     * @param unitType to check against the current formatter unitType
     * @return true if new formatter is needed, false otherwise
     */
    fun shouldUpdate(language: String, unitType: String, roundingIncrement: Int): Boolean {
        return (this.language != language || this.unitType != unitType ||
            this.roundingIncrement != roundingIncrement)
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
        val spannableString = SpannableString(String.format("%s %s", distance, unitStrings[unit]))

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
}
