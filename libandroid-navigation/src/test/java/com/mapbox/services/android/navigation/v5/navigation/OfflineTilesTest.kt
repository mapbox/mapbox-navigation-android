package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.routetiles.v1.MapboxRouteTiles
import org.junit.Test
import org.mockito.Mockito.mock

class OfflineTilesTest {

    @Test(expected = IllegalStateException::class)
    fun checkOfflineTilesBuildIfNoVersionThrowException() {
        val mockMapboxRouteTilesBuilder = mock(MapboxRouteTiles.Builder::class.java)

        val builder = OfflineTiles.builder(mockMapboxRouteTilesBuilder).accessToken("")
        builder.build()
    }
}
