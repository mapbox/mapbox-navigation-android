package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.NetworkStatus
import com.mapbox.common.ReachabilityFactory
import com.mapbox.common.ReachabilityInterface
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeFalse
import kotlin.coroutines.resume

private const val LOG_TAG = "TestNetwork"

suspend fun BaseCoreNoCleanUpTest.withoutInternet(block: suspend () -> Unit) {
    withoutWifiAndMobileData {
        mockWebServerRule.withoutWebServer {
            block()
        }
    }
}

suspend fun withoutWifiAndMobileData(block: suspend () -> Unit) {
    val reachability = ReachabilityFactory.reachability(null)
    Log.d(LOG_TAG, "Got request to turn internet off, checking if it was present")
    reachability.waitForNetworkStatus { it != NetworkStatus.NOT_REACHABLE }
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiAutomation = instrumentation.uiAutomation
    Log.d(LOG_TAG, "turning off wifi and mobile data")
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    try {
        assumeNetworkIsNotReachable(reachability)
        block()
    } finally {
        Log.d(LOG_TAG, "turning on wifi and mobile data")
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
    Log.d(LOG_TAG, "Waiting for network to become reachable")
    reachability.waitForNetworkStatus { it != NetworkStatus.NOT_REACHABLE }
}

private suspend fun assumeNetworkIsNotReachable(reachability: ReachabilityInterface) {
    val networkIsReachable = withTimeoutOrNull(3000) {
        reachability.waitForNetworkStatus { it == NetworkStatus.NOT_REACHABLE }
        false
    } ?: true
    assumeFalse(
        "network should not be reachable if it's turned off on device",
        networkIsReachable
    )
}

suspend fun ReachabilityInterface.waitForNetworkStatus(condition: (NetworkStatus) -> Boolean) {
    val currentStatus = currentNetworkStatus()
    if (condition(currentNetworkStatus())) {
        Log.d(LOG_TAG, "Network status $currentStatus is ok")
        return
    }
    suspendCancellableCoroutine<Unit> { continuation ->
        val id = this.addListener { currentStatus ->
            val satisfiesCondition = condition(currentStatus)
            val messageForStatus = if (satisfiesCondition) {
                "Ok."
            } else {
                "Keep on waiting for updates."
            }
            Log.d("Network", "Current network status $currentStatus. $messageForStatus")
            if (satisfiesCondition) {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
        }
        continuation.invokeOnCancellation {
            this.removeListener(id)
        }
    }
}
