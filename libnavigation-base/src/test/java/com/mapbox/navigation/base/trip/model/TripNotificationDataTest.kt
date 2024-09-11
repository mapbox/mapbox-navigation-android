package com.mapbox.navigation.base.trip.model

import com.jparams.verifier.tostring.ToStringVerifier
import nl.jqno.equalsverifier.EqualsVerifier
import org.junit.Test

class TripNotificationDataTest {

    @Test
    fun testGeneratedEqualsHashcodeToStringFunctions() {
        EqualsVerifier.forClass(TripNotificationState.TripNotificationData::class.java).verify()
        ToStringVerifier.forClass(TripNotificationState.TripNotificationData::class.java).verify()
    }
}
