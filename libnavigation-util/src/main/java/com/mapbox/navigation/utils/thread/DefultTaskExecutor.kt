package com.mapbox.navigation.utils.thread

import android.os.Process
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object DefultTaskExecutor : TaskExecutor {

    private const val KEEP_THREAD_ALIVE_TIME = 60L
    private val backgroundTasksExecutor: PriorityThreadPoolExecutor
    private val backgroundTaskQueue: MutableMap<String, Future<*>> = mutableMapOf()

    init {
        val backgroundPriorityThreadFactory = PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND)
        val numberOfCores = Runtime.getRuntime().availableProcessors()

        backgroundTasksExecutor = PriorityThreadPoolExecutor(
            numberOfCores,
            numberOfCores,
            KEEP_THREAD_ALIVE_TIME,
            TimeUnit.SECONDS,
            backgroundPriorityThreadFactory
        )
    }

    override fun run(task: PriorityRunnable) {
        val future = backgroundTasksExecutor.submit(task)
        backgroundTaskQueue[task.id]?.let {
            cancel(task.id)
        }
        backgroundTaskQueue[task.id] = future
    }

    override fun cancel(taskId: String) {
        backgroundTaskQueue[taskId]?.cancel(true)
    }
}
