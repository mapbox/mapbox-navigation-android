package com.mapbox.navigation.base.options

import com.mapbox.android.core.location.LocationEngineRequest

/**
 * Defines options for the navigator polling loops.
 *
 * TODO move NavigationOptions.navigatorPredictionMillis to these options
 * TODO move NavigationOptions.locationEngine to these options
 *
 * @param locationEngineRequest specifies the rate to request locations from the device.
 * @param navigatorIntervalMillis the rate to update the navigator when no locations have been returned. Default value 1000L.
 * @param navigatorPatienceMillis the time to wait for no locations, and then start a polling interval. Default value 2000L.
 */
class LocationPollingOptions private constructor(
    val locationEngineRequest: LocationEngineRequest,
    val navigatorIntervalMillis: Long,
    val navigatorPatienceMillis: Long
) {

    /**
     * Get a builder to customize a subset of current options.
     */
    fun toBuilder(): Builder = Builder().apply {
        locationEngineRequest(locationEngineRequest)
        navigatorIntervalMillis(navigatorIntervalMillis)
        navigatorPatienceMillis(navigatorPatienceMillis)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationPollingOptions

        if (locationEngineRequest != other.locationEngineRequest) return false
        if (navigatorIntervalMillis != other.navigatorIntervalMillis) return false
        if (navigatorPatienceMillis != other.navigatorPatienceMillis) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = locationEngineRequest.hashCode()
        result = 31 * result + navigatorIntervalMillis.hashCode()
        result = 31 * result + navigatorPatienceMillis.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LocationOptions(" +
            "locationEngineRequest=$locationEngineRequest" +
            "navigatorIntervalMillis=$navigatorIntervalMillis, " +
            "navigatorPatienceMillis=$navigatorPatienceMillis, " +
            ")"
    }

    /**
     * Build a new [LocationPollingOptions]
     */
    class Builder {

        private var locationEngineRequest = LocationEngineRequest.Builder(1000L)
            .setFastestInterval(500L)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        private var navigatorIntervalMillis: Long = 1000L
        private var navigatorPatienceMillis: Long = 2000L

        /**
         * Override the [LocationEngineRequest]
         */
        fun locationEngineRequest(locationEngineRequest: LocationEngineRequest): Builder =
            apply { this.locationEngineRequest = locationEngineRequest }

        /**
         * Override the polling interval for navigator updates.
         */
        fun navigatorIntervalMillis(navigatorIntervalMillis: Long): Builder =
            apply { this.navigatorIntervalMillis = navigatorIntervalMillis }

        /**
         * Override the time it takes before triggering navigator updates.
         */
        fun navigatorPatienceMillis(navigatorPatienceMillis: Long): Builder =
            apply { this.navigatorPatienceMillis = navigatorPatienceMillis }

        /**
         * Build the [LocationPollingOptions]
         */
        fun build(): LocationPollingOptions {
            return LocationPollingOptions(
                locationEngineRequest = locationEngineRequest,
                navigatorIntervalMillis = navigatorIntervalMillis,
                navigatorPatienceMillis = navigatorPatienceMillis
            )
        }
    }
}
