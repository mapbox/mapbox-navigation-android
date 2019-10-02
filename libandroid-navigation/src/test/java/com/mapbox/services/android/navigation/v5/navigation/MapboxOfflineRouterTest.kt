package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class MapboxOfflineRouterTest {

    @Test
    fun initializeOfflineData_filePathIncludesVersion() {
        val tilePath = "/some/path/"
        val offlineNavigator = mock(OfflineNavigator::class.java)
        val callback = mock(OnOfflineTilesConfiguredCallback::class.java)
        val offlineRouter = buildRouter(tilePath, offlineNavigator)

        offlineRouter.configure("version", callback)

        verify(offlineNavigator).configure("/some/path/version", callback)
    }

    @Test
    fun findOfflineRoute_offlineNavigatorIsCalled() {
        val offlineNavigator = mock(OfflineNavigator::class.java)
        val offlineRoute = mock(OfflineRoute::class.java)
        val callback = mock(OnOfflineRouteFoundCallback::class.java)
        val offlineRouter = buildRouter(offlineNavigator)

        offlineRouter.findRoute(offlineRoute, callback)

        verify(offlineNavigator).retrieveRouteFor(offlineRoute, callback)
    }

    @Test
    fun fetchAvailableTileVersions() {
        val accessToken = "access_token"
        val callback = mock(OnTileVersionsFoundCallback::class.java)
        val offlineTileVersions = mock(OfflineTileVersions::class.java)
        val offlineRouter = buildRouter(offlineTileVersions)

        offlineRouter.fetchAvailableTileVersions(accessToken, callback)

        verify(offlineTileVersions).fetchRouteTileVersions(accessToken, callback)
    }

    @Test
    fun checksRemoveTiles() {
        val aTilePath = "/some/path/"
        val anOfflineNavigator = mock(OfflineNavigator::class.java)
        val theOfflineRouter = buildRouter(aTilePath, anOfflineNavigator)
        val southwest = Point.fromLngLat(1.0, 2.0)
        val northeast = Point.fromLngLat(3.0, 4.0)
        val aBoundingBox = BoundingBox.fromPoints(southwest, northeast)
        val aCallback = mock(OnOfflineTilesRemovedCallback::class.java)

        theOfflineRouter.removeTiles("a_version", aBoundingBox, aCallback)

        verify(anOfflineNavigator).removeTiles(
            eq("/some/path/a_version"), eq(southwest), eq(northeast),
            eq(aCallback)
        )
    }

    private fun buildRouter(
        tilePath: String,
        offlineNavigator: OfflineNavigator
    ): MapboxOfflineRouter {
        return MapboxOfflineRouter(
            tilePath,
            offlineNavigator,
            mock(OfflineTileVersions::class.java)
        )
    }

    private fun buildRouter(offlineNavigator: OfflineNavigator): MapboxOfflineRouter {
        return MapboxOfflineRouter("", offlineNavigator, mock(OfflineTileVersions::class.java))
    }

    private fun buildRouter(offlineTileVersions: OfflineTileVersions): MapboxOfflineRouter {
        return MapboxOfflineRouter("", mock(OfflineNavigator::class.java), offlineTileVersions)
    }
}
