package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.ui.SanityUiRouteTest
import com.mapbox.navigation.instrumentation_tests.ui.SimpleMapViewNavigationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SanityUiRouteTest::class,
    SimpleMapViewNavigationTest::class,
)
class UiTestSuite
