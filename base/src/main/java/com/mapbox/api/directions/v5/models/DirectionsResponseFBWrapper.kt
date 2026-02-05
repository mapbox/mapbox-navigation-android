package com.mapbox.api.directions.v5.models

import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper
import com.mapbox.api.directions.v5.models.utils.FlatbuffersListWrapper
import com.mapbox.auto.value.gson.SerializableJsonElement
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObject
import java.nio.ByteBuffer

internal class DirectionsResponseFBWrapper private constructor(
    private val fb: FBDirectionsResponse,
) : DirectionsResponse(), BaseFBWrapper {

    override val unrecognized: ByteBuffer?
        get() = fb.unrecognizedPropertiesAsByteBuffer

    override val unrecognizedPropertiesLength: Int
        get() = fb.unrecognizedPropertiesLength

    override fun code(): String = fb.code

    override fun message(): String? = fb.message

    override fun waypoints(): List<DirectionsWaypoint?>? {
        return FlatbuffersListWrapper.get(fb.waypointsLength) {
            DirectionsWaypointFBWrapper.wrap(fb.waypoints(it))
        }
    }

    override fun routes(): List<DirectionsRoute?> {
        return FlatbuffersListWrapper.get(fb.routesLength) {
            DirectionsRouteFBWrapper.wrap(fb.routes(it))
        } ?: emptyList() // TODO: https://mapbox.atlassian.net/browse/NAVAND-6540
    }

    override fun uuid(): String? = fb.uuid

    override fun metadata(): Metadata? {
        return MetadataFBWrapper.wrap(fb.metadata)
    }

    override fun toBuilder(): Builder? {
        NotSupportedForNativeRouteObject("DirectionsResponse#toBuilder()")
    }

    override fun unrecognized(): Map<String, SerializableJsonElement?>? {
        return super<BaseFBWrapper>.unrecognized()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DirectionsResponseFBWrapper && other.fb === fb) return true
        if (other is DirectionsResponseFBWrapper && efficientEquals(fb, other.fb)) return true

        return false
    }

    override fun hashCode(): Int {
        return efficientHashCode(fb)
    }

    override fun toString(): String {
        return "DirectionsResponse(" +
            "code=${code()}, " +
            "message=${message()}, " +
            "waypoints=${waypoints()}, " +
            "routes=${routes()}, " +
            "uuid=${uuid()}, " +
            "metadata=${metadata()}" +
            ")"
    }

    internal companion object {
        internal fun wrap(fb: FBDirectionsResponse?): DirectionsResponse? {
            return fb?.let { DirectionsResponseFBWrapper(it) }
        }
    }
}
