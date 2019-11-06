package com.mapbox.navigation.utils.thread

import android.os.Process
import com.mapbox.navigation.logger.MapboxLogger
import java.util.concurrent.ThreadFactory

class PriorityThreadFactory(private val threadPriority: Int) : ThreadFactory {

    companion object {
        private const val TAG = "PriorityThreadFactory"
    }

    override fun newThread(runnable: Runnable?): Thread {
        val wrapperRunnable = Runnable {
            try {
                Process.setThreadPriority(threadPriority)
            } catch (t: Throwable) {
                MapboxLogger.e(TAG, t.localizedMessage, t)
            }

            runnable?.run()
        }
        return Thread(wrapperRunnable)
    }
}
