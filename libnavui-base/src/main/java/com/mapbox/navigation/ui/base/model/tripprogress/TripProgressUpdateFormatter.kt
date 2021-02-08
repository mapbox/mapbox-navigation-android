package com.mapbox.navigation.ui.base.model.tripprogress

import android.content.Context
import android.text.SpannableString
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
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
        ValueFormatter<TripProgressUpdate, SpannableString>,
    private val distanceRemainingFormatter: ValueFormatter<TripProgressUpdate, SpannableString>,
    private val timeRemainingFormatter: ValueFormatter<TripProgressUpdate, SpannableString>,
    private val percentRouteTraveledFormatter: ValueFormatter<TripProgressUpdate, SpannableString>
) {

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
     * @param update a [TripProgressUpdate]
     *
     * @return the formatted result
     */
    fun getEstimatedTimeToArrival(update: TripProgressUpdate) =
        estimatedTimeToArrivalFormatter.format(update)

    /**
     * Formats the distance remaining
     *
     * @param update a [TripProgressUpdate]
     *
     * @return the formatted result
     */
    fun getDistanceRemaining(update: TripProgressUpdate) =
        distanceRemainingFormatter.format(update)

    /**
     * Formats the time remaining
     *
     * @param update a [TripProgressUpdate]
     *
     * @return the formatted result
     */
    fun getTimeRemaining(update: TripProgressUpdate) =
        timeRemainingFormatter.format(update)

    /**
     * Formats the percent distance traveled
     *
     * @param update a [TripProgressUpdate]
     *
     * @return the formatted result
     */
    fun getPercentRouteTraveled(update: TripProgressUpdate) =
        percentRouteTraveledFormatter.format(update)

    /**
     * Builds an instance of [TripProgressUpdateFormatter]
     *
     * @param context a instance of a []Context]
     */
    class Builder(private val context: Context) {
        private var estimatedTimeToArrivalFormatter:
            ValueFormatter<TripProgressUpdate, SpannableString>? = null
        private var distanceRemainingFormatter:
            ValueFormatter<TripProgressUpdate, SpannableString>? = null
        private var timeRemainingFormatter:
            ValueFormatter<TripProgressUpdate, SpannableString>? = null
        private var percentRouteTraveledFormatter:
            ValueFormatter<TripProgressUpdate, SpannableString>? = null

        /**
         * A class that formats a [TripProgressUpdate] to a [SpannableString] representing
         * the estimated time to arrival.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun estimatedTimeToArrivalFormatter(
            formatter: ValueFormatter<TripProgressUpdate, SpannableString>
        ): Builder =
            apply { this.estimatedTimeToArrivalFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdate] to a [SpannableString] representing
         * the distance remaining of a route.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun distanceRemainingFormatter(
            formatter: ValueFormatter<TripProgressUpdate, SpannableString>
        ): Builder =
            apply { this.distanceRemainingFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdate] to a [SpannableString] representing
         * the time remaining.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun timeRemainingFormatter(
            formatter: ValueFormatter<TripProgressUpdate, SpannableString>
        ): Builder =
            apply { this.timeRemainingFormatter = formatter }

        /**
         * A class that formats a [TripProgressUpdate] to a [SpannableString] representing
         * the percentage of the route distance traveled.
         *
         * @param formatter a formatter instance
         *
         * @return the builder
         */
        fun percentRouteTraveledFormatter(
            formatter: ValueFormatter<TripProgressUpdate, SpannableString>
        ): Builder =
            apply { this.percentRouteTraveledFormatter = formatter }

        /**
         * Applies the supplied parameters and instantiates a [TripProgressUpdateFormatter]
         *
         * @return a [TripProgressUpdateFormatter] object
         */
        fun build(): TripProgressUpdateFormatter {
            val theEstimatedTimeToArrivalFormatter:
                ValueFormatter<TripProgressUpdate, SpannableString> =
                    estimatedTimeToArrivalFormatter ?: EstimatedTimeToArrivalFormatter(
                        context.applicationContext,
                        TimeFormat.NONE_SPECIFIED
                    )

            val theDistanceRemainingFormatter: ValueFormatter<TripProgressUpdate, SpannableString> =
                distanceRemainingFormatter ?: DistanceRemainingFormatter(
                    DistanceFormatterOptions.Builder(context).build()
                )

            val theTimeRemainingFormatter: ValueFormatter<TripProgressUpdate, SpannableString> =
                timeRemainingFormatter ?: TimeRemainingFormatter(context.applicationContext)

            val thePercentRouteTraveledFormatter:
                ValueFormatter<TripProgressUpdate, SpannableString> =
                    percentRouteTraveledFormatter ?: PercentDistanceTraveledFormatter()

            return TripProgressUpdateFormatter(
                theEstimatedTimeToArrivalFormatter,
                theDistanceRemainingFormatter,
                theTimeRemainingFormatter,
                thePercentRouteTraveledFormatter
            )
        }
    }
}
