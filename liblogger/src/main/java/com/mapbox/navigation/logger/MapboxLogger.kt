package com.mapbox.navigation.logger

import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.logger.annotations.LogLevel
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber

/**
 * Default implementation of [Logger] interface
 */

@MapboxModule(MapboxModuleType.CommonLogger)
object MapboxLogger : Logger {

    /**
     * Defines level of logs need to be logged.
     * For example, if current logLevel is INFO:
     *  - MapboxLogger.d("Message") - will not log anything
     *  - MapboxLogger.i("Message") - will log "Message"
     *  - MapboxLogger.w("Message") - will log "Message"
     */
    @LogLevel
    @Volatile
    var logLevel: Int = VERBOSE

    /**
     * Observes logged messages.
     */
    private val observer: AtomicReference<LoggerObserver> = AtomicReference()

    fun setObserver(observer: LoggerObserver) {
        this.observer.set(observer)
    }

    fun removeObserver() {
        observer.set(null)
    }

    fun v(msg: Message) {
        v(null, msg, null)
    }

    fun v(msg: Message, tr: Throwable) {
        v(null, msg, tr)
    }

    fun v(tag: Tag, msg: Message) {
        v(tag, msg, null)
    }

    override fun v(tag: Tag?, msg: Message, tr: Throwable?) {
        log(VERBOSE, tag?.tag, msg.message, tr) { Timber.v(tr, msg.message) }
    }

    fun d(msg: Message) {
        d(null, msg, null)
    }

    fun d(msg: Message, tr: Throwable) {
        d(null, msg, tr)
    }

    fun d(tag: Tag, msg: Message) {
        d(tag, msg, null)
    }

    override fun d(tag: Tag?, msg: Message, tr: Throwable?) {
        log(DEBUG, tag?.tag, msg.message, tr) { Timber.d(tr, msg.message) }
    }

    fun i(msg: Message) {
        i(null, msg, null)
    }

    fun i(msg: Message, tr: Throwable) {
        i(null, msg, tr)
    }

    fun i(tag: Tag, msg: Message) {
        i(tag, msg, null)
    }

    override fun i(tag: Tag?, msg: Message, tr: Throwable?) {
        log(INFO, tag?.tag, msg.message, tr) { Timber.i(tr, msg.message) }
    }

    fun w(msg: Message) {
        w(null, msg, null)
    }

    fun w(msg: Message, tr: Throwable) {
        w(null, msg, tr)
    }

    fun w(tag: Tag, msg: Message) {
        w(tag, msg, null)
    }

    override fun w(tag: Tag?, msg: Message, tr: Throwable?) {
        log(WARN, tag?.tag, msg.message, tr) { Timber.w(tr, msg.message) }
    }

    fun e(msg: Message) {
        e(null, msg, null)
    }

    fun e(msg: Message, tr: Throwable) {
        e(null, msg, tr)
    }

    fun e(tag: Tag, msg: Message) {
        e(tag, msg, null)
    }

    override fun e(tag: Tag?, msg: Message, tr: Throwable?) {
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
