package com.mapbox.navigation.core.replay.history

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.io.Closeable

/**
 * Lower memory option for reading replay events from a history file.
 */
class ReplayEventStream(
    private val jsonReader: JsonReader
) : Closeable, Iterator<ReplayEventBase> {

    private enum class State {
        INIT,
        READING,
        CLOSED
    }
    private var state = State.INIT
    private var peek: ReplayEventBase? = null

    /**
     * Close the stream, the object cannot be restored.
     */
    override fun close() {
        state = State.CLOSED
        jsonReader.close()
    }

    /**
     * Returns `true` if the iteration has more elements.
     */
    override fun hasNext(): Boolean {
        if (state == State.INIT) {
            jsonReader.beginObject()
            if (jsonReader.hasNext()) {
                val nextName = jsonReader.nextName()
                if (nextName == "events") {
                    jsonReader.beginArray()
                    state = State.READING
                }
            }
        }

        while (state == State.READING && peek == null) {
            val jsonElement = readJson(jsonReader) as JsonObject?
            if (jsonElement != null) {
                peek = mapToEvent(jsonElement)
            }
        }

        return peek != null
    }

    /**
     * Returns the next element in the iteration.
     */
    override fun next(): ReplayEventBase {
        val nextValue = peek
        checkNotNull(nextValue) { "You must first ensure hasNext is true" }
        peek = null
        return nextValue
    }

    private fun readJson(jsonReader: JsonReader): JsonElement? {
        val value = jsonReader.peek()
        return when (jsonReader.peek()) {
            JsonToken.STRING -> JsonPrimitive(jsonReader.nextString())
            JsonToken.NUMBER -> JsonPrimitive(LazilyParsedNumber(jsonReader.nextString()))
            JsonToken.BOOLEAN -> JsonPrimitive(jsonReader.nextBoolean())
            JsonToken.NULL -> JsonNull.INSTANCE
            JsonToken.BEGIN_ARRAY -> {
                val jsonArray = JsonArray()
                jsonReader.beginArray()
                while (jsonReader.hasNext()) {
                    jsonArray.add(readJson(jsonReader))
                }
                jsonReader.endArray()
                jsonArray
            }
            JsonToken.BEGIN_OBJECT -> {
                val jsonObject = JsonObject()
                jsonReader.beginObject()
                while (jsonReader.hasNext()) {
                    jsonObject.add(jsonReader.nextName(), readJson(jsonReader))
                }
                jsonReader.endObject()
                jsonObject
            }
            JsonToken.END_ARRAY -> {
                jsonReader.endArray()
                state = State.CLOSED
                null
            }
            else -> throw IllegalArgumentException(value.toString())
        }
    }

    private fun mapToEvent(jsonObject: JsonObject): ReplayEventBase? {
        return when (val eventType = jsonObject["type"].asString) {
            "updateLocation" -> Gson().fromJson(
                jsonObject.toString(),
                ReplayEventUpdateLocation::class.java
            )
            "getStatus" -> {
                val eventTimestamp =
                    (jsonObject["event_timestamp"] ?: jsonObject["timestamp"]).asDouble
                ReplayEventGetStatus(
                    eventTimestamp = eventTimestamp
                )
            }
            "getStatusMonotonic" -> {
                val eventTimestamp = jsonObject["event_timestamp"].asDouble
                ReplayEventGetStatus(
                    eventTimestamp = eventTimestamp
                )
            }
            "setRoute" -> {
                val directionsRoute = try {
                    if (jsonObject["route"].asString == "{}") {
                        null
                    } else {
                        DirectionsRoute.fromJson(jsonObject["route"].asString)
                    }
                } catch (throwable: Throwable) {
                    println("Unable to setRoute from history file")
                    return null
                }
                ReplaySetRoute(
                    eventTimestamp = jsonObject["event_timestamp"].asDouble,
                    route = directionsRoute
                )
            }
            else -> {
                println("Replay unsupported event $eventType")
                null
            }
        }
    }
}
