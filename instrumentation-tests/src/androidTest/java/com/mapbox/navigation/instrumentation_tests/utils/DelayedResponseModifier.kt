package com.mapbox.navigation.instrumentation_tests.utils

class DelayedResponseModifier(
    private val delayMillis: Long,
    private val originalResponseModifier: ((String) -> String)? = null
) : (String) -> String {

    override fun invoke(p1: String): String {
        Thread.sleep(delayMillis)
        return originalResponseModifier?.invoke(p1) ?: p1
    }
}
