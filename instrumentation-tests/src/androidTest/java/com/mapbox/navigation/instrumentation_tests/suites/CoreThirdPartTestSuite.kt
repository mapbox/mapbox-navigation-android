package com.mapbox.navigation.instrumentation_tests.suites

import com.mapbox.navigation.instrumentation_tests.core.HistoryRecordingStateChangeObserverTest
import com.mapbox.navigation.instrumentation_tests.core.LongRoutesSanityTest
import com.mapbox.navigation.instrumentation_tests.core.MapboxHistoryTest
import com.mapbox.navigation.instrumentation_tests.core.MapboxNavigationTest
import com.mapbox.navigation.instrumentation_tests.core.NavigationRouteTest
import com.mapbox.navigation.instrumentation_tests.core.PlatformOfflineOnlineSwitchTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    HistoryRecordingStateChangeObserverTest::class,
    LongRoutesSanityTest::class,
    MapboxHistoryTest::class,
    MapboxNavigationTest::class,
    NavigationRouteTest::class,
    PlatformOfflineOnlineSwitchTest::class,
)
class CoreThirdPartTestSuite
