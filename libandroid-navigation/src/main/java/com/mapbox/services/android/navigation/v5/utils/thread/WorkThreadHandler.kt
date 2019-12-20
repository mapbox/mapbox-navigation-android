package com.mapbox.services.android.navigation.v5.utils.thread

import android.os.Handler
import android.os.HandlerThread
import com.mapbox.services.android.navigation.v5.utils.extensions.quitSafelySupport

class WorkThreadHandler(
    private val handleThreadName: String = HANDLE_THREAD_NAME
) : ThreadHandler {

    companion object {
        private const val HANDLE_THREAD_NAME = "WorkingThread"
    }

    private lateinit var handlerThread: HandlerThread
    lateinit var handler: Handler

    override var isStarted: Boolean = false

    override fun post(task: () -> Unit) {
        if (!isStarted) {
            return
        }
        handler.post {
            task.invoke()
        }
    }

    override fun postDelayed(task: () -> Unit, delayMillis: Long) {
        if (!isStarted) {
            return
        }
        handler.postDelayed({
            task.invoke()
        }, delayMillis)
    }

    override fun start() {
        handlerThread = HandlerThread(handleThreadName)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
        isStarted = true
    }

    override fun stop() {
        isStarted = false
        removeAllTasks()

        handlerThread.quitSafelySupport()
        try {
            handlerThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun removeAllTasks() {
        handler.removeCallbacksAndMessages(null)
    }
}