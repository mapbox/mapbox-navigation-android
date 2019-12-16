package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
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

    fun v(msg: Message) {
        v(msg, null, null)
    }

    fun v(msg: Message, tr: Throwable) {
        v(msg, null, tr)
    }

    fun v(msg: Message, tag: Tag) {
        v(msg, tag, null)
    }

    override fun v(msg: Message, tag: Tag?, tr: Throwable?) {
        log(VERBOSE, tag?.tag, msg.message, tr) { Timber.v(tr, msg.message) }
    }

    fun d(msg: Message) {
        d(msg, null, null)
    }

    fun d(msg: Message, tr: Throwable) {
        d(msg, null, tr)
    }

    fun d(msg: Message, tag: Tag) {
        d(msg, tag, null)
    }

    override fun d(msg: Message, tag: Tag?, tr: Throwable?) {
        log(DEBUG, tag?.tag, msg.message, tr) { Timber.d(tr, msg.message) }
    }

    fun i(msg: Message) {
        i(msg, null, null)
    }

    fun i(msg: Message, tr: Throwable) {
        i(msg, null, tr)
    }

    fun i(msg: Message, tag: Tag) {
        i(msg, tag, null)
    }

    override fun i(msg: Message, tag: Tag?, tr: Throwable?) {
        log(INFO, tag?.tag, msg.message, tr) { Timber.i(tr, msg.message) }
    }

    fun w(msg: Message) {
        w(msg, null, null)
    }

    fun w(msg: Message, tr: Throwable) {
        w(msg, null, tr)
    }

    fun w(msg: Message, tag: Tag) {
        w(msg, tag, null)
    }

    override fun w(msg: Message, tag: Tag?, tr: Throwable?) {
        log(WARN, tag?.tag, msg.message, tr) { Timber.w(tr, msg.message) }
    }

    fun e(msg: Message) {
        e(msg, null, null)
    }

    fun e(msg: Message, tr: Throwable) {
        e(msg, null, tr)
    }

    fun e(msg: Message, tag: Tag) {
        e(msg, tag, null)
    }

    override fun e(msg: Message, tag: Tag?, tr: Throwable?) {
        log(ERROR, tag?.tag, msg.message, tr) { Timber.e(tr, msg.message) }
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
