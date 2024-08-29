package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.geometry.Angle.Companion.degrees
import com.mapbox.navigation.base.geometry.Angle.Companion.radians
import com.mapbox.navigation.base.internal.utils.safeCompareTo
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RawGnssSatelliteDataTest {

    @Test
    fun testMapToNativeDegrees() {
        val testData = RawGnssSatelliteData(
            svid = 1,
            carrierFrequencyHz = 2.123F,
            basebandCn0DbHz = 3.456,
            cn0DbHz = 4.789,
            usedInFix = true,
            hasEphemerisData = true,
            hasAlmanacData = true,
            constellationType = ConstellationType.Gps,
            azimuth = 15F.degrees,
            elevation = 30F.degrees,
        )

        val native = testData.mapToNative()

        assertEquals(1, native.svid)
        assertEquals(2.123F, native.carrierFrequencyHz)
        assertTrue(3.456.safeCompareTo(native.basebandCn0DbHz))
        assertTrue(4.789.safeCompareTo(native.cn0DbHz))
        assertEquals(true, native.usedInFix)
        assertEquals(true, native.hasEphemerisData)
        assertEquals(true, native.hasAlmanacData)
        assertEquals(NativeConstellationType.GPS, native.constellationType)
        assertEquals(15F, native.azimuthDegrees)
        assertEquals(30F, native.elevationDegrees)
    }

    @Test
    fun testMapToNativeRadians() {
        val testData = RawGnssSatelliteData(
            svid = 1,
            carrierFrequencyHz = 2.123F,
            basebandCn0DbHz = 3.456,
            cn0DbHz = 4.789,
            usedInFix = true,
            hasEphemerisData = true,
            hasAlmanacData = true,
            constellationType = ConstellationType.Gps,
            azimuth = 5.789F.radians,
            elevation = 6.123F.radians,
        )

        val native = testData.mapToNative()

        assertEquals(Math.toDegrees(5.789).toFloat(), native.azimuthDegrees)
        assertEquals(Math.toDegrees(6.123).toFloat(), native.elevationDegrees)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(RawGnssSatelliteData::class.java)
            .verify()

        ToStringVerifier.forClass(RawGnssSatelliteData::class.java)
            .verify()
    }
}
