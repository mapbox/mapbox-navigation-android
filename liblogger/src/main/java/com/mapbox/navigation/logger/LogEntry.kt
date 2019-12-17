package com.mapbox.navigation.logger

data class LogEntry(
    val tag: String?,
    val message: String,
    val throwable: Throwable?
)
