package com.mapbox.navigation.testing.utils

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.common.OfflineSwitch
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import java.net.InetAddress

private const val LOG_TAG = "TestNetwork"
private const val MILLISECONDS_TO_WAIT_FOR_INTERNET_CONNECTION = 5000L

suspend fun BaseCoreNoCleanUpTest.withoutInternet(
    // flag is left to be able to run with really disabled internet locally
    emulateInternetTurnOffInsteadOfReallyDisablingIt: Boolean = true,
    block: suspend () -> Unit
) {
    val noMockWebServerAndProvidedBlock = suspend {
        mockWebServerRule.withoutWebServer {
            block()
        }
    }
    if (emulateInternetTurnOffInsteadOfReallyDisablingIt) {
        disableNetworkInCommon {
            noMockWebServerAndProvidedBlock()
        }
    } else {
        withoutWifiAndMobileData {
            noMockWebServerAndProvidedBlock()
        }
    }
}

/**
 * Lightweight internet switch, which disables network only for
 * Mapbox stack.
 */
private inline fun disableNetworkInCommon(block: () -> Unit) {
    val offlineSwitch = OfflineSwitch.getInstance()
    offlineSwitch.isMapboxStackConnected = false
    try {
        block()
    } finally {
        offlineSwitch.isMapboxStackConnected = true
    }
}


/**
 * Disabled Internet on device as if user turned it off in settings.
 *
 * Works stable locally but doesn't work stable on firebase device lab.
 * Reason is unknown at the moment.
 */
private suspend inline fun withoutWifiAndMobileData(block: () -> Unit) {
    val pingAddress = getPingAddress()
    Log.d(LOG_TAG, "Got request to turn internet off")
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

private suspend fun getPingAddress(): InetAddress = withContext(Dispatchers.IO) {
    val host = "api.mapbox.com"
    val result = withTimeoutOrNull<InetAddress?>(MILLISECONDS_TO_WAIT_FOR_INTERNET_CONNECTION) {
        var address: InetAddress? = null
        while (true) {
            try {
                address = InetAddress.getByName("api.mapbox.com")
                break
            } catch (t: Throwable) {
                Log.e(LOG_TAG, "error resolving address for $host", t)
            }
        }
        address
    }
    assumeTrue(
        "wasn't able to get address of $host" +
            " in $MILLISECONDS_TO_WAIT_FOR_INTERNET_CONNECTION ms" +
            ", check your internet connection.",
        result != null,
    )
    Log.d(LOG_TAG, "Successfully resolved address for $host")
    result!!
}

private suspend fun InetAddress.waitForNetworkStatus(isNetworkReachabilityExpected: Boolean) {
    val actualReachability = withTimeoutOrNull(MILLISECONDS_TO_WAIT_FOR_INTERNET_CONNECTION) {
        do {
            Log.d(LOG_TAG, "checking reachability")
            val isReachable = withContext(Dispatchers.IO) {
                isReachable(300)
            }
            Log.d(
                LOG_TAG,
                "is reachable: $isReachable, " +
                    "is network reachability expected $isNetworkReachabilityExpected ",
            )
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
        isNetworkReachabilityExpected == actualReachability,
    )
}
