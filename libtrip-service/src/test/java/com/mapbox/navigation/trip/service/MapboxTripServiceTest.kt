package com.mapbox.navigation.trip.service

import com.mapbox.navigation.base.trip.TripNotification
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MapboxTripServiceTest {

    private lateinit var service: MapboxTripService
    private val notification: TripNotification = mockk()

    @Before
    fun setUp() {
        service = MapboxTripService(notification)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(service)
    }
}
