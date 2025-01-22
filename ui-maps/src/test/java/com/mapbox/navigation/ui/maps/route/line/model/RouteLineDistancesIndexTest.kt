package com.mapbox.navigation.ui.maps.route.line.model

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.testing.withPrefabTestPoint
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class RouteLineDistancesIndexTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(RouteLineDistancesIndex::class.java)
            .withPrefabTestPoint()
            .verify()

        ToStringVerifier.forClass(RouteLineDistancesIndex::class.java).verify()
    }
}
