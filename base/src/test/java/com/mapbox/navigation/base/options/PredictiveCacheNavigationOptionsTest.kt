package com.mapbox.navigation.base.options

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PredictiveCacheNavigationOptionsTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(PredictiveCacheNavigationOptions::class.java)
            .verify()

        ToStringVerifier.forClass(PredictiveCacheNavigationOptions::class.java)
            .verify()
    }

    @Test
    fun testDefaultBuilderParameters() {
        val options = PredictiveCacheNavigationOptions.Builder().build()
        assertEquals(null, options.tilesDataset)
        assertEquals(null, options.tilesVersion)
        assertEquals(false, options.includeAdas)
        assertEquals(
            PredictiveCacheLocationOptions.Builder().build(),
            options.predictiveCacheLocationOptions,
        )
    }

    @Test
    fun testBuilderParameters() {
        val locationOptions = PredictiveCacheLocationOptions.Builder()
            .routeBufferRadiusInMeters(100)
            .currentLocationRadiusInMeters(500)
            .destinationLocationRadiusInMeters(1000)
            .build()

        val options = PredictiveCacheNavigationOptions.Builder()
            .tilesConfiguration(
                tilesDataset = "test-dataset",
                tilesVersion = "test-version",
                includeAdas = true,
            )
            .predictiveCacheLocationOptions(locationOptions)
            .build()

        assertEquals("test-dataset", options.tilesDataset)
        assertEquals("test-version", options.tilesVersion)
        assertEquals(true, options.includeAdas)
        assertEquals(
            locationOptions,
            options.predictiveCacheLocationOptions,
        )
    }
}
