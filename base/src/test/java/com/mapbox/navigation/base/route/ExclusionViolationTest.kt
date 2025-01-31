package com.mapbox.navigation.base.route

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class ExclusionViolationTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(ExclusionViolation::class.java).verify()
        ToStringVerifier.forClass(ExclusionViolation::class.java).verify()
    }
}
