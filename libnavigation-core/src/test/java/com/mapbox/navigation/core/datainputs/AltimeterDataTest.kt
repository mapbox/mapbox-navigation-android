package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AltimeterDataTest {

    @Test
    fun testMapToNative() {
        val testData = AltimeterData(
            pressure = 1.2F,
            monotonicTimestampNanoseconds = 123L,
        )

        val native = testData.mapToNative()

        assertEquals(1.2F, native.pressure)
        assertEquals(123L, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AltimeterData::class.java)
            .verify()

        ToStringVerifier.forClass(AltimeterData::class.java)
            .verify()
    }
}
