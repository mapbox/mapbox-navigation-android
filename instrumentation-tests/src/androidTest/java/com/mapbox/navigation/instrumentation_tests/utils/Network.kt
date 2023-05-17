package com.mapbox.navigation.instrumentation_tests.utils

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
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    try {
        block()
    } finally {
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
}
