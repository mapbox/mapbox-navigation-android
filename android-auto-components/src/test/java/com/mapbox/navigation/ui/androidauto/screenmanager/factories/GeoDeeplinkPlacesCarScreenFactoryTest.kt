package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import androidx.car.app.CarContext
import androidx.car.app.Screen
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class GeoDeeplinkPlacesCarScreenFactoryTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val carContext: CarContext = mockk()
    private val mapScreen: Screen = mockk()
    private val mapboxScreenManager: MapboxScreenManager = mockk {
        every { createDefaultMapScreenInstead(carContext, any()) } returns mapScreen
    }
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { geoDeeplinkPlacesProvider } returns null
        every { mapboxScreenManager } returns this@GeoDeeplinkPlacesCarScreenFactoryTest
            .mapboxScreenManager
    }

    @Test
    fun `create shows the default map screen when there are no deeplink places`() {
        val screen = GeoDeeplinkPlacesCarScreenFactory(mapboxCarContext).create(carContext)

        assertSame(mapScreen, screen)
    }
}
