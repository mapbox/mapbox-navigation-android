package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.logger.annotations.LogLevel
import java.util.concurrent.CopyOnWriteArrayList
import timber.log.Timber

@MapboxNavigationModule(MapboxNavigationModuleType.Logger, skipConfiguration = true)
object MapboxLogger : Logger {

    @LogLevel
    @Volatile
    var logLevel: Int = VERBOSE

    private val logObservers = CopyOnWriteArrayList<LoggerObserver>()

    fun addObserver(observer: LoggerObserver) {
        logObservers.add(observer)
    }

    fun removeObserver(observer: LoggerObserver) {
        logObservers.remove(observer)
    }

    override fun v(msg: String, tag: String?, tr: Throwable?) {
        log(VERBOSE, tag, msg, tr) { Timber.v(tr, msg) }
    }

    override fun d(msg: String, tag: String?, tr: Throwable?) {
        log(DEBUG, tag, msg, tr) { Timber.d(tr, msg) }
    }

    override fun i(msg: String, tag: String?, tr: Throwable?) {
        log(INFO, tag, msg, tr) { Timber.i(tr, msg) }
    }

    override fun w(msg: String, tag: String?, tr: Throwable?) {
        log(WARN, tag, msg, tr) { Timber.w(tr, msg) }
    }

    override fun e(msg: String, tag: String?, tr: Throwable?) {
        log(ERROR, tag, msg, tr) { Timber.e(tr, msg) }
    }

    private fun log(
        @LogLevel requiredLogLevel: Int,
        tag: String?,
        msg: String,
        tr: Throwable?,
        logBlock: () -> Unit
    ) {
        if (logLevel <= requiredLogLevel) {
            tag?.let { Timber.tag(it) }
            logBlock()
            logObservers.forEach { it.log(requiredLogLevel, LogEntry(tag, msg, tr)) }
        }
    }
}
