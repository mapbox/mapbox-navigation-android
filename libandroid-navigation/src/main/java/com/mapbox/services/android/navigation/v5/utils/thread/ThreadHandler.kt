package com.mapbox.services.android.navigation.v5.utils.thread

interface ThreadHandler {

    val isStarted: Boolean

    fun post(task: () -> Unit)

    fun postDelayed(task: () -> Unit, delayMillis: Long)

    fun start()

    fun stop()

    fun removeAllTasks()
}