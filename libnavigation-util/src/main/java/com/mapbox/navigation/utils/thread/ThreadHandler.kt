package com.mapbox.navigation.utils.thread

internal interface ThreadHandler {

    val isStarted: Boolean

    fun post(task: () -> Unit)

    fun postDelayed(task: () -> Unit, delayMillis: Long)

    fun start()

    fun stop()

    fun removeAllTasks()
}
