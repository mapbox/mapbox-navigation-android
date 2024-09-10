package com.mapbox.navigation.base.internal.utils

import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.utils.internal.ByteBufferBackedInputStream

fun DataRef.toByteArray(): ByteArray {
    return this.buffer.let { ByteBufferBackedInputStream(it).readBytes() }
}
