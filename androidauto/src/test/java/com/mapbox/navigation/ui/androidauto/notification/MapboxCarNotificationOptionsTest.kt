package com.mapbox.navigation.ui.androidauto.notification

import androidx.car.app.CarAppService
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test

class MapboxCarNotificationOptionsTest :
    BuilderTest<MapboxCarNotificationOptions, MapboxCarNotificationOptions.Builder>() {

    override fun getImplementationClass() = MapboxCarNotificationOptions::class

    override fun getFilledUpBuilder(): MapboxCarNotificationOptions.Builder {
        return MapboxCarNotificationOptions.Builder()
            .startAppService(CarAppService::class.java)
    }

    @Test
    override fun trigger() {
        // only used to trigger JUnit4 to run this class if all test cases come from the parent
    }
}
