package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    EvAlternativesTest::class,
    EvOfflineTest::class,
    EVRerouteTest::class,
    EVRouteRefreshTest::class,
)
class CoreSecondPartTestSuite
