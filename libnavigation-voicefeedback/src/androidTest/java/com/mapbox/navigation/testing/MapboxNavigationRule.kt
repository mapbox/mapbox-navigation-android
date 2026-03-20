package com.mapbox.navigation.testing

import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.navigation.core.MapboxNavigationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MapboxNavigationRule : TestWatcher() {
    override fun starting(description: Description) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            check(MapboxNavigationProvider.isCreated().not())
        }
    }

    override fun finished(description: Description) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            MapboxNavigationProvider.destroy()
        }
    }
}
