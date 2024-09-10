package com.mapbox.navigation.testing

import com.mapbox.bindgen.DataRef
import java.nio.ByteBuffer

fun ByteArray.toDataRef(): DataRef {
    val buffer = ByteBuffer.allocateDirect(this.size)
    buffer.put(this)
    return DataRef(buffer)
}
