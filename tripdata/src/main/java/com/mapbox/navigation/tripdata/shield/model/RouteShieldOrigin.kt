package com.mapbox.navigation.tripdata.shield.model

/**
 * Data structure that holds information about the original shield request that was made.
 *
 * @property isFallback is set to false if the original shield request was successful, false otherwise,
 * @property originalUrl is set to the original url used to make shield request.
 *  Can be null in case isFallback is true and the original request resulted in an error because of cancellation.
 * @property originalErrorMessage is empty if the original shield request was successful, otherwise
 * contains error pointing to the reason behind the failure of original shield request.
 */
class RouteShieldOrigin internal constructor(
    val isFallback: Boolean,
    val originalUrl: String?,
    val originalErrorMessage: String,
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteShieldOrigin) return false

        if (isFallback != other.isFallback) return false
        if (originalUrl != other.originalUrl) return false
        if (originalErrorMessage != other.originalErrorMessage) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = isFallback.hashCode()
        result = 31 * result + originalUrl.hashCode()
        result = 31 * result + originalErrorMessage.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteShieldOrigin(isFallback=$isFallback, " +
            "originalUrl='$originalUrl', " +
            "originalErrorMessage='$originalErrorMessage'" +
            ")"
    }
}
