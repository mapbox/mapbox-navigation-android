package com.mapbox.navigation.testing.utils.assertions

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun List<*>.waitUntilHasSize(size: Int, timeout: Duration = 10.seconds) {
    val list = this
    withTimeout(timeout) {
        while (list.size < size) {
            delay(50)
        }
    }
}
