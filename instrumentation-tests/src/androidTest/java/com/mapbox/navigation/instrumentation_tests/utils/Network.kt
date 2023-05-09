package com.mapbox.navigation.instrumentation_tests.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest

suspend inline fun BaseCoreNoCleanUpTest.withoutInternet(block: () -> Unit) {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    mockWebServerRule.stop()
    try {
        block()
    } finally {
        mockWebServerRule.restart()
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
}