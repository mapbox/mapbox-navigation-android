package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.RoutesPreviewTest
import com.mapbox.navigation.instrumentation_tests.core.SanityCoreRouteTest
import com.mapbox.navigation.instrumentation_tests.core.SetRoutesTest
import com.mapbox.navigation.instrumentation_tests.core.TripSessionsBillingTest
import com.mapbox.navigation.instrumentation_tests.core.UpcomingRouteObjectsTest
import com.mapbox.navigation.instrumentation_tests.core.WaypointsTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    RoutesPreviewTest::class,
    SanityCoreRouteTest::class,
    SetRoutesTest::class,
    TripSessionsBillingTest::class,
    UpcomingRouteObjectsTest::class,
    WaypointsTest::class,
)
class CoreFifthPartTestSuite
