package com.mapbox.navigation.core.reroute

import com.jparams.verifier.tostring.ToStringVerifier
import io.mockk.mockk
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class RerouteControllerTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            RerouteState.Failed::class.java,
            RerouteState.RouteFetched::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
        }

        ToStringVerifier.forClass(RerouteState.Failed::class.java)
            .withPrefabValue(URL::class.java, mockk())
            .verify()

        ToStringVerifier.forClass(RerouteState.RouteFetched::class.java).verify()
    }
}
