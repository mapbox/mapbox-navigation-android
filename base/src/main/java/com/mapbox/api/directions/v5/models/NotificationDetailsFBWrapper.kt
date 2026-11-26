package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbNotificationDetails
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class NotificationDetailsFBWrapper private constructor(
    private val b: FbBuffer,
    private val fb: FbNotificationDetails,
) : NotificationDetails(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun requestedValue(): String? = fb.requestedValue(b)

    override fun actualValue(): String? = fb.actualValue(b)

    override fun unit(): String? = fb.unit(b)

    override fun message(): String? = fb.message(b)

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("NotificationDetails#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is NotificationDetails && other !is NotificationDetailsFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is NotificationDetailsFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "NotificationDetails(" +
            "requestedValue=${requestedValue()}, " +
            "actualValue=${actualValue()}, " +
            "unit=${unit()}, " +
            "message=${message()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbNotificationDetails): NotificationDetails? {
            return if (fb.isPresent) NotificationDetailsFBWrapper(b, fb) else null
        }
    }
}
