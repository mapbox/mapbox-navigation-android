package com.mapbox.navigation.core.formatter

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.mapbox.navigation.base.formatter.Rounding
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.R
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConversion
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A utility for rounding distances for displaying in view components.
 */
object MapboxDistanceUtil {

    private const val INVALID_ROUNDING_INCREMENT = 50
    private val enLanguage = Locale("en").language

    /**
     * This will recalculate and format a distance based on the parameters inputted. The value
     * will be rounded for visual display and a distance suffix like 'km' for kilometers or 'ft' for foot/feet will be included.
     *
     * @param distanceInMeters in meters
     * @param roundingIncrement used to alter the original value for display purposes
     * @param unitType indicates whether the value should be returned as metric or imperial
     * @param context a context for determining the correct value for the distance display, for example 3.5 or 3,5
     * @param locale a specified locale to use rather than the default locale provided by the context
     * @return an object containing values for displaying the formatted distance
     */
    fun formatDistance(
        distanceInMeters: Double,
        roundingIncrement: Int,
        unitType: UnitType,
        context: Context,
        locale: Locale,
    ): FormattedDistanceData {
        val formattingData = getFormattingData(
            distanceInMeters,
            roundingIncrement,
            unitType,
            locale,
        )
        val resources = context.applicationContext.resourcesWithLocale(locale)
        val unitStringSuffix = getUnitString(resources, formattingData.turfDistanceUnit)
        return FormattedDistanceData(
            formattingData.distance,
            formattingData.distanceAsString,
            unitStringSuffix,
            formattingData.unitType,
        )
    }

    private fun getFormattingData(
        distanceInMeters: Double,
        roundingIncrement: Int,
        unitType: UnitType,
        locale: Locale,
    ): FormattingData {
        return when (unitType) {
            UnitType.METRIC -> getMetricDistance(distanceInMeters, roundingIncrement, locale)
            UnitType.IMPERIAL -> {
                val distanceInMiles = TurfConversion.convertLength(
                    distanceInMeters,
                    TurfConstants.UNIT_METERS,
                    TurfConstants.UNIT_MILES,
                )
                if (locale.language == enLanguage && locale.country == "GB") {
                    getUKDistance(distanceInMiles, roundingIncrement, locale)
                } else {
                    getUSDistance(distanceInMiles, roundingIncrement, locale)
                }
            }
        }
    }

    private fun getMetricDistance(
        distanceInMeters: Double,
        roundingIncrement: Int,
        locale: Locale,
    ): FormattingData {
        return when {
            distanceInMeters !in 0.0..Double.MAX_VALUE -> smallValue(
                0.0,
                roundingIncrement,
                INVALID_ROUNDING_INCREMENT,
                TurfConstants.UNIT_METERS,
                UnitType.METRIC,
            )

            distanceInMeters < 25.0 -> smallValue(
                distanceInMeters,
                roundingIncrement,
                5,
                TurfConstants.UNIT_METERS,
                UnitType.METRIC,
            )

            distanceInMeters < 100 -> smallValue(
                distanceInMeters,
                roundingIncrement,
                25,
                TurfConstants.UNIT_METERS,
                UnitType.METRIC,
            )

            distanceInMeters < 1000.0 -> smallValue(
                distanceInMeters,
                roundingIncrement,
                50,
                TurfConstants.UNIT_METERS,
                UnitType.METRIC,
            )

            else -> {
                val distanceInKm = TurfConversion.convertLength(
                    distanceInMeters,
                    TurfConstants.UNIT_METERS,
                    TurfConstants.UNIT_KILOMETERS,
                )
                when {
                    distanceInMeters < 3000.0 -> largeValue(
                        distanceInKm,
                        1,
                        TurfConstants.UNIT_KILOMETERS,
                        UnitType.METRIC,
                        locale,
                    )

                    else -> largeValue(
                        distanceInKm,
                        0,
                        TurfConstants.UNIT_KILOMETERS,
                        UnitType.METRIC,
                        locale,
                    )
                }
            }
        }
    }

    private fun getUKDistance(
        distanceInMiles: Double,
        roundingIncrement: Int,
        locale: Locale,
    ): FormattingData {
        return when {
            distanceInMiles !in 0.0..Double.MAX_VALUE -> smallValue(
                0.0,
                roundingIncrement,
                INVALID_ROUNDING_INCREMENT,
                TurfConstants.UNIT_YARDS,
                UnitType.IMPERIAL,
            )

            distanceInMiles < 0.1 -> {
                val distanceInYards = TurfConversion.convertLength(
                    distanceInMiles,
                    TurfConstants.UNIT_MILES,
                    TurfConstants.UNIT_YARDS,
                )
                when {
                    distanceInYards < 20 -> smallValue(
                        distanceInYards,
                        roundingIncrement,
                        10,
                        TurfConstants.UNIT_YARDS,
                        UnitType.IMPERIAL,
                    )

                    distanceInYards < 100 -> smallValue(
                        distanceInYards,
                        roundingIncrement,
                        25,
                        TurfConstants.UNIT_YARDS,
                        UnitType.IMPERIAL,
                    )

                    else -> smallValue(
                        distanceInYards,
                        roundingIncrement,
                        50,
                        TurfConstants.UNIT_YARDS,
                        UnitType.IMPERIAL,
                    )
                }
            }

            distanceInMiles < 3.0 -> largeValue(
                distanceInMiles,
                1,
                TurfConstants.UNIT_MILES,
                UnitType.IMPERIAL,
                locale,
            )

            else -> largeValue(
                distanceInMiles,
                0,
                TurfConstants.UNIT_MILES,
                UnitType.IMPERIAL,
                locale,
            )
        }
    }

    private fun getUSDistance(
        distanceInMiles: Double,
        roundingIncrement: Int,
        locale: Locale,
    ): FormattingData {
        return when {
            distanceInMiles !in 0.0..Double.MAX_VALUE -> smallValue(
                0.0,
                roundingIncrement,
                INVALID_ROUNDING_INCREMENT,
                TurfConstants.UNIT_FEET,
                UnitType.IMPERIAL,
            )

            distanceInMiles < 0.1 -> {
                val distanceInFeet = TurfConversion.convertLength(
                    distanceInMiles,
                    TurfConstants.UNIT_MILES,
                    TurfConstants.UNIT_FEET,
                )
                smallValue(
                    distanceInFeet,
                    roundingIncrement,
                    50,
                    TurfConstants.UNIT_FEET,
                    UnitType.IMPERIAL,
                )
            }

            distanceInMiles < 3.0 -> largeValue(
                distanceInMiles,
                1,
                TurfConstants.UNIT_MILES,
                UnitType.IMPERIAL,
                locale,
            )

            else -> largeValue(
                distanceInMiles,
                0,
                TurfConstants.UNIT_MILES,
                UnitType.IMPERIAL,
                locale,
            )
        }
    }

    private fun smallValue(
        distance: Double,
        roundingIncrement: Int,
        defaultRoundingIncrement: Int,
        unitTypeString: String,
        unitType: UnitType,
    ): FormattingData {
        val inferredRoundingIncrement =
            if (roundingIncrement == Rounding.INCREMENT_DISTANCE_DEPENDENT) {
                defaultRoundingIncrement
            } else {
                roundingIncrement
            }
        val roundedValue = roundSmallDistance(
            distance,
            inferredRoundingIncrement,
        )
        return FormattingData(
            roundedValue.toDouble(),
            roundedValue.toString(),
            unitTypeString,
            unitType,
        )
    }

    private fun largeValue(
        distance: Double,
        maxFractionDigits: Int,
        unitTypeString: String,
        unitType: UnitType,
        locale: Locale,
    ): FormattingData {
        val roundedValue = NumberFormat.getNumberInstance(locale).also {
            it.maximumFractionDigits = maxFractionDigits
        }.format(distance)
        return FormattingData(distance, roundedValue, unitTypeString, unitType)
    }

    /**
     * This will recalculate and format a distance based on the parameters inputted. The value
     * will be rounded for visual display and a distance suffix like 'km' for kilometers or 'ft' for foot/feet will be included.
     *
     * @param distanceInMeters in meters
     * @param roundingIncrement used to alter the original value for display purposes
     * @param unitType indicates whether the value should be returned as metric or imperial
     * @param context a context for determining the correct value for the distance display, for example 3.5 or 3,5
     * @return an object containing values for displaying the formatted distance
     */
    fun formatDistance(
        distanceInMeters: Double,
        roundingIncrement: Int,
        unitType: UnitType,
        context: Context,
    ): FormattedDistanceData {
        return formatDistance(
            distanceInMeters,
            roundingIncrement,
            unitType,
            context,
            Locale.getDefault(),
        )
    }

    /**
     * This will recalculate and format a distance based on the parameters inputted. The value
     * will be rounded for visual display and a distance suffix like 'km' for kilometers or 'ft' for foot/feet will be included.
     *
     * @param distanceInMeters in meters
     * @param roundingIncrement used to alter the original value for display purposes
     * @param unitType indicates whether the value should be returned as metric or imperial
     * @param locale a specified locale to use rather than the default locale provided by the context
     * @return an object containing values for displaying the formatted distance
     */
    fun formatDistance(
        distanceInMeters: Double,
        roundingIncrement: Int,
        unitType: UnitType,
        locale: Locale,
    ): Double {
        return getFormattingData(distanceInMeters, roundingIncrement, unitType, locale).distance
    }

    /**
     * This will recalculate and format a distance based on the parameters inputted. The value
     * will be rounded for visual display and a distance suffix like 'km' for kilometers or 'ft' for foot/feet will be included.
     *
     * @param distanceInMeters in meters
     * @param roundingIncrement used to alter the original value for display purposes
     * @param unitType indicates whether the value should be returned as metric or imperial
     * @return an object containing values for displaying the formatted distance
     */
    fun formatDistance(
        distanceInMeters: Double,
        roundingIncrement: Int,
        unitType: UnitType,
    ): Double {
        return formatDistance(distanceInMeters, roundingIncrement, unitType, Locale.getDefault())
    }

    private fun roundSmallDistance(
        distance: Double,
        roundingIncrement: Int,
    ): Int {
        if (distance < 0) {
            return 0
        }

        val roundedValue = if (roundingIncrement > 0) {
            val roundedDistance = distance.roundToInt()
            if (roundedDistance < roundingIncrement) {
                roundingIncrement
            } else {
                roundedDistance / roundingIncrement * roundingIncrement
            }
        } else {
            distance
        }
        return roundedValue.toInt()
    }

    private fun getUnitString(resources: Resources, @TurfConstants.TurfUnitCriteria unit: String) =
        when (unit) {
            TurfConstants.UNIT_KILOMETERS -> resources.getString(R.string.mapbox_unit_kilometers)
            TurfConstants.UNIT_METERS -> resources.getString(R.string.mapbox_unit_meters)
            TurfConstants.UNIT_MILES -> resources.getString(R.string.mapbox_unit_miles)
            TurfConstants.UNIT_FEET -> resources.getString(R.string.mapbox_unit_feet)
            TurfConstants.UNIT_YARDS -> resources.getString(R.string.mapbox_unit_yards)
            else -> ""
        }

    private fun Context.resourcesWithLocale(locale: Locale?): Resources {
        val config = Configuration(this.resources.configuration).also {
            it.setLocale(locale)
        }
        return this.createConfigurationContext(config).resources
    }

    private data class FormattingData(
        val distance: Double,
        val distanceAsString: String,
        val turfDistanceUnit: String,
        val unitType: UnitType,
    )
}
