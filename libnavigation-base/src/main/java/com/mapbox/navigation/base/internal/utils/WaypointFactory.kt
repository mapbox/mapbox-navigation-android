package com.mapbox.navigation.base.internal.utils

import androidx.annotation.VisibleForTesting
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.route.Waypoint
import org.jetbrains.annotations.TestOnly

@VisibleForTesting
object WaypointFactory {
    @TestOnly
    fun provideWaypoint(
        location: Point,
        name: String,
        target: Point?,
        @Waypoint.Type type: Int,
    ): Waypoint = Waypoint(
        location,
        name,
        target,
        when (type) {
            Waypoint.REGULAR -> Waypoint.InternalType.Regular
            Waypoint.SILENT -> Waypoint.InternalType.Silent
            Waypoint.EV_CHARGING_SERVER -> Waypoint.InternalType.EvChargingServer
            Waypoint.EV_CHARGING_USER -> Waypoint.InternalType.EvChargingUser
            else -> throw IllegalStateException("Unknown waypoint type $type")
        },
    )
}
