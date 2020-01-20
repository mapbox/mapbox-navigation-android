package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import com.mapbox.navigation.base.options.NavigationOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification
    private lateinit var mockedContext: Context
    private val navigationOptions: NavigationOptions = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkStatic(PendingIntent::class)
        mockedContext = createContext()
        notification = MapboxTripNotification(
            mockedContext,
            navigationOptions
        )
    }

    @Test
    fun generateSanityTest() {
        assertNotNull(notification)
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
        val mockedBroadcastReceiverIntent = mockk<Intent>()
        val mockPendingIntentForActivity = mockk<PendingIntent>(relaxed = true)
        val mockPendingIntentForBroadcast = mockk<PendingIntent>(relaxed = true)
        val mockedConfiguration = Configuration()
        mockedConfiguration.locale = Locale("en")
        val mockedResources = mockk<Resources>(relaxed = true)
        every { mockedResources.configuration } returns (mockedConfiguration)
        every { mockedContext.resources } returns (mockedResources)
        val mockedPackageManager = mockk<PackageManager>(relaxed = true)
        every { mockedContext.packageManager } returns (mockedPackageManager)
        every { mockedContext.packageName } returns ("com.mapbox.navigation.trip.notification")
        every { mockedContext.getString(any()) } returns ("%s 454545 ETA")
        val notificationManager = mockk<NotificationManager>(relaxed = true)
        every { mockedContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns (notificationManager)
        every { DateFormat.is24HourFormat(mockedContext) } returns (false)
        every {
            PendingIntent.getActivity(
                mockedContext,
                any(),
                any(),
                any()
            )
        } returns (mockPendingIntentForActivity)
        every {
            PendingIntent.getBroadcast(
                mockedContext,
                any(),
                any(),
                any()
            )
        } returns (mockPendingIntentForBroadcast)
        every {
            mockedContext.registerReceiver(
                any(),
                any()
            )
        } returns (mockedBroadcastReceiverIntent)
        every { mockedContext.unregisterReceiver(any()) } just Runs
        return mockedContext
    }

    @Test
    fun whenTripStartedThenRegisterReceiverCalledOnce() {
        notification.onTripSessionStarted()
        verify(exactly = 1) { mockedContext.registerReceiver(any(), any()) }
    }

    @Test
    fun whenTripStoppedThenCleanupIsDone() {
        val notificationManager =
            mockedContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notification.onTripSessionStopped()

        verify(exactly = 1) { mockedContext.unregisterReceiver(any()) }
        verify(exactly = 1) { notificationManager.cancel(any()) }
        assertEquals(
            true,
            MapboxTripNotification.notificationActionButtonChannel.isClosedForReceive
        )
        assertEquals(true, MapboxTripNotification.notificationActionButtonChannel.isClosedForSend)
    }

    @Test
    fun whenGetNotificationCalledThenNavigationNotificationProviderInteractedOnlyOnce() {
        mockkObject(NavigationNotificationProvider)
        val notificationMock = mockk<Notification>()
        every { NavigationNotificationProvider.buildNotification(any()) } returns notificationMock

        notification.getNotification()

        verify(exactly = 1) { NavigationNotificationProvider.buildNotification(any()) }

        notification.getNotification()
        notification.getNotification()

        verify(exactly = 1) { NavigationNotificationProvider.buildNotification(any()) }
    }
}
