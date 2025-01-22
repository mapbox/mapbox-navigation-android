package com.mapbox.navigation.ui.maps.internal.route.line

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.mapbox.maps.extension.style.expressions.generated.Expression

class ExpressionTypeAdapter : TypeAdapter<Expression>() {

    override fun write(out: JsonWriter, value: Expression?) {
        out.value(value?.toJson())
    }

    override fun read(`in`: JsonReader): Expression? {
        val next = `in`.peek()
        if (next == JsonToken.NULL) {
            return null
        }
        return Expression.fromRaw(`in`.nextString())
    }
}
