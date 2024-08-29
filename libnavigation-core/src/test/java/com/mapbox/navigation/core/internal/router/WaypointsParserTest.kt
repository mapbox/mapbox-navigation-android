package com.mapbox.navigation.core.internal.router

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WaypointsParserTest(
    private val input: JsonElement?,
    private val output: List<DirectionsWaypoint?>?,
    private val description: String,
) {

    companion object {

        @Parameterized.Parameters(name = "{2}")
        @JvmStatic
        fun data(): Collection<Array<Any?>> {
            val validJson = JsonObject().apply {
                add("name", JsonPrimitive("some name"))
                add("distance", JsonPrimitive(12.3))
                add(
                    "location",
                    JsonArray().apply {
                        add(1.2)
                        add(3.4)
                    },
                )
            }
            val secondValidJson = JsonObject().apply {
                add("name", JsonPrimitive("some name 2"))
                add("distance", JsonPrimitive(22.4))
                add(
                    "location",
                    JsonArray().apply {
                        add(2.1)
                        add(4.3)
                    },
                )
            }
            val nonWaypointJson = JsonObject().apply {
                add("name", JsonPrimitive("some name"))
            }
            val validWaypoint = DirectionsWaypoint.builder()
                .name("some name")
                .distance(12.3)
                .rawLocation(doubleArrayOf(1.2, 3.4))
                .build()
            val secondValidWaypoint = DirectionsWaypoint.builder()
                .name("some name 2")
                .distance(22.4)
                .rawLocation(doubleArrayOf(2.1, 4.3))
                .build()
            return listOf(
                arrayOf(null, null, "null"),
                arrayOf(
                    JsonObject().apply { add("key", JsonPrimitive("value")) },
                    null,
                    "non json array",
                ),
                arrayOf(
                    JsonArray(),
                    emptyList<JsonObject>(),
                    "empty json array",
                ),
                arrayOf(
                    JsonArray().apply { add(validJson) },
                    listOf(validWaypoint),
                    "single valid element",
                ),
                arrayOf(
                    JsonArray().apply { add(123) },
                    listOf(null),
                    "single non-json element",
                ),
                arrayOf(
                    JsonArray().apply { add(JsonObject()) },
                    listOf(null),
                    "single empty json element",
                ),
                arrayOf(
                    JsonArray().apply { add(nonWaypointJson) },
                    listOf(null),
                    "single non waypoint element",
                ),
                arrayOf(
                    JsonArray().apply {
                        add(validJson)
                        add(secondValidJson)
                    },
                    listOf(validWaypoint, secondValidWaypoint),
                    "multiple valid elements",
                ),
                arrayOf(
                    JsonArray().apply {
                        add(123)
                        add(JsonObject())
                        add(nonWaypointJson)
                        add(validJson)
                    },
                    listOf(null, null, null, validWaypoint),
                    "multiple elements with invalid",
                ),
            )
        }
    }

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @Test
    fun parse() {
        assertEquals(output, WaypointsParser.parse(input))
    }
}
