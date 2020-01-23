package com.mapbox.navigation.logger

/**
 * Defines entity describes log event.
 *
 * @param tag Tag for log message
 * @param message Message need to be logged
 * @param throwable Throwable need to be logged
 */
data class LogEntry(
    val tag: String?,
    val message: String,
    val throwable: Throwable?
)
