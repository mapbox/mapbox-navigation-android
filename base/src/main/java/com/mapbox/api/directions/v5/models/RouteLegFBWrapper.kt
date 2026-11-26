package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.api.directions.v5.models.utils.throwNotComparableRouteObjects
import com.mapbox.api.directions.v5.models.utils.toHashCode
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.directions.accessors.FbBuffer
import com.mapbox.directions.accessors.FbRouteLeg
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RouteLegFBWrapper
private constructor(
    private val b: FbBuffer,
    private val fb: FbRouteLeg,
) : RouteLeg(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedProperties(b).asByteBuffer(b)

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedProperties(b).size

    override fun viaWaypoints(): List<SilentWaypoint?>? {
        val viaWaypoints = fb.viaWaypoints(b)
        return FlatbuffersListWrapper.get(viaWaypoints.size) {
            SilentWaypointFBWrapper.wrap(b, viaWaypoints.get(b, it))
        }
    }

    override fun distance(): Double? = fb.distanceOrNull(b)

    override fun duration(): Double? = fb.durationOrNull(b)

    override fun durationTypical(): Double? = fb.durationTypicalOrNull(b)

    override fun summary(): String? = fb.summary(b)

    override fun admins(): List<Admin?>? {
        val admins = fb.admins(b)
        return FlatbuffersListWrapper.get(admins.size) {
            AdminFBWrapper.wrap(b, admins.get(b, it))
        }
    }

    override fun steps(): List<LegStep?>? {
        val steps = fb.steps(b)
        return FlatbuffersListWrapper.get(steps.size) {
            LegStepFBWrapper.wrap(b, steps.get(b, it))
        }
    }

    override fun incidents(): List<Incident?>? {
        val incidents = fb.incidents(b)
        return FlatbuffersListWrapper.get(incidents.size) {
            IncidentFBWrapper.wrap(b, incidents.get(b, it))
        }
    }

    override fun annotation(): LegAnnotation? {
        return LegAnnotationFBWrapper.wrap(b, fb.annotation(b))
    }

    override fun closures(): List<Closure?>? {
        val closures = fb.closures(b)
        return FlatbuffersListWrapper.get(closures.size) {
            ClosureFBWrapper.wrap(b, closures.get(b, it))
        }
    }

    override fun notifications(): List<Notification?>? {
        val notifications = fb.notifications(b)
        return FlatbuffersListWrapper.get(notifications.size) {
            NotificationFBWrapper.wrap(b, notifications.get(b, it))
        }
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("RouteLeg#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (other is RouteLeg && other !is RouteLegFBWrapper) {
            throwNotComparableRouteObjects()
        }
        if (other !is RouteLegFBWrapper) return false
        return fb.contentEquals(b, other.fb, other.b)
    }

    override fun hashCode() = fb.hash(b).toHashCode()

    override fun toString(): String {
        return "RouteLeg(" +
            "viaWaypoints=${viaWaypoints()}, " +
            "distance=${distance()}, " +
            "duration=${duration()}, " +
            "durationTypical=${durationTypical()}, " +
            "summary=${summary()}, " +
            "admins=${admins()}, " +
            "steps=${steps()}, " +
            "incidents=${incidents()}, " +
            "annotation=${annotation()}, " +
            "closures=${closures()}, " +
            "notifications=${notifications()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(b: FbBuffer, fb: FbRouteLeg): RouteLeg? {
            return when {
                !fb.isPresent -> null
                fb.isNull(b) -> null
                else -> RouteLegFBWrapper(b, fb)
            }
        }
    }
}
