package com.mapbox.navigation.base.route

/**
 * An exception that is thrown when parsing Directions Response fails.
 *
 * @param original the original exception that was thrown by the parsing tools
 */
class DirectionsResponseParsingException internal constructor(
    val original: Throwable,
) : Exception(original)
