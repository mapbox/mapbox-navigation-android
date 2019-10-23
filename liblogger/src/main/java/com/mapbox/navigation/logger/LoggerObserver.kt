package com.mapbox.navigation.logger

interface LoggerObserver {
    fun log(@LogLevel level: Int, entry: LogEntry)
}
