package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.utils.executeShellCommandBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue

private const val LOG_TAG = "TestNetwork"

suspend fun BaseCoreNoCleanUpTest.withoutInternet(block: suspend () -> Unit) {
    // Doesn't work stable on old devices
    assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
    withoutWifiAndMobileData {
        mockWebServerRule.withoutWebServer {
            block()
        }
    }
}

suspend fun withoutWifiAndMobileData(block: suspend () -> Unit) {
    Log.d(LOG_TAG, "Got request to turn internet off, checking if it was present")
    assumeNetworkIsReachable()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiAutomation = instrumentation.uiAutomation
    Log.d(LOG_TAG, "turning off wifi and mobile data")
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    try {
        assumeNetworkIsNotReachable()
        block()
    } finally {
        Log.d(LOG_TAG, "turning on wifi and mobile data")
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
    assumeNetworkIsReachable()
}

private suspend fun assumeNetworkIsNotReachable() {
    val result = waitForHostReachability(expectedReachability = false)
    assumeTrue(
        "network should not be reachable if it's turned off on device",
        result == WaitResult.SUCCESS
    )
}

private suspend fun assumeNetworkIsReachable() {
    val result = waitForHostReachability(expectedReachability = true)
    assumeTrue(
        "host should be reachable when network is turned on",
        result == WaitResult.SUCCESS
    )
}

private enum class WaitResult { SUCCESS, TIMEOUT }

private suspend fun waitForHostReachability(expectedReachability: Boolean): WaitResult =
    withTimeoutOrNull(20_000) {
        withContext(Dispatchers.IO) {
            while (true) {
                val actualReachability = checkReachability()
                Log.d(
                    LOG_TAG,
                    "is host reachable: $actualReachability, waiting for $expectedReachability"
                )
                if (actualReachability == expectedReachability) {
                    break
                } else {
                    delay(500)
                }
            }
            WaitResult.SUCCESS
        }
    } ?: WaitResult.TIMEOUT

private fun checkReachability(): Boolean {
    val command = "ping -W 1 -c 1 8.8.8.8"
    val result = InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommandBlocking(command)
        .let { String(it) }
    Log.d(LOG_TAG, "result of ping execution: $result")
    return result.isNotEmpty() && (!result.contains("100% packet loss"))
}
