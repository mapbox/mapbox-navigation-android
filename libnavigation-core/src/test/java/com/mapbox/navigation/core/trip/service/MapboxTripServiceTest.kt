package com.mapbox.navigation.core.trip.service

import android.app.Notification
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.TripNotification
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxTripServiceTest {

    private lateinit var service: MapboxTripService
    private val tripNotification: TripNotification = mockk(relaxUnitFun = true)
    private val notification: Notification = mockk()
    private val initializeLambda: () -> Unit = mockk(relaxed = true)
    private val terminateLambda: () -> Unit = mockk(relaxed = true)
    private val logger: Logger = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        service = MapboxTripService(tripNotification, initializeLambda, terminateLambda, logger)
        every { tripNotification.getNotificationId() } answers { NOTIFICATION_ID }
        every { tripNotification.getNotification() } answers { notification }
    }

    @After
    fun cleanUp() {
        service.stopService()
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
        verify(exactly = 1) { logger.i(msg = Message("service already started")) }
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
        verify(exactly = 1) { logger.i(msg = Message("Service is not started yet")) }
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
        val routeProgress: RouteProgress = mockk {
            every { bannerInstructions } returns null
            every { currentLegProgress } returns null
        }

        service.updateNotification(buildTripNotificationState(routeProgress))

        verify(exactly = 1) { tripNotification.updateNotification(any<TripNotificationState>()) }
    }

    @Test
    fun tripNotification_updateNotificationWhenUpdateNotificationCalledWhenRouteProgressNull() {
        service.updateNotification(buildTripNotificationState(null))

        verify(exactly = 1) {
            tripNotification.updateNotification(
                any<TripNotificationState.TripNotificationFreeState>()
            )
        }
    }

    @Test
    fun notificationDataObserverInvokedIfRegisteredBeforeServiceStart() {
        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)
        service.startService()

        verify(exactly = 1) {
            notificationDataObserver.onNotificationUpdated(
                MapboxNotificationData(NOTIFICATION_ID, notification),
            )
        }
    }

    @Test
    fun notificationDataObserverInvokedWithTheLatestDataIfRegisteredAfterServiceStart() {
        service.startService()

        val newNotification = mockk<Notification>()
        every { tripNotification.getNotification() } answers { newNotification }

        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)

        verify(exactly = 1) {
            notificationDataObserver.onNotificationUpdated(
                MapboxNotificationData(NOTIFICATION_ID, newNotification),
            )
        }
    }

    @Test
    fun notificationDataObserverNotInvokedIfRegisteredAfterServiceStop() {
        service.startService()
        service.stopService()

        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)

        verify(exactly = 0) { notificationDataObserver.onNotificationUpdated(any()) }
    }

    @Test
    fun notificationDataObserverNotInvokedIfUnregisteredBeforeServiceStart() {
        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)
        MapboxTripService.unregisterOneTimeNotificationDataObserver(notificationDataObserver)
        service.startService()

        verify(exactly = 0) { notificationDataObserver.onNotificationUpdated(any()) }
    }

    companion object {
        private const val NOTIFICATION_ID = 1234
    }
}
