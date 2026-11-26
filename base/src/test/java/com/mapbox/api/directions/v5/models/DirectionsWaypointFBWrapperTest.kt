package com.mapbox.api.directions.v5.models

import com.google.flatbuffers.FlatBufferBuilder
import com.google.flatbuffers.FlexBuffersBuilder
import com.google.gson.JsonPrimitive
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbDirectionsWaypoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteOrder

class DirectionsWaypointFBWrapperTest {

    // ids of the DirectionsWaypoint fields in the schema
    private companion object {
        const val NAME_FIELD = 2
        const val LOCATION_FIELD = 3
        const val EXTERNAL_ID_FIELD = 5
        const val UNRECOGNIZED_PROPERTIES_FIELD = 6
        const val WAYPOINT_FIELDS_COUNT = 7
    }

    @Test
    fun `external id is returned as unrecognized property`() {
        val waypoint = waypoint(externalId = "origin-stop")

        assertEquals(JsonPrimitive("origin-stop"), waypoint.getUnrecognizedProperty("external_id"))
        assertEquals(setOf("external_id"), waypoint.unrecognizedPropertiesNames)
    }

    @Test
    fun `external id is merged with natively unrecognized properties`() {
        val waypoint = waypoint(
            externalId = "origin-stop",
            unrecognized = mapOf("time_zone" to "America/Los_Angeles"),
        )

        assertEquals(
            mapOf(
                "external_id" to JsonPrimitive("origin-stop"),
                "time_zone" to JsonPrimitive("America/Los_Angeles"),
            ),
            waypoint.unrecognizedJsonProperties,
        )
    }

    @Test
    fun `external id is in the internal unrecognized map`() {
        val waypoint = waypoint(
            externalId = "origin-stop",
            unrecognized = mapOf("time_zone" to "America/Los_Angeles"),
        ) as DirectionsWaypointFBWrapper

        assertEquals(
            mapOf("external_id" to "origin-stop", "time_zone" to "America/Los_Angeles"),
            waypoint.unrecognizedMap(),
        )
    }

    @Test
    fun `empty external id is kept`() {
        val waypoint = waypoint(externalId = "")

        assertEquals(JsonPrimitive(""), waypoint.getUnrecognizedProperty("external_id"))
    }

    @Test
    fun `no external id leaves unrecognized properties empty`() {
        val waypoint = waypoint(externalId = null)

        assertNull(waypoint.getUnrecognizedProperty("external_id"))
        assertNull(waypoint.unrecognizedJsonProperties)
    }

    @Test
    fun `external id null in json is not an unrecognized property`() {
        // the native parser keeps a JSON null in the unrecognized-properties map; the Java parser
        // drops it, and so must the wrapper
        val waypoint = waypoint(externalId = null, unrecognized = mapOf("external_id" to null))

        assertNull(waypoint.getUnrecognizedProperty("external_id"))
        assertTrue(waypoint.unrecognizedPropertiesNames.isEmpty())
    }

    @Test
    fun `external id is serialized by toJson`() {
        val json = waypoint(externalId = "origin-stop").toJson()

        assertTrue(json, json.contains("\"external_id\":\"origin-stop\""))
    }

    private fun waypoint(
        externalId: String?,
        unrecognized: Map<String, String?> = emptyMap(),
    ): DirectionsWaypoint {
        val builder = FlatBufferBuilder()
        val nameOffset = builder.createString("9th Street")
        val externalIdOffset = externalId?.let { builder.createString(it) }
        val unrecognizedOffset = unrecognized.takeIf { it.isNotEmpty() }?.let {
            builder.createByteVector(flexMap(it))
        }

        builder.startTable(WAYPOINT_FIELDS_COUNT)
        builder.addOffset(NAME_FIELD, nameOffset, 0)
        // Coordinate struct: longitude, latitude
        builder.prep(8, 16)
        builder.putDouble(38.577764)
        builder.putDouble(-121.496066)
        builder.addStruct(LOCATION_FIELD, builder.offset(), 0)
        externalIdOffset?.let { builder.addOffset(EXTERNAL_ID_FIELD, it, 0) }
        unrecognizedOffset?.let { builder.addOffset(UNRECOGNIZED_PROPERTIES_FIELD, it, 0) }
        builder.finish(builder.endTable())

        val byteBuffer = builder.dataBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val b = FbBuffer(byteBuffer)
        val fb = FbDirectionsWaypoint(b.indirect(byteBuffer.position()))
        val waypoint = DirectionsWaypointFBWrapper.wrap(b, fb)
        assertNotNull(waypoint)
        return waypoint!!
    }

    private fun flexMap(values: Map<String, String?>): ByteArray {
        val flex = FlexBuffersBuilder()
        val start = flex.startMap()
        values.forEach { (key, value) ->
            if (value == null) flex.putNull(key) else flex.putString(key, value)
        }
        flex.endMap(null, start)
        val buffer = flex.finish()
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }
}
