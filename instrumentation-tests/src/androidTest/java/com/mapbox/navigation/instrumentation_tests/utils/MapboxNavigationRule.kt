package com.mapbox.navigation.instrumentation_tests.utils

import com.mapbox.navigation.core.MapboxNavigationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Before each test run verifies that [MapboxNavigation] doesn't exist.
 *
 * After each test run ensures that [MapboxNavigation] is destroyed.
 */
class MapboxNavigationRule : TestWatcher() {
    override fun starting(description: Description?) {
        check(MapboxNavigationProvider.isCreated().not())
    }

    override fun finished(description: Description?) {
        MapboxNavigationProvider.destroy()
    }
}
