package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class IntRangeTypeAdapter : TypeAdapter<IntRange>() {

    override fun write(out: JsonWriter, value: IntRange?) {
        if (value != null) {
            out.beginObject()
            out.name("first").value(value.first)
            out.name("last").value(value.last)
            out.endObject()
        } else {
            out.nullValue()
        }
    }

    override fun read(`in`: JsonReader): IntRange? {
        val next = `in`.peek()
        if (next == JsonToken.NULL) {
            return null
        }
        `in`.beginObject()
        var first: Int? = null
        var last: Int? = null
        repeat(2) {
            val name = `in`.nextName()
            when (name) {
                "first" -> { first = `in`.nextInt() }
                "last" -> { last = `in`.nextInt() }
            }
        }
        `in`.endObject()
        return first!!..last!!
    }
}
