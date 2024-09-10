package com.mapbox.navigation.core.replay.history

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class ReplayEventsTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        listOf(
            ReplayEvents::class.java,
            ReplayEventGetStatus::class.java,
            ReplayEventUpdateLocation::class.java,
            ReplayEventLocation::class.java,
        ).forEach {
            EqualsVerifier.forClass(it).verify()
            ToStringVerifier.forClass(it).verify()
        }
    }
}
