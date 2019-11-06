package com.mapbox.navigation.utils.thread

import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class PriorityThreadPoolExecutor(
    corePoolSize: Int,
    maximumPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit,
    threadFactory: ThreadFactory
) : ThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    unit,
    PriorityBlockingQueue<Runnable>(),
    threadFactory
) {

    override fun submit(task: Runnable?): Future<*> {
        val futureTask = PriorityFutureTask(task as PriorityRunnable)
        execute(futureTask)
        return futureTask
    }

    private class PriorityFutureTask(
        private val priorityRunnable: PriorityRunnable
    ): FutureTask<PriorityRunnable>(priorityRunnable, null),
        Comparable<PriorityFutureTask> {

        override fun compareTo(other: PriorityFutureTask): Int {
            val priority1 = priorityRunnable.priority
            val priority2 = other.priorityRunnable.priority
            return priority2.priorityValue - priority1.priorityValue
        }
    }
}