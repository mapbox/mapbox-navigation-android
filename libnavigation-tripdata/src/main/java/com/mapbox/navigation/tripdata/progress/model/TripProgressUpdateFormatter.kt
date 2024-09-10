package com.mapbox.navigation.tripdata.progress.model

import android.content.Context
import android.text.SpannableString
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.ui.base.formatter.ValueFormatter

/**
 * Contains various trip related formatters
 *
 * @param estimatedTimeToArrivalFormatter for formatting the estimated time to arrival
 * @param distanceRemainingFormatter for formatting the trip distance remaining
 * @param timeRemainingFormatter for formatting the travel time remaining
 * @param percentRouteTraveledFormatter for formatting the percent distance traveled
 */
class TripProgressUpdateFormatter private constructor(
    private val estimatedTimeToArrivalFormatter:
        ValueFormatter<Long, SpannableString>,
    private val distanceRemainingFormatter: ValueFormatter<Double, SpannableString>,
    private val timeRemainingFormatter: ValueFormatter<Double, SpannableString>,
    private val percentRouteTraveledFormatter: ValueFormatter<Double, SpannableString>,
) {

    private companion object {
        private const val DEFAULT_ROUNDING_IMPERIAL = 5
        private const val DEFAULT_ROUNDING_METRIC = 2
    }

    /**
     * @param context a valid context
     *
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(context: Context): Builder {
        return Builder(context)
            .estimatedTimeToArrivalFormatter(estimatedTimeToArrivalFormatter)
            .distanceRemainingFormatter(distanceRemainingFormatter)
            .timeRemainingFormatter(timeRemainingFormatter)
            .percentRouteTraveledFormatter(percentRouteTraveledFormatter)
    }

    /**
     * An implementation of equals
     *
     * @param other any object to compare equality
     *
     * @return an equality indication as a boolean
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TripProgressUpdateFormatter

        if (distanceRemainingFormatter != other.distanceRemainingFormatter) return false
        if (estimatedTimeToArrivalFormatter != other.estimatedTimeToArrivalFormatter) return false
        if (percentRouteTraveledFormatter != other.percentRouteTraveledFormatter) return false
        if (timeRemainingFormatter != other.timeRemainingFormatter) return false

        return true
    }

    /**
     * @return the object as a string
     */
    override fun toString(): String {
        return "TripProgressUpdateFormatter(" +
            "estimatedTimeToArrivalFormatter=$estimatedTimeToArrivalFormatter, " +
            "distanceRemainingFormatter=$distanceRemainingFormatter, " +
            "timeRemainingFormatter=$timeRemainingFormatter, " +
            "percentRouteTraveledFormatter=$percentRouteTraveledFormatter)"
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = estimatedTimeToArrivalFormatter.hashCode()
        result = 31 * result + distanceRemainingFormatter.hashCode()
        result = 31 * result + timeRemainingFormatter.hashCode()
        result = 31 * result + percentRouteTraveledFormatter.hashCode()
        return result
    }

    /**
     * Formats the estimated time until arrival
     *
     * @param value the estimated time to arrival in milliseconds
     *
     * @return the formatted result
     */
    fun getEstimatedTimeToArrival(value: Long) =
        estimatedTimeToArrivalFormatter.format(value)

    /**
     * Formats the distance remaining
     *
     * @param value the distance remaining
     *
     * @return the formatted result
     */
    fun getDistanceRemaining(value: Double) =
        distanceRemainingFormatter.format(value)

    /**
     * Formats the time remaining
     *
     * @param value the time remaining
     *
     * @return the formatted result
     */
    fun getTimeRemaining(value: Double) =
        timeRemainingFormatter.format(value)

    /**
     * Formats the percent distance traveled
     *
     * @param value the percent distance traveled
     *
     * @return the formatted result
     */
    fun getPercentRouteTraveled(value: Double) =
        percentRouteTraveledFormatter.format(value)

    /**
     * Builds an instance of [TripProgressUpdateFormatter]
     *
     * @param context a instance of a []Context]
     */
    class Builder(private val context: Context) {
        private var estimatedTimeToArrivalFormatter:
            ValueFormatter<Long, SpannableString>? = null
        private var distanceRemainingFormatter:
            ValueFormatter<Double, SpannableString>? = null
        private var timeRemainingFormatter:
            ValueFormatter<Double, SpannableString>? = null
        private var percentRouteTraveledFormatter:
            ValueFormatter<Double, SpannableString>? = null

        /**
         * A class that formats a [TripProgressUpdateValue] to a [SpannableString] representing
         * the estimated time to arrival.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun estimatedTimeToArrivalFormatter(
            formatter: ValueFormatter<Long, SpannableString>,
        ): Builder =
            apply { this.estimatedTimeToArrivalFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdateValue] to a [SpannableString] representing
         * the distance remaining of a route.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun distanceRemainingFormatter(
            formatter: ValueFormatter<Double, SpannableString>,
        ): Builder =
            apply { this.distanceRemainingFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdateValue] to a [SpannableString] representing
         * the time remaining.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun timeRemainingFormatter(
            formatter: ValueFormatter<Double, SpannableString>,
        ): Builder =
            apply { this.timeRemainingFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdateValue] to a [SpannableString] representing
         * the percentage of the route distance traveled.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun percentRouteTraveledFormatter(
            formatter: ValueFormatter<Double, SpannableString>,
        ): Builder =
            apply { this.percentRouteTraveledFormatter = formatter }

        /**
         * Applies the supplied parameters and instantiates a [TripProgressUpdateFormatter]
         *
         * @return a [TripProgressUpdateFormatter] object
         */
        fun build(): TripProgressUpdateFormatter {
            val theEstimatedTimeToArrivalFormatter: ValueFormatter<Long, SpannableString> =
                estimatedTimeToArrivalFormatter
                    ?: EstimatedTimeToArrivalFormatter(
                        context.applicationContext,
                        TimeFormat.NONE_SPECIFIED,
                    )

            val theDistanceRemainingFormatter: ValueFormatter<Double, SpannableString> =
                distanceRemainingFormatter ?: getDefaultDistanceRemainingFormatter(context)

            val theTimeRemainingFormatter: ValueFormatter<Double, SpannableString> =
                timeRemainingFormatter ?: TimeRemainingFormatter(context.applicationContext)

            val thePercentRouteTraveledFormatter: ValueFormatter<Double, SpannableString> =
                percentRouteTraveledFormatter ?: PercentDistanceTraveledFormatter()

            return TripProgressUpdateFormatter(
                theEstimatedTimeToArrivalFormatter,
                theDistanceRemainingFormatter,
                theTimeRemainingFormatter,
                thePercentRouteTraveledFormatter,
            )
        }

        private fun getDefaultDistanceRemainingFormatter(context: Context):
            ValueFormatter<Double, SpannableString> {
            val options = DistanceFormatterOptions.Builder(context).build()
            val roundingUnit = when (options.unitType) {
                UnitType.IMPERIAL -> DEFAULT_ROUNDING_IMPERIAL
                UnitType.METRIC -> DEFAULT_ROUNDING_METRIC
            }
            val finalOptions = options.toBuilder().roundingIncrement(roundingUnit).build()
            return DistanceRemainingFormatter(finalOptions)
        }
    }
}
