package com.mapbox.services.android.navigation.v5.navigation

import android.content.Context
import com.mapbox.api.routetiles.v1.MapboxRouteTiles
import org.junit.Test
import org.mockito.Mockito.mock

class OfflineTilesTest {

    @Test(expected = IllegalStateException::class)
    fun checkOfflineTilesBuildIfNoVersionThrowException() {
        val mockMapboxRouteTilesBuilder = mock(MapboxRouteTiles.Builder::class.java)
        val mockContext = mock(Context::class.java)
        val builder = OfflineTiles.builder(mockMapboxRouteTilesBuilder, mockContext).accessToken("")
        builder.build()
    }
}
