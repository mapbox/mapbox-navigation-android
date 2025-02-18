package com.mapbox.navigation.mapgpt.core.common

interface Log {
    fun d(tag: String, block: () -> String)
    fun i(tag: String, block: () -> String)
    fun w(tag: String, block: () -> String)
    fun e(tag: String, block: () -> String)
    fun isEnabled(level: SharedLogLevel, tag: String): Boolean
}
