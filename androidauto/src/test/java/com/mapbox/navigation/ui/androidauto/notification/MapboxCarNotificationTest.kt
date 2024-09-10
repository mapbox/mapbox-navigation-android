package com.mapbox.navigation.ui.androidauto.notification

import android.graphics.Color
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Session
import androidx.car.app.model.CarColor
import androidx.car.app.notification.CarAppExtender
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotificationInterceptor
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.R
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class MapboxCarNotificationTest {

    private val options: MapboxCarOptions = mockk {
        every { notificationOptions } returns mockk {
            every { startAppService } returns null
        }
    }
    private val carContext = mockk<CarContext> {
        every { getColor(R.color.mapbox_notification_blue) } returns Color.BLUE
    }
    private val idleExtenderUpdater = mockk<IdleExtenderUpdater>(relaxUnitFun = true)
    private val freeDriveExtenderUpdater = mockk<FreeDriveExtenderUpdater>(relaxUnitFun = true)
    private val activeGuidanceExtenderUpdater =
        mockk<ActiveGuidanceExtenderUpdater>(relaxUnitFun = true)
    private val mapboxCarNotification = MapboxCarNotification(
        options,
        carContext,
        idleExtenderUpdater,
        freeDriveExtenderUpdater,
        activeGuidanceExtenderUpdater,
    )
    private val formatterOptions = mockk<DistanceFormatterOptions>()
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true) {
        every { navigationOptions } returns mockk {
            every { distanceFormatterOptions } returns formatterOptions
            every { timeFormatType } returns TimeFormat.TWENTY_FOUR_HOURS
        }
    }
    private val navigationSessionStateObserverSlot = slot<NavigationSessionStateObserver>()
    private val routeProgressObserverSlot = slot<RouteProgressObserver>()
    private val tripNotificationInterceptorSlot = slot<TripNotificationInterceptor>()
    private val carAppExtenderSlot = slot<CarAppExtender>()
    private val notificationBuilder = mockk<NotificationCompat.Builder> {
        every { setOngoing(any()) } returns this
        every { setCategory(any()) } returns this
        every { extend(capture(carAppExtenderSlot)) } returns this
    }
    private val routeProgress = mockk<RouteProgress>()
    private val startAppServiceSlot = slot<Class<out CarAppService>>()

    @Before
    fun setUp() {
        every {
            mapboxNavigation.registerNavigationSessionStateObserver(
                capture(navigationSessionStateObserverSlot),
            )
        } just Runs
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        } just Runs
        every {
            mapboxNavigation.setTripNotificationInterceptor(
                capture(tripNotificationInterceptorSlot),
            )
        } just Runs
        mockkObject(CarPendingIntentFactory)
        every {
            CarPendingIntentFactory.create(any(), capture(startAppServiceSlot))
        } returns mockk()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `interceptor and observers are registered in onAttached`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        verify { mapboxNavigation.registerNavigationSessionStateObserver(any()) }
        verify { mapboxNavigation.registerRouteProgressObserver(any()) }
        verify { mapboxNavigation.setTripNotificationInterceptor(any()) }
    }

    @Test
    fun `interceptor and observers are unregistered in onDetached`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        mapboxCarNotification.onDetached(mapboxNavigation)
        verify { mapboxNavigation.setTripNotificationInterceptor(null) }
        verify {
            mapboxNavigation.registerNavigationSessionStateObserver(
                navigationSessionStateObserverSlot.captured,
            )
        }
        verify {
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserverSlot.captured)
        }
    }

    @Test
    fun `notification is idle by default`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)
        tripNotificationInterceptorSlot.captured.intercept(notificationBuilder)
        verifyCommonProperties()
        verifyIdleProperties()
    }

    @Test
    fun `notification is updated in free drive state`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        setNavigationSessionState(mockk<NavigationSessionState.FreeDrive>())
        tripNotificationInterceptorSlot.captured.intercept(notificationBuilder)
        verifyCommonProperties()
        verifyFreeDriveProperties()
    }

    @Test
    fun `notification is idle in active guidance state without route progress`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        setNavigationSessionState(mockk<NavigationSessionState.ActiveGuidance>())
        tripNotificationInterceptorSlot.captured.intercept(notificationBuilder)
        verifyCommonProperties()
        verifyIdleProperties()
    }

    @Test
    fun `notification is updated in active guidance state with route progress`() {
        mapboxCarNotification.onAttached(mapboxNavigation)
        setNavigationSessionState(mockk<NavigationSessionState.ActiveGuidance>())
        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)
        tripNotificationInterceptorSlot.captured.intercept(notificationBuilder)
        verifyCommonProperties()
        verifyActiveGuidanceProperties()
    }

    @Test
    fun `notification startAppServiceSlot is passed to an intent`() {
        every { options.notificationOptions.startAppService } returns TestCarAppService::class.java
        mapboxCarNotification.onAttached(mapboxNavigation)
        setNavigationSessionState(mockk<NavigationSessionState.ActiveGuidance>())
        routeProgressObserverSlot.captured.onRouteProgressChanged(routeProgress)
        tripNotificationInterceptorSlot.captured.intercept(notificationBuilder)

        assertTrue(startAppServiceSlot.isCaptured)
        assertEquals(TestCarAppService::class.java, startAppServiceSlot.captured)
    }

    private fun verifyCommonProperties() {
        verifyOrder {
            notificationBuilder.setOngoing(true)
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            notificationBuilder.extend(any())
        }
        val carAppExtender = carAppExtenderSlot.captured
        assertEquals(CarColor.createCustom(Color.BLUE, Color.BLUE), carAppExtender.color)
        assertEquals(R.drawable.mapbox_ic_navigation, carAppExtender.smallIcon)
        assertFalse(startAppServiceSlot.isCaptured)
    }

    private fun verifyIdleProperties() {
        verify {
            idleExtenderUpdater.update(match { checkExtender(it) })
            activeGuidanceExtenderUpdater.updateCurrentManeuverToDefault()
        }
        verify(exactly = 0) {
            freeDriveExtenderUpdater.update(any())
            activeGuidanceExtenderUpdater.update(any(), any(), any(), any())
        }
    }

    private fun verifyFreeDriveProperties() {
        verify {
            freeDriveExtenderUpdater.update(match { checkExtender(it) })
            activeGuidanceExtenderUpdater.updateCurrentManeuverToDefault()
        }
        verify(exactly = 0) {
            idleExtenderUpdater.update(any())
            activeGuidanceExtenderUpdater.update(any(), any(), any(), any())
        }
    }

    private fun verifyActiveGuidanceProperties() {
        verify {
            activeGuidanceExtenderUpdater.update(
                match { checkExtender(it) },
                routeProgress,
                match { it is MapboxDistanceFormatter && it.options == formatterOptions },
                TimeFormat.TWENTY_FOUR_HOURS,
            )
        }
        verify(exactly = 0) {
            idleExtenderUpdater.update(any())
            freeDriveExtenderUpdater.update(any())
            activeGuidanceExtenderUpdater.updateCurrentManeuverToDefault()
        }
    }

    private fun setNavigationSessionState(sessionState: NavigationSessionState) {
        navigationSessionStateObserverSlot.captured.onNavigationSessionStateChanged(sessionState)
    }

    private fun checkExtender(actualExtenderBuilder: CarAppExtender.Builder): Boolean {
        val expectedExtender = carAppExtenderSlot.captured
        val actualExtender = actualExtenderBuilder.build()
        return expectedExtender.contentTitle == actualExtender.contentTitle &&
            expectedExtender.contentText == actualExtender.contentText &&
            expectedExtender.smallIcon == actualExtender.smallIcon &&
            expectedExtender.largeIcon == actualExtender.largeIcon &&
            expectedExtender.contentIntent == actualExtender.contentIntent &&
            expectedExtender.deleteIntent == actualExtender.deleteIntent &&
            expectedExtender.actions == actualExtender.actions &&
            expectedExtender.importance == actualExtender.importance &&
            expectedExtender.color == actualExtender.color &&
            expectedExtender.channelId == actualExtender.channelId
    }

    class TestCarAppService : CarAppService() {
        override fun createHostValidator(): HostValidator {
            return mockk()
        }

        override fun onCreateSession(): Session {
            return mockk()
        }
    }
}
