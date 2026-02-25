package com.mapbox.navigation.testing.utils.assertions

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

suspend fun List<*>.waitUntilHasSize(
    size: Int,
    timeoutMillis: Long = 10000,
    tag: String = ""
) {
    val list = this
    val completed = withTimeoutOrNull(timeoutMillis) {
        while (list.size < size) {
            delay(50)
        }
        true
    } ?: false
    assertTrue(
        "${tag}: List did not reach the expected size of $size within $timeoutMillis ms." +
            "Current size: ${list.size}",
        completed
    )
}
