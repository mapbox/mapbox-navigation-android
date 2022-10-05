package com.mapbox.navigation.utils.internal

import com.mapbox.common.LoggingLevel

fun LoggingLevel?.atLeast(loggingLevel: LoggingLevel): Boolean {
    return toPriority(this) >= toPriority(loggingLevel)
}

private fun toPriority(loggingLevel: LoggingLevel?) = when (loggingLevel) {
    null -> 0
    LoggingLevel.DEBUG -> 1
    LoggingLevel.INFO -> 2
    LoggingLevel.WARNING -> 3
    LoggingLevel.ERROR -> 4
}