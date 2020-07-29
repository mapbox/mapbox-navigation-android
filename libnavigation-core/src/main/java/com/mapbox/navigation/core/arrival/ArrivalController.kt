package com.mapbox.navigation.core.arrival

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.MapboxNavigation

/**
 * When navigating to points of interest, you may want to control the arrival experience.
 * This interface gives you options to control arrival via [MapboxNavigation.setArrivalController].
 *
 * To observe arrival, see [ArrivalObserver]
 */
interface ArrivalController {

    /**
     * Override the options for your arrival callback.
     */
    fun arrivalOptions(): ArrivalOptions

    /**
     * Based on your [ArrivalOptions], this will be called as the next stop is approached.
     * To manually navigate to the next leg, return false and call [MapboxNavigation.navigateNextRouteLeg].
     *
     * @return true to automatically call [MapboxNavigation.navigateNextRouteLeg], false to do it manually
     */
    fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean
}

/**
 * The default controller for arrival. This will move onto the next leg automatically
 * if there is one.
 */
class AutoArrivalController : ArrivalController {

    /**
     * Default arrival options.
     */
    override fun arrivalOptions(): ArrivalOptions = ArrivalOptions.Builder().build()

    /**
     * By default this will move onto the next step.
     */
    override fun navigateNextRouteLeg(routeLegProgress: RouteLegProgress): Boolean {
        return true
    }
}

/**
 * Choose when to be notified of arrival.
 *
 * @param arrivalInSeconds While the next stop is less than [arrivalInSeconds] away,
 * [ArrivalController.navigateNextRouteLeg] will be called
 * @param arrivalInMeters While the next stop is less than [arrivalInMeters] away,
 * [ArrivalController.navigateNextRouteLeg] will be called
 */
class ArrivalOptions private constructor(
    val arrivalInSeconds: Double?,
    val arrivalInMeters: Double?
) {
    /**
     * @return the builder that created the [ArrivalOptions]
     */
    fun toBuilder() = Builder().apply {
        arrivalInSeconds(arrivalInSeconds)
        arrivalInMeters(arrivalInMeters)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrivalOptions

        if (arrivalInSeconds != other.arrivalInSeconds) return false
        if (arrivalInMeters != other.arrivalInMeters) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = arrivalInSeconds?.hashCode() ?: 0
        result = 31 * result + (arrivalInMeters?.hashCode() ?: 0)
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ArrivalOptions(arrivalInSeconds=$arrivalInSeconds, arrivalInMeters=$arrivalInMeters)"
    }

    /**
     * Build your [ArrivalOptions].
     */
    class Builder {

        private var arrivalInSeconds: Double? = 5.0
        private var arrivalInMeters: Double? = 40.0

        /**
         * (Recommended) Use time estimation for arrival, arrival is influenced by traffic conditions.
         * Arrive when the estimated time to a stop is less than or equal to this threshold.
         */
        fun arrivalInSeconds(arriveInSeconds: Double?): Builder {
            this.arrivalInSeconds = arriveInSeconds
            return this
        }

        /**
         * Arrive when the estimated distance to a stop is less than or equal to this threshold.
         */
        fun arrivalInMeters(arriveInMeters: Double?): Builder {
            this.arrivalInMeters = arriveInMeters
            return this
        }

        /**
         * Build the object. If you want to disable this feature set *null* in [MapboxNavigation.setArrivalController].
         */
        fun build(): ArrivalOptions {
            check(arrivalInSeconds != null || arrivalInSeconds != null) {
                "Choose a method to be notified of arrival, time and/or distance."
            }
            return ArrivalOptions(
                arrivalInSeconds = arrivalInSeconds,
                arrivalInMeters = arrivalInMeters
            )
        }
    }
}
