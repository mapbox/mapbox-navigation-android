package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.geojson.BoundingBox
import com.mapbox.geojson.Point
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MapboxOfflineRouterTest {

    @Test
    fun initializeOfflineData_filePathIncludesVersion() {
        val tilePath = "/some/path/"
        val offlineNavigator = mockk<OfflineNavigator>(relaxUnitFun = true)
        val callback = mockk<OnOfflineTilesConfiguredCallback>(relaxUnitFun = true)
        val offlineRouter = buildRouter(tilePath, offlineNavigator)

        offlineRouter.configure("version", callback)

        verify { offlineNavigator.configure("/some/path/version", callback) }
    }

    @Test
    fun findOfflineRoute_offlineNavigatorIsCalled() {
        val offlineNavigator = mockk<OfflineNavigator>(relaxUnitFun = true)
        val offlineRoute = mockk<OfflineRoute>(relaxUnitFun = true)
        val callback = mockk<OnOfflineRouteFoundCallback>(relaxUnitFun = true)
        val offlineRouter = buildRouter(offlineNavigator)

        offlineRouter.findRoute(offlineRoute, callback)

        verify { offlineNavigator.retrieveRouteFor(offlineRoute, callback) }
    }

    @Test
    fun fetchAvailableTileVersions() {
        val accessToken = "access_token"
        val callback = mockk<OnTileVersionsFoundCallback>(relaxUnitFun = true)
        val offlineTileVersions = mockk<OfflineTileVersions>(relaxUnitFun = true)
        val offlineRouter = buildRouter(offlineTileVersions)

        offlineRouter.fetchAvailableTileVersions(accessToken, callback)

        verify { offlineTileVersions.fetchRouteTileVersions(accessToken, callback) }
    }

    @Test
    fun checksRemoveTiles() {
        val aTilePath = "/some/path/"
        val anOfflineNavigator = mockk<OfflineNavigator>(relaxUnitFun = true)
        val theOfflineRouter = buildRouter(aTilePath, anOfflineNavigator)
        val southwest = Point.fromLngLat(1.0, 2.0)
        val northeast = Point.fromLngLat(3.0, 4.0)
        val aBoundingBox = BoundingBox.fromPoints(southwest, northeast)
        val aCallback = mockk<OnOfflineTilesRemovedCallback>()

        theOfflineRouter.removeTiles("a_version", aBoundingBox, aCallback)

        verify {
            anOfflineNavigator.removeTiles(
                eq("/some/path/a_version"), eq(southwest), eq(northeast),
                eq(aCallback)
            )
        }
    }

    private fun buildRouter(
        tilePath: String,
        offlineNavigator: OfflineNavigator
    ): MapboxOfflineRouter {
        return MapboxOfflineRouter(
            tilePath,
            offlineNavigator,
            mockk(relaxUnitFun = true)
        )
    }

    private fun buildRouter(offlineNavigator: OfflineNavigator): MapboxOfflineRouter {
        return MapboxOfflineRouter("", offlineNavigator, mockk(relaxUnitFun = true))
    }

    private fun buildRouter(offlineTileVersions: OfflineTileVersions): MapboxOfflineRouter {
        return MapboxOfflineRouter("", mockk(relaxUnitFun = true), offlineTileVersions)
    }
}
