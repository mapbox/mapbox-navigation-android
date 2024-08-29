package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AttitudeDataTest {

    @Test
    fun testMapToNativeDegrees() {
        val testData = AttitudeData(
            pitch = 15.degrees,
            yaw = 30.degrees,
            roll = 60.degrees,
        )

        val native = testData.mapToNative()

        assertEquals(Math.toRadians(15.0).toFloat(), native.pitch)
        assertEquals(Math.toRadians(30.0).toFloat(), native.yaw)
        assertEquals(Math.toRadians(60.0).toFloat(), native.roll)
    }

    @Test
    fun testMapToNativeRadians() {
        val testData = AttitudeData(
            pitch = 0.5.radians,
            yaw = 0.7.radians,
            roll = 1.2.radians,
        )

        val native = testData.mapToNative()

        assertEquals(0.5F, native.pitch)
        assertEquals(0.7F, native.yaw)
        assertEquals(1.2F, native.roll)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AttitudeData::class.java)
            .verify()

        ToStringVerifier.forClass(AttitudeData::class.java)
            .verify()
    }
}
