package com.mapbox.navigation.core.adas

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigator.SpeedLimitType
import com.mapbox.navigator.SpeedLimitUnit
import com.mapbox.navigator.Weather

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object AdasTypeFactory {

    val NATIVE_VALUE_ON_EDGE_1 = com.mapbox.navigator.ValueOnEdge(0.1f, 0.2, 0.3)
    val VALUE_ON_EDGE_1 = AdasValueOnEdge.createFromNativeObject(NATIVE_VALUE_ON_EDGE_1)

    val NATIVE_VALUE_ON_EDGE_2 = com.mapbox.navigator.ValueOnEdge(0.4f, 0.5, 0.6)
    val VALUE_ON_EDGE_2 = AdasValueOnEdge.createFromNativeObject(NATIVE_VALUE_ON_EDGE_2)

    val NATIVE_SPEED_LIMIT_RESTRICTION = com.mapbox.navigator.SpeedLimitRestriction(
        listOf(Weather.WET_ROAD, Weather.RAIN),
        "test-dateTimeCondition",
        listOf(
            com.mapbox.navigator.VehicleType.TRUCK,
            com.mapbox.navigator.VehicleType.TRAILER,
        ),
        listOf(0, 1),
    )

    val SPEED_LIMIT_RESTRICTION = AdasSpeedLimitRestriction.createFromNativeObject(
        NATIVE_SPEED_LIMIT_RESTRICTION,
    )

    val NATIVE_SPEED_LIMIT_INFO = com.mapbox.navigator.SpeedLimitInfo(
        30,
        SpeedLimitUnit.KILOMETRES_PER_HOUR,
        SpeedLimitType.EXPLICIT,
        NATIVE_SPEED_LIMIT_RESTRICTION,
    )

    val SPEED_LIMIT_INFO = AdasSpeedLimitInfo.createFromNativeObject(NATIVE_SPEED_LIMIT_INFO)
}
