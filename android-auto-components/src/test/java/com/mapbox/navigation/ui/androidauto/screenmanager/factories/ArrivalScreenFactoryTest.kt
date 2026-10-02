package com.mapbox.navigation.ui.androidauto.screenmanager.factories

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ArrivalScreenFactoryTest {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
    private val mapboxScreenManager: MapboxScreenManager = mockk()
    private val mapboxCarContext: MapboxCarContext = mockk {
        every { mapboxScreenManager } returns this@ArrivalScreenFactoryTest.mapboxScreenManager
    }

    @Before
    fun setup() {
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        mockkObject(MapboxScreenManager)
        every { MapboxScreenManager.replaceTop(any()) } just runs
    }

    @Test
    fun `onFinish clears the routes and shows the default map screen`() {
        every { mapboxScreenManager.defaultMapScreenKey() } returns MapboxScreen.NAVIGATION

        ArrivalScreenFactory(mapboxCarContext).onFinish()

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
        verify { MapboxScreenManager.replaceTop(MapboxScreen.NAVIGATION) }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `onFinish shows FREE_DRIVE when it is the default map screen`() {
        every { mapboxScreenManager.defaultMapScreenKey() } returns MapboxScreen.FREE_DRIVE

        ArrivalScreenFactory(mapboxCarContext).onFinish()

        verify { MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE) }
    }
}
