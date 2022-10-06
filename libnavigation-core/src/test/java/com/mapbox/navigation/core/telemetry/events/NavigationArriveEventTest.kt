package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import org.junit.Test

class NavigationArriveEventTest {

    @Test
    fun testValue() {
        val arriveEvent = NavigationArriveEvent(EventsTestHelper.mockPhoneState()).apply {
            fillValues()
        }

        val toValue = arriveEvent.toValue()

        toValue.verifyNavigationEventFields(arriveEvent.event)
    }
}
