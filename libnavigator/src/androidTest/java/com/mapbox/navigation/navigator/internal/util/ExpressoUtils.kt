package com.mapbox.navigation.navigator.internal.util

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Runs the code block on the app's main thread and blocks until the block returns.
 */
fun runOnMainSync(runnable: Runnable) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable)

/**
 * Runs the code block on the app's main thread and blocks until the block returns.
 */
fun runOnMainSync(fn: () -> Unit) =
    InstrumentationRegistry.getInstrumentation().runOnMainSync(fn)
