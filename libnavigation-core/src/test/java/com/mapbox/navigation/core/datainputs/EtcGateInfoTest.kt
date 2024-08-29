package com.mapbox.navigation.core.datainputs

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class EtcGateInfoTest {

    @Test
    fun testMapToNative() {
        val testData = EtcGateInfo(
            id = 123,
            monotonicTimestampNanoseconds = 456L,
        )

        val native = testData.mapToNative()

        assertEquals(123, native.id)
        assertEquals(456L, native.monotonicTimestampNanoseconds)
    }

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(EtcGateInfo::class.java)
            .verify()

        ToStringVerifier.forClass(EtcGateInfo::class.java)
            .verify()
    }
}
