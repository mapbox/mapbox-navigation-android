package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbInterchange
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class InterchangeFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbInterchange,
) : Interchange(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun name(): String? = fb.name(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("Interchange#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is Interchange && other !is InterchangeFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is InterchangeFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "Interchange(name=${name()})"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbInterchange): Interchange? {
            return if (fb.isPresent) InterchangeFBWrapper(b, fb) else null
        }
    }
}
