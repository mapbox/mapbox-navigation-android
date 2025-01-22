package com.mapbox.navigation.core.trip.session.location

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class DRSensorFusionStatusTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(DRSensorFusionStatus::class.java)
            .verify()

        ToStringVerifier.forClass(DRSensorFusionStatus::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.DRSensorFusionStatus(
            com.mapbox.navigator.DRSensorFusionState.INITIALIZATION,
            30,
            50,
        )

        val platform = DRSensorFusionStatus.createFromNativeObject(native)

        assertNotNull(platform)
        assertEquals(DRSensorFusionState.createFromNativeObject(native.state), platform!!.state)
        assertEquals(native.progressInitDistance, platform.progressInitDistance)
        assertEquals(native.progressCorrectionBuckets, platform.progressCorrectionBuckets)
    }
}
