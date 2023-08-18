package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import java.net.InetAddress

private const val LOG_TAG = "TestNetwork"

suspend fun BaseCoreNoCleanUpTest.withoutInternet(block: suspend () -> Unit) {
    withoutWifiAndMobileData {
        mockWebServerRule.withoutWebServer {
            block()
        }
    }
}

suspend fun withoutWifiAndMobileData(block: suspend () -> Unit) {
    val pingAddress = withContext(Dispatchers.IO) {
        InetAddress.getByName("api.mapbox.com")
    }
    Log.d(LOG_TAG, "Got request to turn internet off, checking if it was present")
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiAutomation = instrumentation.uiAutomation
    Log.d(LOG_TAG, "turning off wifi and mobile data")
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    try {
        pingAddress.waitForNetworkStatus(isNetworkReachabilityExpected = false)
        block()
    } finally {
        Log.d(LOG_TAG, "turning on wifi and mobile data")
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
    Log.d(LOG_TAG, "Waiting for network to become reachable")
    pingAddress.waitForNetworkStatus(isNetworkReachabilityExpected = true)
}

private suspend fun InetAddress.waitForNetworkStatus(isNetworkReachabilityExpected: Boolean) {
    val actualReachability = withTimeoutOrNull(5000) {
        do {
            val isReachable = withContext(Dispatchers.IO) {
                isReachable(1000)
            }
        } while (isReachable != isNetworkReachabilityExpected)
        isNetworkReachabilityExpected
    } ?: !isNetworkReachabilityExpected
    val message = if (isNetworkReachabilityExpected) {
        "Network is expected to be reachable, but it's not. " +
            "Something went wrong during turning on network on device."
    } else {
        "Network is expected to be unreachable, but it is. " +
            "Something went wrong during turning off network on device."
    }
    assumeTrue(
        message,
        isNetworkReachabilityExpected == actualReachability
    )
}
