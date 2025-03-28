package com.mapbox.navigation.core.adas

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.model.VehicleType
import com.mapbox.navigation.base.model.WeatherCondition
import com.mapbox.navigator.Weather
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasSpeedLimitRestrictionTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AdasSpeedLimitRestriction::class.java)
            .verify()

        ToStringVerifier.forClass(AdasSpeedLimitRestriction::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.SpeedLimitRestriction(
            listOf(Weather.WET_ROAD, Weather.RAIN),
            "test-dateTimeCondition",
            listOf(
                com.mapbox.navigator.VehicleType.TRUCK,
                com.mapbox.navigator.VehicleType.TRAILER,
            ),
            listOf(0, 1),
        )

        val platform = AdasSpeedLimitRestriction.createFromNativeObject(native)
        assertEquals(
            listOf(WeatherCondition.WET_ROAD, WeatherCondition.RAIN),
            platform.weatherConditionTypes,
        )
        assertEquals(native.dateTimeCondition, platform.dateTimeCondition)
        assertEquals(
            listOf(VehicleType.TRUCK, VehicleType.TRAILER),
            platform.vehicleTypes,
        )
        assertEquals(native.lanes, platform.lanes)
    }
}
