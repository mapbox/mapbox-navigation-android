package com.mapbox.navigation.instrumentation_tests.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.DownloadOptions
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceFactory
import com.mapbox.common.HttpServiceInterceptorInterface
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
    val interseptor = object : HttpServiceInterceptorInterface {
        override fun onRequest(request: HttpRequest): HttpRequest {
            Log.d(LOG_TAG, "on request ${request.url}")
            return request
        }

        override fun onDownload(download: DownloadOptions): DownloadOptions {
            Log.d(LOG_TAG, "on request ${download.request.url}")
            return download
        }

        override fun onResponse(response: HttpResponse): HttpResponse {
            Log.d(LOG_TAG, "on response code: ${response.result.value?.code} ${response.request.url}, error ${response.result.error?.message}")
            return response
        }

    }
    Log.d(LOG_TAG, "registering interceptor")
    HttpServiceFactory.getInstance().setInterceptor(interseptor)
    try {
        withoutWifiAndMobileData {
            mockWebServerRule.withoutWebServer {
                block()
            }
        }
    } finally {
        Log.d(LOG_TAG, "unregistering interceptor")
        HttpServiceFactory.getInstance().setInterceptor(null)
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

private suspend fun ReachabilityInterface.waitForNetworkStatus(
    condition: (NetworkStatus) -> Boolean
) {
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
            Log.d(LOG_TAG, "Current network status $currentStatus. $messageForStatus")
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
