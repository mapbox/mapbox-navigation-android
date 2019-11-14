package com.mapbox.navigation.utils.thread

import android.os.Handler

abstract class PriorityRunnable(
    val id: String,
    val priority: Priority,
    val handler: Handler
) : Runnable
