package com.mapbox.navigation.base.options

import org.junit.Assert.assertEquals
import org.junit.Test

class PredictiveCacheOptionsTest {

    @Test
    fun defaultMapsOptionsList() {
        val expected = PredictiveCacheMapsOptions.Builder().build()
        val actual = PredictiveCacheOptions.Builder().build()

        assertEquals(expected, actual.predictiveCacheMapsOptions)
        assertEquals(listOf(expected), actual.predictiveCacheMapsOptionsList)
    }

    @Test
    fun deprecatedMapsOptions() {
        val mapsOptions = PredictiveCacheMapsOptions.Builder().minZoom(12).maxZoom(14).build()
        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptions(mapsOptions)
            .build()

        assertEquals(mapsOptions, actual.predictiveCacheMapsOptions)
        assertEquals(listOf(mapsOptions), actual.predictiveCacheMapsOptionsList)
    }

    @Test
    fun customMapsOptionsList() {
        val mapsOptions1 = PredictiveCacheMapsOptions.Builder().minZoom(12).maxZoom(14).build()
        val mapsOptions2 = PredictiveCacheMapsOptions.Builder().minZoom(11).maxZoom(13).build()
        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(listOf(mapsOptions1, mapsOptions2))
            .build()

        assertEquals(mapsOptions1, actual.predictiveCacheMapsOptions)
        assertEquals(listOf(mapsOptions1, mapsOptions2), actual.predictiveCacheMapsOptionsList)
    }

    @Test(expected = IllegalArgumentException::class)
    fun customEmptyMapsOptionsList() {
        PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(emptyList())
            .build()
    }

    @Test
    fun customMapsOptionsAfterDeprecatedMapsOptions() {
        val deprecatedMapsOptions = PredictiveCacheMapsOptions.Builder()
            .minZoom(7)
            .maxZoom(9)
            .build()
        val mapsOptions1 = PredictiveCacheMapsOptions.Builder().minZoom(12).maxZoom(14).build()
        val mapsOptions2 = PredictiveCacheMapsOptions.Builder().minZoom(11).maxZoom(13).build()
        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptions(deprecatedMapsOptions)
            .predictiveCacheMapsOptionsList(listOf(mapsOptions1, mapsOptions2))
            .build()

        assertEquals(mapsOptions1, actual.predictiveCacheMapsOptions)
        assertEquals(listOf(mapsOptions1, mapsOptions2), actual.predictiveCacheMapsOptionsList)
    }

    @Test
    fun deprecatedMapsOptionsAfterCustomMapsOptionsList() {
        val deprecatedMapsOptions = PredictiveCacheMapsOptions.Builder()
            .minZoom(7)
            .maxZoom(9)
            .build()
        val mapsOptions1 = PredictiveCacheMapsOptions.Builder().minZoom(12).maxZoom(14).build()
        val mapsOptions2 = PredictiveCacheMapsOptions.Builder().minZoom(11).maxZoom(13).build()
        val actual = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(listOf(mapsOptions1, mapsOptions2))
            .predictiveCacheMapsOptions(deprecatedMapsOptions)
            .build()

        assertEquals(deprecatedMapsOptions, actual.predictiveCacheMapsOptions)
        assertEquals(listOf(deprecatedMapsOptions), actual.predictiveCacheMapsOptionsList)
    }
}
