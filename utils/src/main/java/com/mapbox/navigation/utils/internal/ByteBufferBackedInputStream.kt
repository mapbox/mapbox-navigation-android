package com.mapbox.navigation.utils.internal

import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferBackedInputStream(
    private val buffer: ByteBuffer,
) : InputStream() {

    init {
        buffer.position(0)
    }

    override fun read(): Int {
        return if (!buffer.hasRemaining()) {
            -1
        } else {
            buffer.get().toInt()
        }
    }

    override fun available(): Int {
        return buffer.remaining()
    }

    override fun read(bytes: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }
        val bytesToRead = len.coerceAtMost(buffer.remaining())
        buffer.get(bytes, off, bytesToRead)
        return bytesToRead
    }
}
