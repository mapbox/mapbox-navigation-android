package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.ui.SanityUiRouteTest
import com.mapbox.navigation.instrumentation_tests.ui.SimpleMapViewNavigationTest
import com.mapbox.navigation.instrumentation_tests.ui.camera.NavigationCameraTest
import com.mapbox.navigation.instrumentation_tests.ui.dropin.NavigationViewLifecycleTest
import com.mapbox.navigation.instrumentation_tests.ui.routeline.AlternativeRouteSelectionTest
import com.mapbox.navigation.instrumentation_tests.ui.routeline.RouteLineLayersTest
import com.mapbox.navigation.instrumentation_tests.ui.routeline.SetRouteOrderTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    SanityUiRouteTest::class,
    SimpleMapViewNavigationTest::class,
    NavigationCameraTest::class,
    NavigationViewLifecycleTest::class,
    AlternativeRouteSelectionTest::class,
    RouteLineLayersTest::class,
    SetRouteOrderTest::class,
)
class UiTestSuite
