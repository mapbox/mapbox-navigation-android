package com.mapbox.navigation.core

import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI

internal object MapboxCrashHandler {

    @Volatile
    private var defaultCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun setUp() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logE("Caught a crash: $throwable", "[MapboxCamera-Crash]")
            printThreadsDump()
            defaultCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun printThreadsDump() {
        logI("All threads dump:", "[MapboxCamera-Crash]")
        Thread.getAllStackTraces().forEach { (thread, stacktrace) ->
            logI("Thread: ${thread.name} with id: ${thread.id}, state: ${thread.state.explain()}", "[MapboxCamera-Crash]")
            stacktrace.forEach {
                logI("at ${it.className}.${it.methodName}:${it.lineNumber} (${it.fileName})", "[MapboxCamera-Crash]")
            }
        }
        logI("==================================================", "[MapboxCamera-Crash]")
    }

    private fun Thread.State.explain(): String {
        return when(this) {
            Thread.State.NEW -> "NEW"
            Thread.State.RUNNABLE -> "RUNNABLE"
            Thread.State.BLOCKED -> "BLOCKED"
            Thread.State.WAITING -> "WAITING"
            Thread.State.TIMED_WAITING -> "TIMED_WAITING"
            Thread.State.TERMINATED -> "TERMINATED"
        }
    }
}
