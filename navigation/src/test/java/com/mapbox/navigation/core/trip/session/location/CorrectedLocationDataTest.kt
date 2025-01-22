package com.mapbox.navigation.core.trip.session.location

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.testing.factories.createFixedLocation
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalMapboxNavigationAPI::class)
class CorrectedLocationDataTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(CorrectedLocationData::class.java)
            .verify()

        ToStringVerifier.forClass(CorrectedLocationData::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.CorrectedLocationData(
            createFixedLocation(1.0, 2.0),
            true,
            com.mapbox.navigator.IMUDerivedData(45.0),
            true,
            123,
            com.mapbox.navigator.DRSensorFusionStatus(
                com.mapbox.navigator.DRSensorFusionState.INITIALIZATION,
                30,
                50,
            ),
        )

        val platform = CorrectedLocationData.createFromNativeObject(native)

        assertNotNull(platform)
        assertEquals(native.location.toLocation(), platform!!.location)
        assertEquals(native.isStill, platform.isStill)
        assertEquals(
            DRSensorFusionStatus.createFromNativeObject(native.drSensorFusionStatus),
            platform.drSensorFusionStatus,
        )
    }
}
