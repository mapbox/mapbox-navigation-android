package com.mapbox.navigation.core

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.VoiceUnit
import com.mapbox.navigation.trip.notification.R
import com.mapbox.navigation.trip.notification.utils.extensions.getUnitTypeForLocale
import com.mapbox.navigation.utils.extensions.inferDeviceLocale
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConversion
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class MapboxDistanceFormatter(private val context: Context, unitType: String, private val roundingIncrement: Int) : DistanceFormatter {

    // These values are in meters.
    private val smallDistanceUpperThresholdInMeters = 400.0
    private val mediumDistanceUpperThresholdInMeters = 10000.0

    private val smallUnit = when (unitType) {
        IMPERIAL -> TurfConstants.UNIT_FEET
        else -> TurfConstants.UNIT_METERS
    }

    private val largeUnit = when (unitType) {
        IMPERIAL -> TurfConstants.UNIT_MILES
        else -> TurfConstants.UNIT_KILOMETERS
    }

    private val locale: Locale by lazy {
        context.inferDeviceLocale()
    }

    companion object {
        @JvmStatic
        fun builder(context: Context): Builder = Builder(context)
    }

    class Builder(private val context: Context) {
        private var unitType: String? = null
        private var roundingIncrement = 0

        fun withUnitType(@VoiceUnit unitType: String) =
                apply { this.unitType = unitType }

        fun withRoundingIncrement(roundingIncrement: Int) =
                apply { this.roundingIncrement = roundingIncrement }

        fun build(): MapboxDistanceFormatter {
            val unitTypeToUse = if (unitType.isNullOrEmpty()) {
                context.inferDeviceLocale().getUnitTypeForLocale()
            } else {
                unitType!!
            }

            return MapboxDistanceFormatter(
                    context,
                    unitTypeToUse,
                    roundingIncrement)
        }
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
        val unitStringSuffix = getUnitString(context, smallUnit)
        val roundedNumber = distanceUnit.roundToInt() / roundingIncrement * roundingIncrement
        val roundedValue = (if (roundedNumber < roundingIncrement) roundingIncrement else roundedNumber).toString()
        return Pair(roundedValue, unitStringSuffix)
    }

    private fun formatDistanceAndSuffixForLargeUnit(distance: Double, maxFractionDigits: Int): Pair<String, String> {
        val unitStringSuffix = getUnitString(context, largeUnit)
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
     * @param distance formatted with appropriate decimal places
     * @param unit string from TurfConstants. This will be converted to the abbreviated form.
     * @return String with bolded distance and shrunken units
     */
    internal fun getSpannableDistanceString(distanceAndSuffix: Pair<String, String>): SpannableString {
        val spannableString = SpannableString("${distanceAndSuffix.first} ${distanceAndSuffix.second}")

        spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                distanceAndSuffix.first.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableString.setSpan(
                RelativeSizeSpan(0.65f), distanceAndSuffix.first.length + 1,
                spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }

    private fun getUnitString(context: Context, @TurfConstants.TurfUnitCriteria unit: String) = when (unit) {
        TurfConstants.UNIT_KILOMETERS -> context.getString(R.string.kilometers)
        TurfConstants.UNIT_METERS -> context.getString(R.string.meters)
        TurfConstants.UNIT_MILES -> context.getString(R.string.miles)
        TurfConstants.UNIT_FEET -> context.getString(R.string.feet)
        else -> ""
    }
}
