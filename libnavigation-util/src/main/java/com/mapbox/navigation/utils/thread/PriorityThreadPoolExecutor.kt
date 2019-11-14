package com.mapbox.navigation.utils.thread

import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class PriorityThreadPoolExecutor(
    initialPoolSize: Int,
    maximumPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit,
    threadFactory: ThreadFactory
) : ThreadPoolExecutor(
    initialPoolSize,
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
    ) : FutureTask<PriorityRunnable>(priorityRunnable, null),
        Comparable<PriorityFutureTask> {

        override fun compareTo(other: PriorityFutureTask): Int {
            val priorityCurrent = priorityRunnable.priority
            val priorityOther = other.priorityRunnable.priority
            return priorityOther.priorityValue - priorityCurrent.priorityValue
        }
    }
}
