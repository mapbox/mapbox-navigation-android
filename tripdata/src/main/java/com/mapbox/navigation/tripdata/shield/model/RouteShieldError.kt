package com.mapbox.navigation.tripdata.shield.model

import com.mapbox.navigation.utils.internal.obfuscateAccessToken

/**
 * Data structure that holds information about errors in downloading route shields.
 * @property url that was downloaded and resulted in an error
 * @property errorMessage explains the reason for failure to download the shield.
 */
class RouteShieldError internal constructor(
    val url: String?,
    val errorMessage: String,
) {
    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RouteShieldError) return false

        if (url != other.url) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + errorMessage.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouteShieldError(" +
            "url='${url?.obfuscateAccessToken()}', " +
            "errorMessage='$errorMessage'" +
            ")"
    }
}
