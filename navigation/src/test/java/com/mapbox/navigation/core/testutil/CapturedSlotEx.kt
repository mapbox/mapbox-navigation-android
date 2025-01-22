package com.mapbox.navigation.core.testutil

import io.mockk.CapturingSlot

fun <T : Any> CapturingSlot<T>.ifCaptured(func: (T.() -> Unit)) {
    if (isCaptured) {
        captured.func()
    }
}
