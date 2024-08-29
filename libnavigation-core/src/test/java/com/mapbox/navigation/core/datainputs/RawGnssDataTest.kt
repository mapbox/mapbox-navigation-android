package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RawGnssDataTest {

    @Test
    fun testMapToNative() {
        val testData = RawGnssData(
            location = DataInputsTestDataFactory.testRawGnssLocation,
            satellites = listOf(DataInputsTestDataFactory.testRawGnssSatelliteData),
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(testData.location.mapToNative(), native.location)
        assertEquals(testData.satellites.map { it.mapToNative() }, native.satellites)
        assertEquals(testData.monotonicTimestampNanoseconds, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(RawGnssData::class.java)
            .verify()

        ToStringVerifier.forClass(RawGnssData::class.java)
            .verify()
    }
}
