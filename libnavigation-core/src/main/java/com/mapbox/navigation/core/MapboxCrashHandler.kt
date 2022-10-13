package com.mapbox.navigation.core

import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logI

internal object MapboxCrashHandler {

    private const val TAG = "[MapboxCamera-Crash2]"

    @Volatile
    private var defaultCrashHandler: Thread.UncaughtExceptionHandler? = null

    fun setUp() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logE("Caught a crash: $throwable", TAG)
            printThreadsDump()
            defaultCrashHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun printThreadsDump() {
        logI("All threads dump:", TAG)
        Thread.getAllStackTraces().forEach { (thread, stacktrace) ->
            logI("Thread: ${thread.name} with id: ${thread.id}, state: ${thread.state.explain()}", TAG)
            stacktrace.forEach {
                logI("at ${it.className}.${it.methodName}:${it.lineNumber} (${it.fileName})", TAG)
            }
        }
        logI("==================================================", TAG)
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
