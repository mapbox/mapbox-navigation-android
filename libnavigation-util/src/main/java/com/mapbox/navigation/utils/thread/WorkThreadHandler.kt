package com.mapbox.navigation.utils.thread

import android.os.Handler
import android.os.HandlerThread
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.utils.extensions.quitSafelySupport

class WorkThreadHandler(
    private val handleThreadName: String = HANDLE_THREAD_NAME
) : ThreadHandler {

    companion object {
        private const val TAG = "WORK_THREAD_HANDLER"
        private const val HANDLE_THREAD_NAME = "WorkingThread"
    }

    private lateinit var handlerThread: HandlerThread
    lateinit var handler: Handler

    override var isStarted: Boolean = false

    override fun post(task: () -> Unit) {
        if (!isStarted) {
            MapboxLogger.e(TAG, "Need to start handler before post event")
            return
        }
        handler.post {
            task.invoke()
        }
    }

    override fun postDelayed(task: () -> Unit, delayMillis: Long) {
        if (!isStarted) {
            MapboxLogger.e(TAG, "Need to start handler before post event")
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
            MapboxLogger.e(TAG, e.localizedMessage, e)
        }
    }

    override fun removeAllTasks() {
        handler.removeCallbacksAndMessages(null)
    }
}
