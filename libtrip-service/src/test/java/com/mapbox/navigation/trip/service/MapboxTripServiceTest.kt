package com.mapbox.navigation.trip.service

import com.mapbox.navigation.base.trip.TripNotification
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class MapboxTripServiceTest {

    private lateinit var service: MapboxTripService
    private val notification: TripNotification = mockk()
    private val callback: () -> Unit = { }

    @Before
    fun setUp() {
        service = MapboxTripService(notification, callback)
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(service)
    }
}
