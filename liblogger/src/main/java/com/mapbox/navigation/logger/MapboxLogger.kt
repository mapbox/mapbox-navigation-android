package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

@MapboxNavigationModule(MapboxNavigationModuleType.Logger, skipConfiguration = true)
class MapboxLogger : Logger {

    @LogLevel
    var logLevel: Int = VERBOSE

    private val observer: AtomicReference<LoggerObserver> = AtomicReference()

    fun setObserver(observer: LoggerObserver) {
        this.observer.set(observer)
    }

    override fun v(tag: String, msg: String, tr: Throwable?) {
        if (logLevel <= VERBOSE) {
            Timber.v(tr, tag, msg)
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }

    override fun d(tag: String, msg: String, tr: Throwable?) {
        if (logLevel <= DEBUG) {
            Timber.d(tr, tag, msg)
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }

    override fun i(tag: String, msg: String, tr: Throwable?) {
        if (logLevel <= INFO) {
            Timber.i(tr, tag, msg)
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }

    override fun w(tag: String, msg: String, tr: Throwable?) {
        if (logLevel <= WARN) {
            Timber.w(tr, tag, msg)
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }

    override fun e(tag: String, msg: String, tr: Throwable?) {
        if (logLevel <= ERROR) {
            Timber.e(tr, tag, msg)
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }
}
