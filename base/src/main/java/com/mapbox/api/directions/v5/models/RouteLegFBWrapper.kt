package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class RouteLegFBWrapper
private constructor(private val fb: FBRouteLeg) : RouteLeg(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun viaWaypoints(): List<SilentWaypoint?>? {
        return FlatbuffersListWrapper.get(fb.viaWaypointsLength) {
            SilentWaypointFBWrapper.wrap(fb.viaWaypoints(it))
        }
    }

    override fun distance(): Double? = fb.distance

    override fun duration(): Double? = fb.duration

    override fun durationTypical(): Double? = fb.durationTypical

    override fun summary(): String? = fb.summary

    override fun admins(): List<Admin?>? {
        return FlatbuffersListWrapper.get(fb.adminsLength) {
            AdminFBWrapper.wrap(fb.admins(it))
        }
    }

    override fun steps(): List<LegStep?>? {
        return FlatbuffersListWrapper.get(fb.stepsLength) {
            LegStepFBWrapper.wrap(fb.steps(it))
        }
    }

    override fun incidents(): List<Incident?>? {
        return FlatbuffersListWrapper.get(fb.incidentsLength) {
            IncidentFBWrapper.wrap(fb.incidents(it))
        }
    }

    override fun annotation(): LegAnnotation? {
        return LegAnnotationFBWrapper.wrap(fb.annotation)
    }

    override fun closures(): List<Closure?>? {
        return FlatbuffersListWrapper.get(fb.closuresLength) {
            ClosureFBWrapper.wrap(fb.closures(it))
        }
    }

    override fun notifications(): List<Notification?>? {
        return FlatbuffersListWrapper.get(fb.notificationsLength) {
            NotificationFBWrapper.wrap(fb.notifications(it))
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

    internal companion object {
        internal fun wrap(fb: FBRouteLeg?): RouteLeg? {
            return when {
                fb == null -> null
                fb.isNull -> null
                else -> RouteLegFBWrapper(fb)
            }
        }
    }
}
