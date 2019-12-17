package com.mapbox.navigation.logger

import com.mapbox.navigation.logger.annotations.LogLevel

interface LoggerObserver {
    fun log(@LogLevel level: Int, entry: LogEntry)
}
