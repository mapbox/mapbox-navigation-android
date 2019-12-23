package com.mapbox.navigation.logger

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.navigation.base.logger.Logger
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.logger.annotations.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.whileSelect
import java.util.concurrent.atomic.AtomicReference
import timber.log.Timber
import java.lang.Exception
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@MapboxNavigationModule(MapboxNavigationModuleType.Logger, skipConfiguration = true)
object MapboxLogger : Logger {

    @LogLevel
    @Volatile
    var logLevel: Int = VERBOSE

    data class MessageData(val message: String, val tag: String?)

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val channel = BroadcastChannel<Message>(Channel.UNLIMITED)
    private val observer: AtomicReference<LoggerObserver> = AtomicReference()

    /**
     * Sample code
     * During initialization, create a BroadcastChannel with an unlimited buffer. This channel has several properties
     * 1. It is lock-free
     * 2. Thread safe
     * 3. Each openSubscription() call guarantees that you well receive the entire buffer of data
     * This implies that if multiple clients call offer() on this channel from different threads, this
     * coroutine (the one we launched from init{}) will receive each message.
     */
    init {
        val subscription = channel.openSubscription()
        scope.launch {
            val channelData = subscription.receiveOrClosed()
            when (channelData.isClosed) {
                true -> return@launch
                false -> {
                    channel.consumeEach { messageData ->
                        d(Message(messageData.message))
                    }
                }
            }
        }
    }

    fun getLoggChannel() = channel
    fun setObserver(observer: LoggerObserver) {
        this.observer.set(observer)
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
