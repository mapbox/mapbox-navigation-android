package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.geometry.Point3D
import com.mapbox.navigation.base.internal.mapToNative
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CompassDataTest {

    @Test
    fun testMapToNativeDegrees() {
        val rawGeomagneticData = Point3D(1.0, 2.0, 3.0)

        val testData = CompassData(
            magneticHeading = 15.degrees,
            trueHeading = 30.degrees,
            headingAccuracy = 60.degrees,
            rawGeomagneticData = rawGeomagneticData,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(15F, native.magneticHeading)
        assertEquals(30F, native.trueHeading)
        assertEquals(60F, native.headingAccuracy)
        assertEquals(rawGeomagneticData.mapToNative(), native.rawGeomagneticData)
        assertEquals(123L, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testMapToNativeRadians() {
        val rawGeomagneticData = Point3D(1.0, 2.0, 3.0)
        val testData = CompassData(
            magneticHeading = 0.5.radians,
            trueHeading = 0.7.radians,
            headingAccuracy = 1.2.radians,
            rawGeomagneticData = rawGeomagneticData,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(Math.toDegrees(0.5).toFloat(), native.magneticHeading)
        assertEquals(Math.toDegrees(0.7).toFloat(), native.trueHeading)
        assertEquals(Math.toDegrees(1.2).toFloat(), native.headingAccuracy)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(CompassData::class.java)
            .verify()

        ToStringVerifier.forClass(CompassData::class.java)
            .verify()
    }
}
