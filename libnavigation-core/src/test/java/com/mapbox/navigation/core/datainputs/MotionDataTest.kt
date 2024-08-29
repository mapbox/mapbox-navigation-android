package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.geometry.Point3D
import com.mapbox.navigation.base.internal.mapToNative
import com.mapbox.navigation.base.physics.AngularVelocity3D
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MotionDataTest {

    @Test
    fun testMapToNativeDegrees() {
        val attitude = AttitudeData(15.degrees, 30.degrees, 60.degrees)
        val rotationRate = AngularVelocity3D.degreesPerSecond(
            5.0,
            10.0,
            15.0,
        )

        val gravityAcceleration = Point3D(2.0, 2.1, 2.2)
        val userAcceleration = Point3D(3.0, 3.1, 3.2)
        val magneticField = Point3D(4.0, 4.1, 4.2)
        val testData = MotionData(
            attitude = attitude,
            rotationRate = rotationRate,
            gravityAcceleration = gravityAcceleration,
            userAcceleration = userAcceleration,
            magneticField = magneticField,
            heading = 45.degrees,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(Math.toRadians(15.0).toFloat(), native.attitude.pitch, 0.0001F)
        assertEquals(Math.toRadians(30.0).toFloat(), native.attitude.yaw, 0.0001F)
        assertEquals(Math.toRadians(60.0).toFloat(), native.attitude.roll, 0.0001F)

        assertEquals(Math.toRadians(5.0).toFloat(), native.rotationRate.x, 0.0001F)
        assertEquals(Math.toRadians(10.0).toFloat(), native.rotationRate.y, 0.0001F)
        assertEquals(Math.toRadians(15.0).toFloat(), native.rotationRate.z, 0.0001F)

        assertEquals(gravityAcceleration.mapToNative(), native.gravityAcceleration)
        assertEquals(userAcceleration.mapToNative(), native.userAcceleration)
        assertEquals(magneticField.mapToNative(), native.magneticField)

        assertEquals(45F, native.heading, 0.0001F)
        assertEquals(123L, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testMapToNativeRadians() {
        val attitude = AttitudeData(0.1.radians, 0.2.radians, 0.3.radians)
        val rotationRate = AngularVelocity3D.radiansPerSecond(
            1.0,
            1.1,
            1.2,
        )
        val gravityAcceleration = Point3D(2.0, 2.1, 2.2)
        val userAcceleration = Point3D(3.0, 3.1, 3.2)
        val magneticField = Point3D(4.0, 4.1, 4.2)

        val testData = MotionData(
            attitude = attitude,
            rotationRate = rotationRate,
            gravityAcceleration = gravityAcceleration,
            userAcceleration = userAcceleration,
            magneticField = magneticField,
            heading = Math.PI.radians,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(attitude.pitch.value.toFloat(), native.attitude.pitch, 0.0001F)
        assertEquals(attitude.yaw.value.toFloat(), native.attitude.yaw, 0.0001F)
        assertEquals(attitude.roll.value.toFloat(), native.attitude.roll, 0.0001F)

        assertEquals(1.0F, native.rotationRate.x, 0.0001F)
        assertEquals(1.1F, native.rotationRate.y, 0.0001F)
        assertEquals(1.2F, native.rotationRate.z, 0.0001F)

        assertEquals(Math.toDegrees(Math.PI).toFloat(), native.heading)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(MotionData::class.java)
            .verify()

        ToStringVerifier.forClass(MotionData::class.java)
            .verify()
    }
}
