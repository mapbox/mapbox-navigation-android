package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.GsonBuilder
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FeatureCollectionAdapterTest {

    @Test
    fun serialiseNull() {
        val adapter = FeatureCollectionAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(FeatureCollection::class.java, adapter)
            .create()
        val fc: FeatureCollection? = null
        val holder = Holder(
            1,
            "aaaaa",
            fc,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    @Test
    fun serialiseNonNull() {
        val adapter = FeatureCollectionAdapter()
        val gson = GsonBuilder()
            .registerTypeAdapter(FeatureCollection::class.java, adapter)
            .create()
        val fc = FeatureCollection.fromFeatures(
            listOf(
                Feature.fromGeometry(LineString.fromPolyline("etylgAl`guhFpJrBh@kHbC{[nAZ", 6)),
                Feature.fromGeometry(LineString.fromPolyline("scj|zA{k_aURjW", 6)),
            ),
        )
        val holder = Holder(
            1,
            "aaaaa",
            fc,
            10,
            "bbbb",
        )
        val json = gson.toJson(holder)
        val restored = gson.fromJson(json, Holder::class.java)
        assertEquals(holder, restored)
    }

    private data class Holder(
        val a: Int,
        val b: String,
        val fc: FeatureCollection?,
        val c: Long,
        val d: String,
    )
}
