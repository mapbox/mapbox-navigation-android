package com.mapbox.navigation.ui.androidauto.action

import androidx.car.app.Screen
import androidx.car.app.model.ActionStrip
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxScreenActionStripProviderTest {

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
}
