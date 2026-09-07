package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class NotificationDetailsFBWrapper private constructor(
    private val fb: FBNotificationDetails,
) : NotificationDetails(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun requestedValue(): String? = fb.requestedValue

    override fun actualValue(): String? = fb.actualValue

    override fun unit(): String? = fb.unit

    override fun message(): String? = fb.message

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
        return fb.contentEquals(other.fb)
    }

    override fun hashCode() = fb.contentHash().toHashCode()

    override fun toString(): String {
        return "NotificationDetails(" +
            "requestedValue=${requestedValue()}, " +
            "actualValue=${actualValue()}, " +
            "unit=${unit()}, " +
            "message=${message()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBNotificationDetails?): NotificationDetails? {
            return fb?.let { NotificationDetailsFBWrapper(it) }
        }
    }
}
