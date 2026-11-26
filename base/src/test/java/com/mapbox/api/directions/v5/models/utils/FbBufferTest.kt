package com.mapbox.api.directions.v5.models.utils

import com.google.flatbuffers.FlatBufferBuilder
import com.mapbox.directions.accessors.FbBooleanVector
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbDirectionsWaypoint
import com.mapbox.directions.accessors.FbDoubleVector
import com.mapbox.directions.accessors.FbIntVector
import com.mapbox.directions.accessors.FbStringVector
import com.mapbox.directions.accessors.FbUByteVector
import com.mapbox.directions.accessors.packVector
import com.mapbox.directions.accessors.vectorSize
import com.mapbox.directions.accessors.vectorStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteOrder

class FbBufferTest {

    // vtable offsets of the fields of the table built by testTable()
    private companion object {
        const val INT = 4
        const val ABSENT = 6
        const val STRING = 8
        const val STRUCT = 10
        const val CHILD = 12
        const val INTS = 14
        const val STRINGS = 16
        const val BYTES = 18
        const val CHILDREN = 20
        const val BOOLEAN = 22
        const val BOOLEANS = 24
        const val DOUBLES = 26
        const val EMPTY_BYTES = 28
        const val OUTSIDE_OF_VTABLE = 60
    }

    private class TestTable(val b: FbBuffer, val pos: Int)

    private fun testTable(): TestTable {
        val builder = FlatBufferBuilder(16)
        val string = builder.createString("héllo, 世界")
        val strings = builder.createVectorOfTables(
            intArrayOf(builder.createString("first"), builder.createString("")),
        )
        builder.startVector(4, 3, 4)
        intArrayOf(3, -2, 1).forEach { builder.addInt(it) }
        val ints = builder.endVector()
        val bytes = builder.createByteVector(byteArrayOf(1, -1))
        val emptyBytes = builder.createByteVector(byteArrayOf())
        builder.startVector(1, 2, 1)
        builder.addBoolean(false)
        builder.addBoolean(true)
        val booleans = builder.endVector()
        builder.startVector(8, 2, 8)
        builder.addDouble(-2.5)
        builder.addDouble(1.5)
        val doubles = builder.endVector()
        builder.startTable(1)
        builder.addInt(0, 7, 0)
        val child = builder.endTable()
        val children = builder.createVectorOfTables(intArrayOf(child))

        builder.startTable(13)
        builder.addInt(0, 42, 0)
        builder.addOffset(2, string, 0)
        builder.prep(8, 16)
        builder.putDouble(53.9)
        builder.putDouble(27.5)
        builder.addStruct(3, builder.offset(), 0)
        builder.addOffset(4, child, 0)
        builder.addOffset(5, ints, 0)
        builder.addOffset(6, strings, 0)
        builder.addOffset(7, bytes, 0)
        builder.addOffset(8, children, 0)
        builder.addBoolean(9, true, false)
        builder.addOffset(10, booleans, 0)
        builder.addOffset(11, doubles, 0)
        builder.addOffset(12, emptyBytes, 0)
        builder.finish(builder.endTable())

        val byteBuffer = builder.dataBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val b = FbBuffer(byteBuffer)
        return TestTable(b, b.indirect(byteBuffer.position()))
    }

    @Test
    fun `field offset is zero for absent fields`() {
        val t = testTable()

        assertTrue(t.b.field(t.pos, INT) != 0)
        assertEquals(0, t.b.field(t.pos, ABSENT))
        assertEquals(0, t.b.field(t.pos, OUTSIDE_OF_VTABLE))
    }

    @Test
    fun `scalars are read at the field position`() {
        val t = testTable()

        assertEquals(42, t.b.getInt(t.pos + t.b.field(t.pos, INT)))
        assertTrue(t.b.getBoolean(t.pos + t.b.field(t.pos, BOOLEAN)))
    }

    @Test
    fun `string is decoded as utf8`() {
        val t = testTable()

        assertEquals("héllo, 世界", t.b.string(t.pos, STRING))
        assertEquals("héllo, 世界", t.b.requiredString(t.pos, STRING))
    }

    @Test
    fun `absent string is null, absent required string throws`() {
        val t = testTable()

        assertNull(t.b.string(t.pos, ABSENT))
        assertThrows(IllegalStateException::class.java) { t.b.requiredString(t.pos, ABSENT) }
    }

    @Test
    fun `struct is read in place`() {
        val t = testTable()

        val struct = t.b.struct(t.pos, STRUCT)

        assertEquals(27.5, t.b.getDouble(struct), 0.0)
        assertEquals(53.9, t.b.getDouble(struct + 8), 0.0)
        assertEquals(struct, t.b.requiredStruct(t.pos, STRUCT))
    }

    @Test
    fun `absent struct is zero, absent required struct throws`() {
        val t = testTable()

        assertEquals(0, t.b.struct(t.pos, ABSENT))
        assertThrows(IllegalStateException::class.java) { t.b.requiredStruct(t.pos, ABSENT) }
    }

    @Test
    fun `nested table is followed`() {
        val t = testTable()

        val child = t.b.table(t.pos, CHILD)

        assertEquals(7, t.b.getInt(child + t.b.field(child, 4)))
        assertEquals(child, t.b.requiredTable(t.pos, CHILD))
    }

    @Test
    fun `absent table is zero, absent required table throws`() {
        val t = testTable()

        assertEquals(0, t.b.table(t.pos, ABSENT))
        assertThrows(IllegalStateException::class.java) { t.b.requiredTable(t.pos, ABSENT) }
    }

    @Test
    fun `absent vector is empty`() {
        val t = testTable()

        val vector = t.b.vector(t.pos, ABSENT)

        assertEquals(FbBuffer.EMPTY_VECTOR, vector)
        assertEquals(0, FbIntVector(vector).size)
    }

    @Test
    fun `int vector`() {
        val t = testTable()

        val vector = FbIntVector(t.b.vector(t.pos, INTS))

        assertEquals(listOf(1, -2, 3), (0 until vector.size).map { vector.get(t.b, it) })
    }

    @Test
    fun `double vector`() {
        val t = testTable()

        val vector = FbDoubleVector(t.b.vector(t.pos, DOUBLES))

        assertEquals(listOf(1.5, -2.5), (0 until vector.size).map { vector.get(t.b, it) })
    }

    @Test
    fun `boolean vector`() {
        val t = testTable()

        val vector = FbBooleanVector(t.b.vector(t.pos, BOOLEANS))

        assertEquals(listOf(true, false), (0 until vector.size).map { vector.get(t.b, it) })
    }

    @Test
    fun `string vector`() {
        val t = testTable()

        val vector = FbStringVector(t.b.vector(t.pos, STRINGS))

        assertEquals(listOf("first", ""), (0 until vector.size).map { vector.get(t.b, it) })
    }

    @Test
    fun `vector of tables`() {
        val t = testTable()

        val vector = t.b.vector(t.pos, CHILDREN)
        val child = t.b.indirect(t.b.element(vector, 0, 4))

        assertEquals(1, vectorSize(vector))
        assertEquals(7, t.b.getInt(child + t.b.field(child, 4)))
    }

    @Test
    fun `index out of vector bounds throws`() {
        val t = testTable()
        val vector = FbIntVector(t.b.vector(t.pos, INTS))

        assertThrows(IndexOutOfBoundsException::class.java) { vector.get(t.b, -1) }
        assertThrows(IndexOutOfBoundsException::class.java) { vector.get(t.b, 3) }
        assertThrows(IndexOutOfBoundsException::class.java) {
            FbIntVector(FbBuffer.EMPTY_VECTOR).get(t.b, 0)
        }
    }

    @Test
    fun `ubyte vector is unsigned`() {
        val t = testTable()

        val vector = FbUByteVector(t.b.vector(t.pos, BYTES))

        assertEquals(listOf<UByte>(1u, 255u), (0 until vector.size).map { vector.get(t.b, it) })
    }

    @Test
    fun `ubyte vector as byte buffer is a view of the vector only`() {
        val t = testTable()
        val positionBefore = t.b.byteBuffer.position()
        val limitBefore = t.b.byteBuffer.limit()

        val view = FbUByteVector(t.b.vector(t.pos, BYTES)).asByteBuffer(t.b)!!

        assertEquals(2, view.remaining())
        assertEquals(ByteOrder.LITTLE_ENDIAN, view.order())
        assertEquals(1.toByte(), view.get())
        assertEquals((-1).toByte(), view.get())
        assertEquals(positionBefore, t.b.byteBuffer.position())
        assertEquals(limitBefore, t.b.byteBuffer.limit())
    }

    @Test
    fun `empty or absent ubyte vector has no byte buffer`() {
        val t = testTable()

        assertNull(FbUByteVector(t.b.vector(t.pos, EMPTY_BYTES)).asByteBuffer(t.b))
        assertNull(FbUByteVector(t.b.vector(t.pos, ABSENT)).asByteBuffer(t.b))
    }

    @Test
    fun `vector packing keeps start and size`() {
        listOf(0 to 0, 4 to 1, 123_456 to 7_890, Int.MAX_VALUE to Int.MAX_VALUE).forEach {
            val packed = packVector(it.first, it.second)

            assertEquals(it.first, vectorStart(packed))
            assertEquals(it.second, vectorSize(packed))
        }
    }

    // DirectionsWaypoint fields: hash, is_null, name, location, distance, external_id
    private fun waypoint(
        name: String? = "Main Street",
        withLocation: Boolean = true,
        distance: Double? = null,
    ): Pair<FbBuffer, FbDirectionsWaypoint> {
        val builder = FlatBufferBuilder(16)
        val nameOffset = name?.let { builder.createString(it) }
        builder.startTable(7)
        nameOffset?.let { builder.addOffset(2, it, 0) }
        if (withLocation) {
            builder.prep(8, 16)
            builder.putDouble(53.9)
            builder.putDouble(27.5)
            builder.addStruct(3, builder.offset(), 0)
        }
        distance?.let {
            builder.prep(8, 0)
            builder.putDouble(it)
            builder.slot(4)
        }
        builder.finish(builder.endTable())
        val byteBuffer = builder.dataBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val b = FbBuffer(byteBuffer)
        return b to FbDirectionsWaypoint(b.indirect(byteBuffer.position()))
    }

    @Test
    fun `generated accessor reads fields`() {
        val (b, waypoint) = waypoint(distance = 0.0)

        assertTrue(waypoint.isPresent)
        assertEquals("Main Street", waypoint.name(b))
        assertEquals(27.5, waypoint.location(b).longitude(b), 0.0)
        assertEquals(53.9, waypoint.location(b).latitude(b), 0.0)
        assertTrue(waypoint.hasDistance(b))
        assertEquals(0.0, waypoint.distanceOrNull(b)!!, 0.0)
    }

    @Test
    fun `generated accessor uses defaults for absent optional fields`() {
        val (b, waypoint) = waypoint()

        assertFalse(waypoint.isNull(b))
        assertEquals(0uL, waypoint.hash(b))
        assertFalse(waypoint.hasDistance(b))
        assertNull(waypoint.distanceOrNull(b))
        assertEquals(0.0, waypoint.distance(b), 0.0)
        assertNull(waypoint.externalId(b))
        assertEquals(0, waypoint.unrecognizedProperties(b).size)
    }

    @Test
    fun `generated accessor throws for absent required fields`() {
        val (b, waypoint) = waypoint(name = null, withLocation = false)

        assertThrows(IllegalStateException::class.java) { waypoint.name(b) }
        assertThrows(IllegalStateException::class.java) { waypoint.location(b) }
    }

    @Test
    fun `accessor at position zero is absent`() {
        assertFalse(FbDirectionsWaypoint(0).isPresent)
    }
}
