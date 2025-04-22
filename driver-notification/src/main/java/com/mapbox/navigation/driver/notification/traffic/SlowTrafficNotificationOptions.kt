package com.mapbox.navigation.driver.notification.traffic

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration options for the `SlowTrafficNotificationProvider`.
 *
 * This class allows customization of the behavior of the `SlowTrafficNotificationProvider`,
 * including thresholds for detecting slow traffic and the frequency of checks.
 *
 * @property slowTrafficCongestionRange the range of congestion levels considered as slow traffic.
 * Values within this range will trigger slow traffic detection.
 * @property slowTrafficPeriodCheck the interval at which slow traffic conditions are checked.
 * @property trafficDelay the minimum delay caused by slow traffic to trigger a notification.
 *
 * @see [SlowTrafficNotificationProvider] for the provider that uses these options.
 */
class SlowTrafficNotificationOptions private constructor(
    val slowTrafficCongestionRange: IntRange,
    val slowTrafficPeriodCheck: Duration,
    val trafficDelay: Duration,
) {

    /**
     * Creates a builder instance initialized with the current configuration.
     */
    fun toBuilder(): Builder {
        return Builder()
            .slowTrafficCongestionRange(slowTrafficCongestionRange)
            .slowTrafficPeriodCheck(slowTrafficPeriodCheck)
            .trafficDelay(trafficDelay)
    }

    /**
     * Compares this `SlowTrafficNotificationOptions` instance with another object for equality.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlowTrafficNotificationOptions

        if (slowTrafficCongestionRange != other.slowTrafficCongestionRange) return false
        if (slowTrafficPeriodCheck != other.slowTrafficPeriodCheck) return false
        if (trafficDelay != other.trafficDelay) return false

        return true
    }

    /**
     * Generates a hash code for the `SlowTrafficNotificationOptions` instance.
     */
    override fun hashCode(): Int {
        var result = slowTrafficCongestionRange.hashCode()
        result = 31 * result + slowTrafficPeriodCheck.hashCode()
        result = 31 * result + trafficDelay.hashCode()
        return result
    }

    /**
     * Returns a string representation of the `SlowTrafficNotificationOptions` instance.
     */
    override fun toString(): String {
        return "SlowTrafficNotificationOptions(" +
            "slowTrafficCongestionRange=$slowTrafficCongestionRange, " +
            "slowTrafficPeriodCheck=$slowTrafficPeriodCheck, " +
            "trafficDelay=$trafficDelay" +
            ")"
    }

    class Builder {

        private var slowTrafficCongestionRange: IntRange = 60..100
        private var slowTrafficPeriodCheck: Duration = 10.seconds
        private var trafficDelay: Duration = 2.minutes

        /**
         * Sets the range of congestion levels considered as slow traffic.
         */
        fun slowTrafficCongestionRange(range: IntRange): Builder = apply {
            this.slowTrafficCongestionRange = range
        }

        /**
         * Sets the interval at which slow traffic conditions are checked.

         */
        fun slowTrafficPeriodCheck(period: Duration): Builder = apply {
            this.slowTrafficPeriodCheck = period
        }

        /**
         * Sets the minimum delay caused by slow traffic to trigger a notification.
         */
        fun trafficDelay(delay: Duration): Builder = apply {
            this.trafficDelay = delay
        }

        /**
         * Builds a new `SlowTrafficNotificationOptions` instance with the configured values.
         */
        fun build(): SlowTrafficNotificationOptions {
            return SlowTrafficNotificationOptions(
                slowTrafficCongestionRange,
                slowTrafficPeriodCheck,
                trafficDelay,
            )
        }
    }
}
