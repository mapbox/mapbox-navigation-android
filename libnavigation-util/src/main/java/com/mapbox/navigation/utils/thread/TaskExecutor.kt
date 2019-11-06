package com.mapbox.navigation.utils.thread

interface TaskExecutor {

    fun run(
        taskId: Int,
        priority: Priority,
        task: () -> Unit
    )

    fun cancel(taskId: Int)
}
