package com.mapbox.navigation.instrumentation_tests.utils

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest

suspend fun BaseCoreNoCleanUpTest.withoutInternet(block: suspend () -> Unit) {
    withoutWifiAndMobileData {
        mockWebServerRule.withoutWebServer {
            block()
        }
    }
}

suspend fun withoutWifiAndMobileData(block: suspend () -> Unit) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val uiAutomation = instrumentation.uiAutomation
    uiAutomation.grantRuntimePermission(
        instrumentation.context.packageName,
        Manifest.permission.CHANGE_WIFI_STATE
    )
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    try {
        block()
    } finally {
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
}
