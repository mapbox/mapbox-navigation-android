package com.mapbox.navigation.logger

interface LoggerObserver {
    fun log(level: Int, entry: LogEntry)
}
