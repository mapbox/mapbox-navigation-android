package com.mapbox.navigation.core.trip.service

import android.app.Notification
import com.mapbox.navigation.base.trip.TripNotification
import com.mapbox.navigation.base.trip.model.RouteProgress
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MapboxTripServiceTest {

    private lateinit var service: MapboxTripService
    private val tripNotification: TripNotification = mockk(relaxUnitFun = true)
    private val notification: Notification = mockk()
    private val initializeLambda: () -> Unit = mockk(relaxed = true)
    private val terminateLambda: () -> Unit = mockk(relaxed = true)

    @Before
    fun setUp() {
        service = MapboxTripService(tripNotification, initializeLambda, terminateLambda)
        every { tripNotification.getNotificationId() } answers { 1234 }
        every { tripNotification.getNotification() } answers { notification }
    }

    @Test
    fun serviceStartStopShouldNotCrash() {
        service.startService()
        service.stopService()
        service.startService()
        service.stopService()
    }

    @Test
    fun generationSanityTest() {
        assertNotNull(service)
    }

    @Test
    fun tripNotification_onTripSessionStartedCalledWhenStartServiceWithServiceNotStarted() {
        service.startService()

        verify(exactly = 1) { tripNotification.onTripSessionStarted() }
    }

    @Test
    fun tripNotification_onTripSessionStartedNotCalledWhenStartServiceWithServiceStarted() {
        service.startService()
        service.startService()

        verify(exactly = 1) { tripNotification.onTripSessionStarted() }
    }

    @Test
    fun initializeLambdaCalledWhenStartServiceWithServiceNotStarted() {
        service.startService()

        verify(exactly = 1) { initializeLambda() }
    }

    @Test
    fun initializeLambdaNotCalledWhenStartServiceWithServiceStarted() {
        service.startService()
        service.startService()

        verify(exactly = 1) { initializeLambda() }
    }

    @Test
    fun tripNotification_onTripSessionStoppedWhenStopServiceWithServiceStarted() {
        service.startService()
        service.stopService()

        verify(exactly = 1) { tripNotification.onTripSessionStopped() }
    }

    @Test
    fun tripNotification_onTripSessionNotStoppedWhenStartServiceWithServiceNotStarted() {
        service.startService()
        service.stopService()
        service.stopService()

        verify(exactly = 1) { tripNotification.onTripSessionStopped() }
    }

    @Test
    fun terminateLambdaCalledWhenStopServiceWithServiceStarted() {
        service.startService()
        service.stopService()

        verify(exactly = 1) { terminateLambda() }
    }

    @Test
    fun terminateLambdaNotCalledWhenStopServiceWithServiceNotStarted() {
        service.startService()
        service.stopService()
        service.stopService()

        verify(exactly = 1) { terminateLambda() }
    }

    @Test
    fun tripNotification_updateNotificationWhenUpdateNotificationCalled() {
        val routeProgress: RouteProgress = mockk()

        service.updateNotification(routeProgress)

        verify(exactly = 1) { tripNotification.updateNotification(routeProgress) }
    }
}
