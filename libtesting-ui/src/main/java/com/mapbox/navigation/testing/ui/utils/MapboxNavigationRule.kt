package com.mapbox.navigation.testing.ui.utils

import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Before each test run verifies that [MapboxNavigation] doesn't exist.
 *
 * After each test run ensures that [MapboxNavigation] is destroyed.
 */
class MapboxNavigationRule : TestWatcher() {
    override fun starting(description: Description) {
        runOnMainSync {
            check(MapboxNavigationProvider.isCreated().not())
        }
    }

    override fun finished(description: Description) {
        runOnMainSync {
            MapboxNavigationProvider.destroy()
        }
    }
}
