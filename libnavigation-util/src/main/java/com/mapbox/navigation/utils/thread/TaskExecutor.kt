package com.mapbox.navigation.utils.thread

interface TaskExecutor {

    fun run(task: PriorityRunnable)

    fun cancel(taskId: String)
}
