package com.mapbox.navigation.instrumentation_tests.utils.tiles

import androidx.test.platform.app.InstrumentationRegistry
import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.instrumentation_tests.R
import com.mapbox.navigation.instrumentation_tests.utils.readRawFileText

object OfflineRegions {
    val Berlin = OfflineRegion(
        id = "berlin-test-tiles",
        geometry = BERLIN_GEOMETRY
    )
}

private val BERLIN_GEOMETRY = FeatureCollection.fromJson(
    readRawFileText(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.raw.geometry_berlin
    )
).features()!!.first().geometry()!!
