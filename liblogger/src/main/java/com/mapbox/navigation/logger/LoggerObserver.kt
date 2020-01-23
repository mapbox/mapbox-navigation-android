package com.mapbox.navigation.logger

import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.logger.annotations.LogLevel

/**
 * Defines API observe events logged by [Logger].
 */
interface LoggerObserver {

    /**
     * Observe [LogEntry] created based on event logged with [Logger].
     *
     * @param level [LogLevel] indicates which level has observed [LogEntry].
     * @param entry [LogEntry] sent with observer.
     */
    fun log(@LogLevel level: Int, entry: LogEntry)
}
