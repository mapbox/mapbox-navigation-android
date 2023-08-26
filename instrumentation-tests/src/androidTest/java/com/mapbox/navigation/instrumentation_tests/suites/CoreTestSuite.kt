package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.BannerAndVoiceInstructionsTest
import com.mapbox.navigation.instrumentation_tests.core.ClosuresTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    BannerAndVoiceInstructionsTest::class,
    ClosuresTest::class,
)
class CoreTestSuite
