package com.mapbox.navigation.core.stops

import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation

/**
 * When navigating to points of interest, you may want to control the arrival
 * experience. This interface gives you options to change arrival via [MapboxNavigation.attachArrivalController]
 */
interface ArrivalController {

    /**
     * Override the options for your arrival callback.
     */
    fun arrivalOptions(): ArrivalOptions

    /**
     * Based on your [ArrivalOptions], this will be called as the next stop is approached.
     * To manually navigate the next leg, return false and call [MapboxNavigation.navigateNextRouteLeg]
     *
     * @return true to automatically move to the next step, false to do it manually
     */
    fun onStopArrival(routeLegProgress: RouteLegProgress): Boolean

    /**
     * Once the [RouteProgress.currentState] has reached [RouteProgressState.ROUTE_ARRIVED]
     * for the last stop, this will be called once.
     */
    fun onRouteArrival(routeProgress: RouteProgress) {}
}

/**
 * The default controller for arrival. This will move onto the next leg automatically
 * if there is one.
 */
class AutoArrivalController : ArrivalController {

    /**
     * Default arrival options
     */
    override fun arrivalOptions(): ArrivalOptions = ArrivalOptions.Builder().build()

    /**
     * By default this will move onto the next step.
     */
    override fun onStopArrival(routeLegProgress: RouteLegProgress): Boolean {
        return true
    }
}

/**
 * Choose when to be notified of arrival.
 */
data class ArrivalOptions(

    /**
     * While the next stop is less than [arrivalInSeconds] away,
     * [ArrivalController.onStopArrival] will be called
     */
    val arrivalInSeconds: Double?,

    /**
     * While the next stop is less than [arrivalInMeters] away,
     * [ArrivalController.onStopArrival] will be called
     */
    val arrivalInMeters: Double?
) {
    /**
     * Build your arrival options.
     */
    class Builder {

        private var arrivalInSeconds: Double? = 5.0
        private var arrivalInMeters: Double? = 40.0

        /**
         * (Recommended) Use time estimation for arrival, arrival is influenced by traffic conditions.
         * Arrive when the estimated time to a stop is less than or equal to this threshold
         */
        fun arriveInSeconds(arriveInSeconds: Double?): Builder {
            this.arrivalInSeconds = arriveInSeconds
            return this
        }

        /**
         * Arrive when the estimated distance to a stop is less than or equal to this threshold
         */
        fun arriveInMeters(arriveInMeters: Double?): Builder {
            this.arrivalInMeters = arriveInMeters
            return this
        }

        /**
         * Build the object. If you want to disable this feature use [MapboxNavigation.removeArrivalController]
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
