package com.mapbox.navigation.instrumentation_tests.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.testing.ui.BaseCoreNoCleanUpTest
import com.mapbox.navigation.testing.ui.http.MockRequestHandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy

inline fun BaseCoreNoCleanUpTest.withoutInternet(block: () -> Unit) {
    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
    uiAutomation.executeShellCommand("svc wifi disable")
    uiAutomation.executeShellCommand("svc data disable")
    val offlineRequestHandler = MockRequestHandler {
        MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
    }
    mockWebServerRule.requestHandlers.add(0, offlineRequestHandler)
    try {
        block()
    } finally {
        mockWebServerRule.requestHandlers.removeAt(0)
        uiAutomation.executeShellCommand("svc wifi enable")
        uiAutomation.executeShellCommand("svc data enable")
    }
}