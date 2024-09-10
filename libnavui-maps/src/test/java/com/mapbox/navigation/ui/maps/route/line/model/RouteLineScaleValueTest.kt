package com.mapbox.navigation.ui.maps.route.line.model

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class RouteLineScaleValueTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(RouteLineScaleValue::class.java).verify()
        ToStringVerifier.forClass(RouteLineScaleValue::class.java).verify()
    }
}
