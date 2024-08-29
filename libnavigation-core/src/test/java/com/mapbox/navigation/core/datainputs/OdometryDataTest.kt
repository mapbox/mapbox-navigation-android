package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class OdometryDataTest {

    @Test
    fun testMapToNativeDegress() {
        val testData = OdometryData(
            x = 1.0F,
            y = 2.0F,
            z = 3.0F,
            yawAngle = 30.degrees,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(1.0F, native.x)
        assertEquals(2.0F, native.y)
        assertEquals(3.0F, native.z)
        assertEquals(30F, native.yawAngle)
        assertEquals(123L, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testMapToNativeRadians() {
        val testData = OdometryData(
            x = 1.0F,
            y = 2.0F,
            z = 3.0F,
            yawAngle = Math.PI.radians,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(Math.toDegrees(Math.PI).toFloat(), native.yawAngle)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(OdometryData::class.java)
            .verify()

        ToStringVerifier.forClass(OdometryData::class.java)
            .verify()
    }
}
