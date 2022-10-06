package com.mapbox.navigation.core.telemetry.events

import com.mapbox.bindgen.Value
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.fillValues
import com.mapbox.navigation.core.telemetry.events.EventsTestHelper.verifyNavigationEventFields
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationCancelEventTest {

    @Test
    fun testValue() {
        val cancelEvent = NavigationCancelEvent(EventsTestHelper.mockPhoneState()).apply {
            fillValues()
            arrivalTimestamp = "arrivalTimestamp_0"
            rating = 999
            comment = "comment_0"
        }

        val toValue = cancelEvent.toValue()

        toValue.verifyNavigationEventFields(cancelEvent.event)
        (toValue.contents as Map<String, Value>).let { content ->
            assertEquals(cancelEvent.arrivalTimestamp, content["arrivalTimestamp"]!!.contents)
            assertEquals(cancelEvent.rating.toLong(), content["rating"]!!.contents)
            assertEquals(cancelEvent.comment, content["comment"]!!.contents)
        }
    }
}
