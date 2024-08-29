package com.mapbox.navigation.base.internal.route

import androidx.annotation.IntDef
import com.google.gson.JsonElement
import com.mapbox.geojson.Point

class Waypoint internal constructor(
    val location: Point,
    val name: String,
    val target: Point?,
    internal val internalType: InternalType,
    val metadata: Map<String, JsonElement>?,
) {

    @Type
    val type: Int = when (internalType) {
        InternalType.Regular -> REGULAR
        InternalType.Silent -> SILENT
        InternalType.EvChargingServer -> EV_CHARGING_SERVER
        InternalType.EvChargingUser -> EV_CHARGING_USER
    }

    companion object {
        const val REGULAR = 1
        const val SILENT = 2
        const val EV_CHARGING_SERVER = 3
        const val EV_CHARGING_USER = 4
    }

    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.TYPE,
    )
    @Retention(AnnotationRetention.BINARY)
    @IntDef(REGULAR, SILENT, EV_CHARGING_SERVER, EV_CHARGING_USER)
    annotation class Type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Waypoint

        if (location != other.location) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (target != other.target) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + type
        result = 31 * result + name.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "Waypoint(" +
            "location=$location, " +
            "type=$type, " +
            "name='$name', " +
            "target=$target, " +
            "metadata=$metadata" +
            ")"
    }

    internal enum class InternalType {
        Regular,
        Silent,
        EvChargingServer,
        EvChargingUser,
    }
}
