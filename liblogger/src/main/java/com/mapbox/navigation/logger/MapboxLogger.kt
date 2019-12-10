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
        v(null, msg, null)
    }

    fun v(msg: String, tr: Throwable) {
        v(null, msg, tr)
    }

    fun v(tag: String, msg: String) {
        v(tag, msg, null)
    }

    override fun v(tag: String?, msg: String, tr: Throwable?) {
        log(VERBOSE, tag, msg, tr) { Timber.v(tr, msg) }
    }

    fun d(msg: String) {
        d(null, msg, null)
    }

    fun d(msg: String, tr: Throwable) {
        d(null, msg, tr)
    }

    fun d(tag: String, msg: String) {
        d(tag, msg, null)
    }

    override fun d(tag: String?, msg: String, tr: Throwable?) {
        log(DEBUG, tag, msg, tr) { Timber.d(tr, msg) }
    }

    fun i(msg: String) {
        i(null, msg, null)
    }

    fun i(msg: String, tr: Throwable) {
        i(null, msg, tr)
    }

    fun i(tag: String, msg: String) {
        i(tag, msg, null)
    }

    override fun i(tag: String?, msg: String, tr: Throwable?) {
        log(INFO, tag, msg, tr) { Timber.i(tr, msg) }
    }

    fun w(msg: String) {
        w(null, msg, null)
    }

    fun w(msg: String, tr: Throwable) {
        w(null, msg, tr)
    }

    fun w(tag: String, msg: String) {
        w(tag, msg, null)
    }

    override fun w(tag: String?, msg: String, tr: Throwable?) {
        log(WARN, tag, msg, tr) { Timber.w(tr, msg) }
    }

    fun e(msg: String) {
        e(null, msg, null)
    }

    fun e(msg: String, tr: Throwable) {
        e(null, msg, tr)
    }

    fun e(tag: String, msg: String) {
        e(tag, msg, null)
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
            logBlock()
            observer.get()?.log(requiredLogLevel, LogEntry(tag, msg, tr))
        }
    }
}
