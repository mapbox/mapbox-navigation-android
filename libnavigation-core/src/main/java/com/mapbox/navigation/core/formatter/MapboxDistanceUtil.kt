package com.mapbox.navigation.core.formatter

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
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

    private const val smallDistanceUpperThresholdInMeters = 400.0
    private const val mediumDistanceUpperThresholdInMeters = 10000.0

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
        locale: Locale
    ): FormattedDistanceData {
        return when (distanceInMeters) {
            !in 0.0..Double.MAX_VALUE -> {
                formatDistanceAndSuffixForSmallUnit(
                    0.0,
                    roundingIncrement,
                    unitType,
                    context,
                    locale
                )
            }
            in 0.0..smallDistanceUpperThresholdInMeters -> {
                formatDistanceAndSuffixForSmallUnit(
                    distanceInMeters,
                    roundingIncrement,
                    unitType,
                    context,
                    locale
                )
            }
            in smallDistanceUpperThresholdInMeters..mediumDistanceUpperThresholdInMeters -> {
                formatDistanceAndSuffixForLargeUnit(distanceInMeters, 1, unitType, context, locale)
            }
            else -> {
                formatDistanceAndSuffixForLargeUnit(distanceInMeters, 0, unitType, context, locale)
            }
        }
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
        context: Context
    ): FormattedDistanceData {
        return formatDistance(
            distanceInMeters,
            roundingIncrement,
            unitType,
            context,
            Locale.getDefault()
        )
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
        unitType: UnitType
    ): Double {
        return when (distanceInMeters) {
            !in 0.0..Double.MAX_VALUE -> {
                roundSmallDistance(distanceInMeters, roundingIncrement, unitType).toDouble()
            }
            in 0.0..smallDistanceUpperThresholdInMeters -> {
                roundSmallDistance(distanceInMeters, roundingIncrement, unitType).toDouble()
            }
            in smallDistanceUpperThresholdInMeters..mediumDistanceUpperThresholdInMeters -> {
                roundLargeDistance(distanceInMeters, unitType)
            }
            else -> {
                roundLargeDistance(distanceInMeters, unitType)
            }
        }
    }

    private fun roundSmallDistance(
        distance: Double,
        roundingIncrement: Int,
        unitType: UnitType
    ): Int {
        if (distance < 0) {
            return 0
        }

        val distanceUnit = TurfConversion.convertLength(
            distance,
            TurfConstants.UNIT_METERS,
            getSmallTurfUnitType(unitType)
        )

        val roundedValue = if (roundingIncrement > 0) {
            val roundedDistance = distanceUnit.roundToInt()
            if (roundedDistance < roundingIncrement) {
                roundingIncrement
            } else {
                roundedDistance / roundingIncrement * roundingIncrement
            }
        } else {
            distanceUnit
        }
        return roundedValue.toInt()
    }

    private fun roundLargeDistance(
        distance: Double,
        unitType: UnitType
    ): Double {
        return TurfConversion.convertLength(
            distance,
            TurfConstants.UNIT_METERS,
            getLargeTurfUnitType(unitType)
        )
    }

    private fun formatDistanceAndSuffixForSmallUnit(
        distance: Double,
        roundingIncrement: Int,
        unitType: UnitType,
        context: Context,
        locale: Locale
    ): FormattedDistanceData {
        val resources = context.applicationContext.resourcesWithLocale(locale)
        val unitStringSuffix = getUnitString(resources, getSmallTurfUnitType(unitType))
        if (distance < 0) {
            return FormattedDistanceData(0.0, "0", unitStringSuffix, unitType)
        }
        val roundedValue = roundSmallDistance(
            distance,
            roundingIncrement,
            unitType
        )

        return FormattedDistanceData(
            roundedValue.toDouble(),
            roundedValue.toString(),
            unitStringSuffix,
            unitType
        )
    }

    private fun formatDistanceAndSuffixForLargeUnit(
        distance: Double,
        maxFractionDigits: Int,
        unitType: UnitType,
        context: Context,
        locale: Locale
    ): FormattedDistanceData {
        val resources = context.applicationContext.resourcesWithLocale(locale)
        val unitStringSuffix = getUnitString(resources, getLargeTurfUnitType(unitType))
        val distanceUnit = roundLargeDistance(
            distance,
            unitType
        )
        val roundedValue = NumberFormat.getNumberInstance(locale).also {
            it.maximumFractionDigits = maxFractionDigits
        }.format(distanceUnit)

        return FormattedDistanceData(distanceUnit, roundedValue, unitStringSuffix, unitType)
    }

    private fun getSmallTurfUnitType(unitType: UnitType): String {
        return when (unitType) {
            UnitType.IMPERIAL -> TurfConstants.UNIT_FEET
            UnitType.METRIC -> TurfConstants.UNIT_METERS
        }
    }

    private fun getLargeTurfUnitType(unitType: UnitType): String {
        return when (unitType) {
            UnitType.IMPERIAL -> TurfConstants.UNIT_MILES
            UnitType.METRIC -> TurfConstants.UNIT_KILOMETERS
        }
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
