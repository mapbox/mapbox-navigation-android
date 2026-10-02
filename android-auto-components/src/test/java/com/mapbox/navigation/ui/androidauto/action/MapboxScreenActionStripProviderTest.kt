package com.mapbox.navigation.ui.androidauto.action

import androidx.car.app.Screen
import androidx.car.app.model.ActionStrip
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.androidauto.navigation.CarArrivalTrigger
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenEvent
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenOperation
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Suppress("DEPRECATION")
class MapboxScreenActionStripProviderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getActionStrip maps to overridable functions`() {
        val sut = object : MapboxScreenActionStripProvider() {
            val freeDrive: ActionStrip = mockk()
            val search: ActionStrip = mockk()
            val favorites: ActionStrip = mockk()
            val geoDeeplink: ActionStrip = mockk()
            val routePreview: ActionStrip = mockk()
            val activeGuidance: ActionStrip = mockk()

            override fun getFreeDrive(screen: Screen): ActionStrip = freeDrive
            override fun getSearch(screen: Screen): ActionStrip = search
            override fun getFavorites(screen: Screen): ActionStrip = favorites
            override fun getGeoDeeplink(screen: Screen): ActionStrip = geoDeeplink
            override fun getRoutePreview(screen: Screen): ActionStrip = routePreview
            override fun getActiveGuidance(screen: Screen): ActionStrip = activeGuidance
        }

        assertEquals(sut.freeDrive, sut.getActionStrip(mockk(), MapboxScreen.FREE_DRIVE))
        assertEquals(sut.search, sut.getActionStrip(mockk(), MapboxScreen.SEARCH))
        assertEquals(sut.favorites, sut.getActionStrip(mockk(), MapboxScreen.FAVORITES))
        assertEquals(sut.geoDeeplink, sut.getActionStrip(mockk(), MapboxScreen.GEO_DEEPLINK))
        assertEquals(sut.routePreview, sut.getActionStrip(mockk(), MapboxScreen.ROUTE_PREVIEW))
        assertEquals(sut.activeGuidance, sut.getActionStrip(mockk(), MapboxScreen.ACTIVE_GUIDANCE))
    }

    @Test
    fun `getActionStrip can be overridden to customize all screens`() {
        val sut = object : MapboxScreenActionStripProvider() {
            val actionStrip: ActionStrip = mockk()

            override fun getActionStrip(screen: Screen, mapboxScreen: String): ActionStrip {
                return actionStrip
            }
        }

        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.FREE_DRIVE))
        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.ROUTE_PREVIEW))
        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.SEARCH))
        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.FREE_DRIVE))
        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.ACTIVE_GUIDANCE))
        assertEquals(sut.actionStrip, sut.getActionStrip(mockk(), MapboxScreen.GEO_DEEPLINK))
    }

    @Test(expected = NotImplementedError::class)
    fun `getActionStrip throws error when screen is not recognized`() {
        val sut = MapboxScreenActionStripProvider()

        sut.getActionStrip(mockk(), "UnknownScreen")
    }

    @Test
    fun `stop triggers arrival through the attached arrival trigger`() {
        val carArrivalTrigger = mockk<CarArrivalTrigger>(relaxed = true)
        mockkObject(MapboxNavigationApp)
        every {
            MapboxNavigationApp.getObservers(CarArrivalTrigger::class)
        } returns listOf(carArrivalTrigger)
        givenTopScreen(MapboxScreen.NAVIGATION)

        triggerArrivalOnStop()

        verify { carArrivalTrigger.triggerArrival() }
        verify(exactly = 0) { MapboxScreenManager.replaceTop(any()) }
    }

    @Test
    fun `stop without an attached arrival trigger shows arrival in legacy guidance`() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.getObservers(CarArrivalTrigger::class) } returns emptyList()
        givenTopScreen(MapboxScreen.ACTIVE_GUIDANCE)

        triggerArrivalOnStop()

        verify { MapboxScreenManager.replaceTop(MapboxScreen.ARRIVAL) }
    }

    @Test
    fun `stop without an attached arrival trigger and without a screen is ignored`() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.getObservers(CarArrivalTrigger::class) } returns emptyList()
        givenTopScreen(null)

        triggerArrivalOnStop()

        verify(exactly = 0) { MapboxScreenManager.replaceTop(any()) }
    }

    @Test
    fun `stop without an attached arrival trigger shows arrival when guidance is on top`() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.getObservers(CarArrivalTrigger::class) } returns emptyList()
        givenTopScreen(MapboxScreen.NAVIGATION)

        triggerArrivalOnStop()

        verify { MapboxScreenManager.replaceTop(MapboxScreen.ARRIVAL) }
    }

    @Test
    fun `late stop from a guidance template does not replace another screen`() {
        mockkObject(MapboxNavigationApp)
        every { MapboxNavigationApp.getObservers(CarArrivalTrigger::class) } returns emptyList()
        givenTopScreen(MapboxScreen.SEARCH)

        triggerArrivalOnStop()

        verify(exactly = 0) { MapboxScreenManager.replaceTop(any()) }
    }

    private fun givenTopScreen(key: String?) {
        mockkObject(MapboxScreenManager)
        mockkStatic(MapboxScreenManager::class)
        every { MapboxScreenManager.current() } returns
            key?.let { MapboxScreenEvent(it, MapboxScreenOperation.REPLACE_TOP) }
        every { MapboxScreenManager.replaceTop(any()) } just runs
    }
}
