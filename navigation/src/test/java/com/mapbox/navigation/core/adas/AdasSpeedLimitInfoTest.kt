package com.mapbox.navigation.core.adas

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.speed.model.SpeedUnit
import com.mapbox.navigator.SpeedLimitType
import com.mapbox.navigator.SpeedLimitUnit
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class AdasSpeedLimitInfoTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(AdasSpeedLimitInfo::class.java)
            .verify()

        ToStringVerifier.forClass(AdasSpeedLimitInfo::class.java)
            .verify()
    }

    @Test
    fun testCreateFromNativeObject() {
        val native = com.mapbox.navigator.SpeedLimitInfo(
            30,
            SpeedLimitUnit.KILOMETRES_PER_HOUR,
            SpeedLimitType.EXPLICIT,
            AdasTypeFactory.NATIVE_SPEED_LIMIT_RESTRICTION,
        )

        val platform = AdasSpeedLimitInfo.createFromNativeObject(native)
        assertEquals(native.value, platform.value)
        assertEquals(SpeedUnit.KILOMETERS_PER_HOUR, platform.speedUnit)
        assertEquals(AdasSpeedLimitInfo.SpeedLimitType.EXPLICIT, platform.speedLimitType)
        assertEquals(AdasTypeFactory.SPEED_LIMIT_RESTRICTION, platform.restriction)
    }
}
