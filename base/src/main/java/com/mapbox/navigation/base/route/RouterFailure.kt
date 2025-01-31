package com.mapbox.navigation.base.route

import java.net.URL

/**
 * Describes a reason for a route request failure.
 *
 * @param url original request URL
 * @param routerOrigin router that failed to generate a route
 * @param message message attached to the error code
 * @param type failure type
 * @param throwable provided if an unexpected exception occurred when creating the request or processing the response
 * @param isRetryable Indicates if it makes sense to retry the failed route request for this type of failure.
 */
class RouterFailure internal constructor(
    val url: URL,
    @RouterOrigin val routerOrigin: String,
    val message: String,
    @RouterFailureType val type: String,
    val throwable: Throwable?,
    val isRetryable: Boolean,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouterFailure

        if (url != other.url) return false
        if (routerOrigin != other.routerOrigin) return false
        if (message != other.message) return false
        if (type != other.type) return false
        if (throwable != other.throwable) return false
        return isRetryable == other.isRetryable
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + routerOrigin.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (throwable?.hashCode() ?: 0)
        result = 31 * result + isRetryable.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "RouterFailure(" +
            "url=$url, " +
            "routerOrigin='$routerOrigin', " +
            "message='$message', " +
            "type='$type', " +
            "throwable=$throwable, " +
            "isRetryable=$isRetryable" +
            ")"
    }
}
