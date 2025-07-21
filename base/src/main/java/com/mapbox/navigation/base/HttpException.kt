package com.mapbox.navigation.base

/**
 * Exception thrown when an HTTP response is not successful,
 * i.e., the status code is not in the range [200..299].
 *
 * @property httpCode The HTTP status code returned by the server.
 * @property message The HTTP status message, or a custom error message.
 * @property cause The underlying cause of this exception, if any.
 */
@ExperimentalPreviewMapboxNavigationAPI
class HttpException @JvmOverloads constructor(
    val httpCode: Int,
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /**
     * @return a string representation of the object
     */
    override fun toString(): String {
        return "HttpException(httpCode=$httpCode, message='$message', cause=$cause)"
    }
}
