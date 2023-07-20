package com.mapbox.navigation.instrumentation_tests.utils.routes

import com.mapbox.navigation.base.route.NavigationRoute

fun NavigationRoute.getChargingStationIds(): List<String?> {
    return getWaypointsMetadata("station_id")
}

fun NavigationRoute.getChargingStationPowersKW(): List<String?> {
    return getWaypointsMetadata("power_kw")
}

fun NavigationRoute.getChargingStationPowerCurrentTypes(): List<String?> {
    return getWaypointsMetadata("current_type")
}

fun NavigationRoute.getChargingStationTypes(): List<String?> {
    return getWaypointsMetadata("type")
}

fun NavigationRoute.getWaypointsMetadata(fieldName: String): List<String?> {
    return this.waypoints?.map {
        it.getUnrecognizedProperty("metadata")
            ?.asJsonObject
            ?.get(fieldName)
            ?.asString
    } ?: emptyList()
}
