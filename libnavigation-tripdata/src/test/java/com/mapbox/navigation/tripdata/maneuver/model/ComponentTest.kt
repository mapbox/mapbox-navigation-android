package com.mapbox.navigation.tripdata.maneuver.model

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class ComponentTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(Component::class.java)
            .verify()

        ToStringVerifier.forClass(Component::class.java)
            .verify()
    }
}
