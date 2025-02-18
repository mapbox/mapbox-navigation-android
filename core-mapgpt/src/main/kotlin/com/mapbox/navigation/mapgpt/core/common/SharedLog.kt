package com.mapbox.navigation.mapgpt.core.common

import android.util.Log as AndroidLog

object SharedLog : Log {

    override fun d(tag: String, block: () -> String) = println(SharedLogLevel.Debug, tag, block)
    override fun i(tag: String, block: () -> String) = println(SharedLogLevel.Info, tag, block)
    override fun w(tag: String, block: () -> String) = println(SharedLogLevel.Warning, tag, block)
    override fun e(tag: String, block: () -> String) = println(SharedLogLevel.Error, tag, block)

    override fun isEnabled(level: SharedLogLevel, tag: String): Boolean {
        val currentLevel = PlatformLogConfiguration.getLoggingLevelForCategory(tag)
            ?: PlatformLogConfiguration.getLoggingLevel()
        return currentLevel != null && level.logLevel >= currentLevel.logLevel
    }

    inline fun println(level: SharedLogLevel, tag: String, block: () -> String) {
        if (isEnabled(level, tag)) {
            when (level) {
                SharedLogLevel.Debug -> AndroidLog.d(tag, block())
                SharedLogLevel.Error -> AndroidLog.e(tag, block())
                SharedLogLevel.Info -> AndroidLog.i(tag, block())
                SharedLogLevel.Warning -> AndroidLog.w(tag, block())
            }
        }
    }
}

fun SharedLog.w(tag: String, tr: Throwable?, block: () -> String) {
    println(SharedLogLevel.Warning, tag, tr, block)
}

fun SharedLog.e(tag: String, tr: Throwable?, block: () -> String) {
    println(SharedLogLevel.Error, tag, tr, block)
}

inline fun SharedLog.println(
    level: SharedLogLevel,
    tag: String,
    tr: Throwable?,
    crossinline block: () -> String,
) {
    val blockWithThrowable: () -> String =
        { "${block()}\n${android.util.Log.getStackTraceString(tr)}" }
    println(level, tag, blockWithThrowable)
}
