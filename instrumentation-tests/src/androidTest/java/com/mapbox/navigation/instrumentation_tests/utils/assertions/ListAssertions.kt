package com.mapbox.navigation.instrumentation_tests.utils.assertions

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

suspend fun List<*>.waitUntilHasSize(size: Int, timeoutMillis: Long = 10000) {
    val list = this
    withTimeout(timeoutMillis) {
        while (list.size < size) {
            delay(50)
        }
    }
}
