package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.mapbox.geojson.FeatureCollection

class FeatureCollectionAdapter : TypeAdapter<FeatureCollection>() {

    override fun write(out: JsonWriter, value: FeatureCollection?) {
        out.value(value?.toJson())
    }

    override fun read(`in`: JsonReader): FeatureCollection? {
        val next = `in`.peek()
        if (next == JsonToken.NULL) {
            return null
        }
        return FeatureCollection.fromJson(`in`.nextString())
    }
}
