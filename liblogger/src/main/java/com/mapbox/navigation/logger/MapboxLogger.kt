package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.logger.annotations.LogLevel
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

@MapboxNavigationModule(MapboxNavigationModuleType.Logger, skipConfiguration = true)
object MapboxLogger : Logger {

    @LogLevel
    @Volatile
    var logLevel: Int = VERBOSE

    private val observer: AtomicReference<LoggerObserver> = AtomicReference()

    fun setObserver(observer: LoggerObserver) {
        this.observer.set(observer)
    }

    fun v(msg: String) {
        v(msg, null, null)
    }

    fun v(msg: String, tr: Throwable) {
        v(msg, null, tr)
    }

    fun v(msg: String, tag: String) {
        v(msg, tag, null)
    }

    override fun v(msg: String, tag: String?, tr: Throwable?) {
        log(VERBOSE, tag, msg, tr) { Timber.v(tr, msg) }
    }

    fun d(msg: String) {
        d(msg, null, null)
    }

    fun d(msg: String, tr: Throwable) {
        d(msg, null, tr)
    }

    fun d(msg: String, tag: String) {
        d(msg, tag, null)
    }

    override fun d(msg: String, tag: String?, tr: Throwable?) {
        log(DEBUG, tag, msg, tr) { Timber.d(tr, msg) }
    }

    fun i(msg: String) {
        i(msg, null, null)
    }

    fun i(msg: String, tr: Throwable) {
        i(msg, null, tr)
    }

    fun i(msg: String, tag: String) {
        i(msg, tag, null)
    }

    override fun i(msg: String, tag: String?, tr: Throwable?) {
        log(INFO, tag, msg, tr) { Timber.i(tr, msg) }
    }

    fun w(msg: String) {
        w(msg, null, null)
    }

    fun w(msg: String, tr: Throwable) {
        w(msg, null, tr)
    }

    fun w(msg: String, tag: String) {
        w(msg, tag, null)
    }

    override fun w(msg: String, tag: String?, tr: Throwable?) {
        log(WARN, tag, msg, tr) { Timber.w(tr, msg) }
    }

    fun e(msg: String) {
        e(msg, null, null)
    }

    fun e(msg: String, tr: Throwable) {
        e(msg, null, tr)
    }

    fun e(msg: String, tag: String) {
        e(msg, tag, null)
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
            observer.get()?.log(requiredLogLevel, LogEntry(tag, msg, tr))
        }
    }
}
