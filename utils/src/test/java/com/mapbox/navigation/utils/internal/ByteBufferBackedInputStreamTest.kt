package com.mapbox.navigation.utils.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class ByteBufferBackedInputStreamTest {

    @Test
    fun emptyBuffer() {
        val input = ByteBuffer.wrap(byteArrayOf())

        val output = ByteBufferBackedInputStream(input)

        val bytes = output.use { it.readBytes() }
        assertEquals(0, bytes.size)
    }

    @Test
    fun filledBuffer() {
        val expected = "ekrigjlwqko2eoi123ywifujenskfgjvenrjkлцоуалцурплцшгрп".toByteArray()
        val input = ByteBuffer.wrap(expected)

        val output = ByteBufferBackedInputStream(input)

        val bytes = output.use { it.readBytes() }
        assertTrue(bytes.contentEquals(expected))
    }
}
