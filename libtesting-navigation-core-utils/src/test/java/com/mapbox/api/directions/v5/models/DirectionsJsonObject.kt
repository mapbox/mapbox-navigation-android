package com.mapbox.api.directions.v5.models

import com.mapbox.auto.value.gson.SerializableJsonElement

class TestDirectionsJsonObject(
    val unrecognized: Map<String, SerializableJsonElement?>?
) : DirectionsJsonObject() {

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return unrecognized
    }
}
