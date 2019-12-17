package com.mapbox.navigation.trip.notification

import android.content.Context
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification
    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        notification = MapboxTripNotification(context)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(notification)
    }
}
