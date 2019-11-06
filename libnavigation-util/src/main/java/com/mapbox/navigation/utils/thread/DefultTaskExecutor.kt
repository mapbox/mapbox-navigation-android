package com.mapbox.navigation.utils.thread

import android.os.Process
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object DefultTaskExecutor: TaskExecutor {

    private const val KEEP_THREAD_ALIVE_TIME = 60L
    private val backgroundTasksExecutor: PriorityThreadPoolExecutor
    private val backgroundTaskQueue: MutableMap<Int, Future<*>> = mutableMapOf()

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

    override fun run(
        taskId: Int,
        priority: Priority,
        task: () -> Unit
    ) {
        val future = backgroundTasksExecutor.submit(object : PriorityRunnable(priority) {
            override fun run() {
                task.invoke()
            }
        })
        backgroundTaskQueue[taskId]?.let {
            cancel(taskId)
        }
        backgroundTaskQueue[taskId] = future
    }

    override fun cancel(taskId: Int) {
        backgroundTaskQueue[taskId]?.cancel(true)
    }
}
