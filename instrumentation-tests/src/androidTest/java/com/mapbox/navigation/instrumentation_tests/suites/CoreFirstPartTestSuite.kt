package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.BannerAndVoiceInstructionsTest
import com.mapbox.navigation.instrumentation_tests.core.ClosuresTest
import com.mapbox.navigation.instrumentation_tests.core.CopilotIntegrationTest
import com.mapbox.navigation.instrumentation_tests.core.CoreRerouteTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    BannerAndVoiceInstructionsTest::class,
    ClosuresTest::class,
    CopilotIntegrationTest::class,
    CoreRerouteTest::class,
)
class CoreFirstPartTestSuite
