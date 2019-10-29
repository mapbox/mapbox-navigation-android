package com.mapbox.navigation.trip.notification

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification

    @Before
    fun setUp() {
        notification = MapboxTripNotification()
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(notification)
    }
}
