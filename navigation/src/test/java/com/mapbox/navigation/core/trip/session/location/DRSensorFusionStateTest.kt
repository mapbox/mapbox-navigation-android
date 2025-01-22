package com.mapbox.navigation.core.trip.session.location

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.location.DRSensorFusionState.COLD_START
import com.mapbox.navigation.core.trip.session.location.DRSensorFusionState.DISABLED
import com.mapbox.navigation.core.trip.session.location.DRSensorFusionState.FAILURE
import com.mapbox.navigation.core.trip.session.location.DRSensorFusionState.INITIALIZATION
import com.mapbox.navigation.core.trip.session.location.DRSensorFusionState.NORMAL_OPERATION
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class DRSensorFusionStateTest {

    @Test
    fun testCreateFromNativeObject() {
        com.mapbox.navigator.DRSensorFusionState.values().forEach {
            val expected = when (it) {
                com.mapbox.navigator.DRSensorFusionState.DISABLED -> DISABLED
                com.mapbox.navigator.DRSensorFusionState.COLD_START -> COLD_START
                com.mapbox.navigator.DRSensorFusionState.INITIALIZATION -> INITIALIZATION
                com.mapbox.navigator.DRSensorFusionState.NORMAL_OPERATION -> NORMAL_OPERATION
                com.mapbox.navigator.DRSensorFusionState.FAILURE -> FAILURE
            }

            assertEquals(expected, DRSensorFusionState.createFromNativeObject(it))
        }
    }
}
