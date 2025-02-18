package com.mapbox.navigation.mapgpt.core.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

internal class ConversationTimeoutMonitor(
    private val coroutineScope: CoroutineScope,
) {

    private var timeoutTask: TimeoutTask? = null

    fun onNewConversationStarted(
        timeout: Duration,
        onTimeout: () -> Unit,
    ) {
        timeoutTask?.stop()
        timeoutTask = TimeoutTask(
            coroutineScope = coroutineScope,
            onTimeout = onTimeout,
            timeout = timeout,
        ).also {
            it.start()
        }
    }

    fun onNewEventReceived() {
        timeoutTask?.start()
    }

    fun cancel() {
        timeoutTask?.stop()
        timeoutTask = null
    }

    private class TimeoutTask(
        private val coroutineScope: CoroutineScope,
        private val onTimeout: () -> Unit,
        private val timeout: Duration,
    ) {
        private var conversationTimeoutJob: Job? = null

        fun start() {
            stop()
            conversationTimeoutJob = coroutineScope.launch {
                delay(timeout)
                onTimeout()
            }
        }

        fun stop() {
            conversationTimeoutJob?.cancel()
        }
    }
}
