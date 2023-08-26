package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.RefreshTtlTest
import com.mapbox.navigation.instrumentation_tests.core.ReplayLocationTest
import com.mapbox.navigation.instrumentation_tests.core.ReplayRouteSessionTest
import com.mapbox.navigation.instrumentation_tests.core.RouteAlternativesTest
import com.mapbox.navigation.instrumentation_tests.core.RouteRefreshOnDemandTest
import com.mapbox.navigation.instrumentation_tests.core.RouteRefreshTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    RefreshTtlTest::class,
    ReplayLocationTest::class,
    ReplayRouteSessionTest::class,
    RouteAlternativesTest::class,
    RouteRefreshOnDemandTest::class,
    RouteRefreshTest::class,
)
class CoreFourthPartTestSuite
