package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RouteLegFBWrapper(private val fb: FBRouteLeg) : RouteLeg(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun viaWaypoints(): List<SilentWaypoint?>? {
        return FlatbuffersListWrapper.get(fb.viaWaypointsLength) {
            fb.viaWaypoints(it)?.let { waypoint -> SilentWaypointFBWrapper(waypoint) }
        }
    }

    override fun distance(): Double? = fb.distance

    override fun duration(): Double? = fb.duration

    override fun durationTypical(): Double? = fb.durationTypical

    override fun summary(): String? = fb.summary

    override fun admins(): List<Admin?>? {
        return FlatbuffersListWrapper.get(fb.adminsLength) {
            fb.admins(it)?.let { admin -> AdminFBWrapper(admin) }
        }
    }

    override fun steps(): List<LegStep?>? {
        return FlatbuffersListWrapper.get(fb.stepsLength) {
            fb.steps(it)?.let { step -> LegStepFBWrapper(step) }
        }
    }

    override fun incidents(): List<Incident?>? {
        return FlatbuffersListWrapper.get(fb.incidentsLength) {
            fb.incidents(it)?.let { incident -> IncidentFBWrapper(incident) }
        }
    }
    override fun annotation(): LegAnnotation? {
        return fb.annotation?.let { LegAnnotationFBWrapper(it) }
    }

    override fun closures(): List<Closure?>? {
        return FlatbuffersListWrapper.get(fb.closuresLength) {
            fb.closures(it)?.let { closure -> ClosureFBWrapper(closure) }
        }
    }

    override fun notifications(): List<Notification?>? {
        return FlatbuffersListWrapper.get(fb.notificationsLength) {
            fb.notifications(it)?.let { notification ->
                NotificationFBWrapper(notification)
            }
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
        if (other is RouteLegFBWrapper && other.fb === fb) return true
        if (other is RouteLegFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

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
}
