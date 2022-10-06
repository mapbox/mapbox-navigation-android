package com.mapbox.navigation.core.telemetry.events

import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import org.junit.Test

class NavigationDepartEventTest {

    @Test
    fun testValue() {
        val departEvent = NavigationDepartEvent(EventsTestHelper.mockPhoneState()).apply {
            fillValues()
        }

        val toValue = departEvent.toValue()

        toValue.verifyNavigationEventFields(departEvent.event)
    }
}
