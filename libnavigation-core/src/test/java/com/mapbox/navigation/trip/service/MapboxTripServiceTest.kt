package com.mapbox.navigation.trip.service

import android.app.Notification
import com.mapbox.navigation.base.trip.TripNotification
import io.mockk.every
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
    private val tripNotification: TripNotification = mockk()
    private val notification: Notification = mockk()
    private val callback: () -> Unit = { }

    @Before
    fun setUp() {
        service = MapboxTripService(tripNotification, callback)
        every { tripNotification.getNotificationId() } answers { 1234 }
        every { tripNotification.getNotification() } answers { notification }
        every { tripNotification.onTripSessionStopped() } answers { Unit }

    }

    @Test
    fun testServiceStartStop() {
        service.startService()
        service.stopService()
        service.startService()
        service.stopService()
    }

    @Test
    fun generationSanityTest() {
        Assert.assertNotNull(service)
    }
}
