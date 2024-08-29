package com.mapbox.navigation.base.options

import com.mapbox.common.TilesetDescriptor
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PredictiveCacheOptionsTest {

    private val mockTilesetDescriptor1: TilesetDescriptor = mockk()
    private val mockTilesetDescriptor2: TilesetDescriptor = mockk()

    @Test
    fun defaultMapsOptionsList() {
        val expected = PredictiveCacheMapsOptions.Builder().build()
        val actual = PredictiveCacheOptions.Builder().build()

        assertEquals(listOf(expected), actual.predictiveCacheMapsOptionsList)
    }

    @Test
    fun customMapsOptionsList() {
        val mapsOptions1 = PredictiveCacheMapsOptions.Builder().minZoom(12).maxZoom(14).build()
        val mapsOptions2 = PredictiveCacheMapsOptions.Builder().minZoom(11).maxZoom(13).build()
        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(listOf(mapsOptions1, mapsOptions2))
            .build()

        assertEquals(listOf(mapsOptions1, mapsOptions2), actual.predictiveCacheMapsOptionsList)
    }

    @Test
    fun customSearchOptionsList() {
        val options1 = PredictiveCacheSearchOptions
            .Builder(mockTilesetDescriptor1)
            .build()

        val options2 = PredictiveCacheSearchOptions
            .Builder(mockTilesetDescriptor2)
            .build()

        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheSearchOptionsList(listOf(options1, options2))
            .build()

        assertEquals(
            listOf(options1, options2),
            actual.predictiveCacheSearchOptionsList,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun customEmptyMapsOptionsList() {
        PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(emptyList())
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun customEmptySearchOptionsList() {
        PredictiveCacheOptions.Builder()
            .predictiveCacheSearchOptionsList(emptyList())
            .build()
    }
}
