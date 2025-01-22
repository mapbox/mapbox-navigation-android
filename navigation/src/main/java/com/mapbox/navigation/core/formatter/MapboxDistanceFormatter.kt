package com.mapbox.navigation.core.formatter

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions

/**
 * Implementation of DistanceFormatter, which can format distances in meters
 * based on a language format and unit type.
 *
 * @param options to build the [MapboxDistanceFormatter]
 */
class MapboxDistanceFormatter(
    val options: DistanceFormatterOptions,
) : DistanceFormatter {

    /**
     * Returns a formatted SpannableString with bold and size formatting. I.e., "10 mi", "350 m"
     *
     * @param distance in meters
     * @return SpannableString representation which has a bolded number and units which have a
     * relative size of .65 times the size of the number
     */
    override fun formatDistance(distance: Double): SpannableString {
        return MapboxDistanceUtil.formatDistance(
            distance,
            options.roundingIncrement,
            options.unitType,
            options.applicationContext,
            options.locale,
        ).run {
            getSpannableDistanceString(Pair(this.distanceAsString, this.distanceSuffix))
        }
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
        distanceAndSuffix: Pair<String, String>,
    ): SpannableString {
        val spannableString =
            SpannableString("${distanceAndSuffix.first} ${distanceAndSuffix.second}")

        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            0,
            distanceAndSuffix.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannableString.setSpan(
            RelativeSizeSpan(0.75f),
            distanceAndSuffix.first.length + 1,
            spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        return spannableString
    }
}
