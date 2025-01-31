package com.mapbox.navigation.base.trip.model.eh

import com.jparams.verifier.tostring.ToStringVerifier
import com.mapbox.navigation.testing.withPrefabTestPoint
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class MatchableTypesTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            MatchableOpenLr::class.java,
            MatchableGeometry::class.java,
            MatchablePoint::class.java,
        ).forEach {
            EqualsVerifier.forClass(it)
                .withPrefabTestPoint()
                .verify()

            ToStringVerifier.forClass(it).verify()
        }
    }
}
