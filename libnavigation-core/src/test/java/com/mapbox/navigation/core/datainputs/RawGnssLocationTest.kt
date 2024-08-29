package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.physics.Speed.Companion.kph
import com.mapbox.navigation.base.physics.Speed.Companion.m_s
import com.mapbox.navigation.base.speed.model.SpeedUnit
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RawGnssLocationTest {

    @Test
    fun testMapToNativeDegrees() {
        val testData = RawGnssLocation(
            altitude = 1.123F,
            bearing = 2.345F.degrees,
            speed = 3.789.kph,
            latitude = 4.123F,
            longitude = -5.456F,
            bearingAccuracy = 6.789F.degrees,
            speedAccuracy = 7.123.kph,
            horizontalAccuracyMeters = 8.456F,
            verticalAccuracyMeters = 9.789F,
        )

        val native = testData.mapToNative()

        assertEquals(1.123F, native.altitude)
        assertEquals(2.345F, native.bearing)
        assertEquals(3.789.kph.toFloat(SpeedUnit.METERS_PER_SECOND), native.speed)
        assertEquals(4.123F, native.latitude)
        assertEquals(-5.456F, native.longitude)
        assertEquals(6.789F, native.bearingAccuracy)
        assertEquals(7.123.kph.toFloat(SpeedUnit.METERS_PER_SECOND), native.speedAccuracy)
        assertEquals(8.456F, native.horizontalAccuracy)
        assertEquals(9.789F, native.verticalAccuracy)
    }

    @Test
    fun testMapToNativeRadians() {
        val testData = RawGnssLocation(
            altitude = 1.123F,
            bearing = (Math.PI).radians,
            speed = 3.789.m_s,
            latitude = 4.123F,
            longitude = -5.456F,
            bearingAccuracy = (Math.PI / 2).radians,
            speedAccuracy = 7.123.m_s,
            horizontalAccuracyMeters = 8.456F,
            verticalAccuracyMeters = 9.789F,
        )

        val native = testData.mapToNative()

        assertEquals(3.789F, native.speed)
        assertEquals(Math.toDegrees(Math.PI).toFloat(), native.bearing)
        assertEquals(7.123F, native.speedAccuracy)
        assertEquals(Math.toDegrees(Math.PI / 2).toFloat(), native.bearingAccuracy)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(RawGnssLocation::class.java)
            .verify()

        ToStringVerifier.forClass(RawGnssLocation::class.java)
            .verify()
    }
}
