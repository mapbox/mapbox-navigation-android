package com.mapbox.navigation.utils.internal

import com.mapbox.bindgen.DataRef
import java.io.InputStreamReader
import java.io.Reader

fun DataRef.isNotEmpty(): Boolean {
    val buffer = this.buffer
    buffer.position(0)
    return buffer.hasRemaining()
}

fun DataRef.toReader(): Reader {
    val stream = ByteBufferBackedInputStream(buffer)
    return InputStreamReader(stream, Charsets.UTF_8)
}

fun String.toDataRef(): DataRef {
    val bytes = encodeToByteArray()
    val result = DataRef.allocateNative(bytes.size)
    result.buffer.put(bytes)
    return result
}
