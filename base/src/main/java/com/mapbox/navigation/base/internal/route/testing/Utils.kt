package com.mapbox.navigation.base.internal.route.testing

import com.mapbox.bindgen.DataRef
import java.nio.ByteBuffer

fun String.toDataRefJava(): DataRef {
    val bytes = encodeToByteArray()
    val buffer = ByteBuffer.allocateDirect(bytes.size)
    val result = DataRef(buffer)
    result.buffer.put(bytes)
    return result
}
