package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

@MapboxNavigationModule(MapboxNavigationModuleType.Logger, skipConfiguration = true)
class MapboxLogger private constructor() : Logger {

    companion object {
        @JvmStatic
        val instance: MapboxLogger by lazy { MapboxLogger() }
    }

    @LogLevel
    @Volatile
    var logLevel: Int = VERBOSE

    private val observer: AtomicReference<LoggerObserver> = AtomicReference()

    fun setObserver(observer: LoggerObserver) {
        this.observer.set(observer)
    }

    override fun v(tag: String?, msg: String, tr: Throwable?) {
        log(VERBOSE, tag, msg, tr) { Timber.v(tr, msg) }
    }

    override fun d(tag: String?, msg: String, tr: Throwable?) {
        log(DEBUG, tag, msg, tr) { Timber.d(tr, msg) }
    }

    override fun i(tag: String?, msg: String, tr: Throwable?) {
        log(INFO, tag, msg, tr) { Timber.i(tr, msg) }
    }

    override fun w(tag: String?, msg: String, tr: Throwable?) {
        log(WARN, tag, msg, tr) { Timber.w(tr, msg) }
    }

    override fun e(tag: String?, msg: String, tr: Throwable?) {
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
            logBlock.invoke()
            observer.get()?.log(logLevel, LogEntry(tag, msg, tr))
        }
    }
}
