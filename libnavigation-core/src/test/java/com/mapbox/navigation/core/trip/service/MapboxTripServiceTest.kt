package com.mapbox.navigation.core.trip.service

import android.app.Notification
import android.os.SystemClock
import com.mapbox.navigation.base.internal.factory.TripNotificationStateFactory.buildTripNotificationState
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.LoggerFrontend
import com.mapbox.navigation.utils.internal.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MapboxTripServiceTest {

    private lateinit var service: MapboxTripService
    private val tripNotification: TripNotification = mockk(relaxUnitFun = true)
    private val notification: Notification = mockk()
    private val initializeLambda: () -> Boolean = mockk(relaxed = true)
    private val terminateLambda: () -> Unit = mockk(relaxed = true)
    private val logger = mockk<LoggerFrontend>(relaxed = true)

    @get:Rule
    val loggerRule = LoggingFrontendTestRule(logger)

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        service = MapboxTripService(
            tripNotification,
            initializeLambda,
            terminateLambda,
            ThreadController(),
        )
        every { tripNotification.getNotificationId() } answers { NOTIFICATION_ID }
        every { tripNotification.getNotification() } answers { notification }
        every { initializeLambda() } returns true
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
    fun tripNotification_onTripSessionStartedCalledWhenStartServiceWithServiceNotStartedStartSucceeded() {
        every { initializeLambda() } returns true
        service.startService()

        verify(exactly = 1) { tripNotification.onTripSessionStarted() }
    }

    @Test
    fun tripNotification_onTripSessionStartedNotCalledWhenStartServiceWithServiceNotStartedStartFailed() {
        every { initializeLambda() } returns false
        service.startService()

        verify(exactly = 0) { tripNotification.onTripSessionStarted() }
    }

    @Test
    fun tripNotification_onTripSessionStartedNotCalledWhenStartServiceWithServiceStarted() {
        service.startService()
        service.startService()

        verify(exactly = 1) { tripNotification.onTripSessionStarted() }
        verify(exactly = 1) {
            logger.logI("service already started", "MapboxTripService")
        }
    }

    @Test
    fun initializeLambdaCalledWhenStartServiceWithServiceNotStarted() {
        service.startService()

        verify(exactly = 1) { initializeLambda() }
    }

    @Test
    fun initializeLambdaCalledWhenStartServiceWithServiceFailedToStart() {
        every { initializeLambda() } returns false
        service.startService()
        service.startService()

        verify(exactly = 2) { initializeLambda() }
    }

    @Test
    fun tripNotification_onTripSessionStoppedWhenStopServiceWithServiceStarted() {
        service.startService()
        service.stopService()

        verify(exactly = 1) { tripNotification.onTripSessionStopped() }
    }

    @Test
    fun tripNotification_onTripSessionNotStoppedWhenStopServiceWithServiceFailedToStart() {
        every { initializeLambda() } returns false
        service.startService()
        service.stopService()

        verify(exactly = 0) { tripNotification.onTripSessionStopped() }
    }

    @Test
    fun tripNotification_onTripSessionStoppedWhenStopServiceWithServiceFailedToStart() {
        every { initializeLambda() } returns false
        service.startService()
        service.stopService()

        verify(exactly = 0) { tripNotification.onTripSessionStopped() }
    }

    @Test
    fun tripNotification_onTripSessionNotStoppedWhenStartServiceWithServiceNotStarted() {
        service.startService()
        service.stopService()
        service.stopService()

        verify(exactly = 1) { tripNotification.onTripSessionStopped() }
        verify(exactly = 1) {
            logger.logI("Service is not started yet", "MapboxTripService")
        }
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
    fun terminateLambdaCalledWhenStopServiceWithServiceNotStarted() {
        service.startService()
        service.stopService()
        service.stopService()

        verify(exactly = 1) { terminateLambda() }
    }

    @Test
    fun tripNotification_updateNotificationWhenUpdateNotificationCalled() =
        coroutineRule.runBlockingTest {
            val routeProgress: RouteProgress = mockk {
                every { bannerInstructions } returns null
                every { currentLegProgress } returns null
            }
            service.startService()

            service.updateNotification(buildTripNotificationState(routeProgress))

            verify(exactly = 1) { tripNotification.updateNotification(any()) }
        }

    @Test
    fun tripNotification_updateNotificationWhenUpdateNotificationCalledWhenRouteProgressNull() =
        coroutineRule.runBlockingTest {
            service.startService()
            service.updateNotification(buildTripNotificationState(null))

            verify(exactly = 1) {
                tripNotification.updateNotification(
                    any<TripNotificationState.TripNotificationFreeState>()
                )
            }
        }

    @Test
    fun notificationDataObserverInvokedIfRegisteredBeforeServiceStart() =
        coroutineRule.runBlockingTest {
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
    fun notificationDataObserverNotInvokedIfRegisteredBeforeServiceStartStartFailed() {
        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)
        every { initializeLambda() } returns false
        service.startService()

        verify(exactly = 0) {
            notificationDataObserver.onNotificationUpdated(any())
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
    fun notificationDataObserverNotInvokedWithTheLatestDataIfRegisteredAfterServiceFailedToStart() {
        every { initializeLambda() } returns false
        service.startService()

        val newNotification = mockk<Notification>()
        every { tripNotification.getNotification() } answers { newNotification }

        val notificationDataObserver = mockk<NotificationDataObserver>(relaxUnitFun = true)
        MapboxTripService.registerOneTimeNotificationDataObserver(notificationDataObserver)

        verify(exactly = 0) {
            notificationDataObserver.onNotificationUpdated(any())
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

    @Test
    fun notificationUpdateAfterServiceStartIsDelayed() = coroutineRule.runBlockingTest {
        val notificationState = buildTripNotificationState(null)
        service.startService()
        service.updateNotification(notificationState)
        coroutineRule.testDispatcher.advanceTimeBy(250)
        verify(exactly = 0) { tripNotification.updateNotification(any()) }
        coroutineRule.testDispatcher.advanceTimeBy(500)
        verify(exactly = 1) { tripNotification.updateNotification(notificationState) }
    }

    @Test
    fun notificationUpdateAfterServiceStartIsNotDelayedIfTimeHasAlreadyPassed() {
        mockkStatic(SystemClock::class) {
            val notificationState = buildTripNotificationState(null)
            every { SystemClock.elapsedRealtime() } returns 0
            service.startService()
            every { SystemClock.elapsedRealtime() } returns 500
            service.updateNotification(notificationState)
            verify(exactly = 1) { tripNotification.updateNotification(notificationState) }
        }
    }

    @Test
    fun notificationUpdateAfterServiceFailedToStartStartedIsDelayed() = coroutineRule.runBlockingTest {
        val notificationState = buildTripNotificationState(null)
        every { initializeLambda() } returns false
        service.startService()
        service.updateNotification(notificationState)
        coroutineRule.testDispatcher.advanceTimeBy(750)
        verify(exactly = 0) { tripNotification.updateNotification(any()) }
    }

    companion object {
        private const val NOTIFICATION_ID = 1234
    }
}
