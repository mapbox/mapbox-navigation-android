package com.mapbox.navigation.trip.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.NOTIFICATION_ID
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
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
        mockRemoteViews()
        notification = MapboxTripNotification(
            mockedContext,
            navigationOptions
        )
    }

    private fun mockRemoteViews() {
        mockkConstructor(RemoteViews::class)
        every { anyConstructed<RemoteViews>().setInt(any(), any(), any()) } just Runs
        every { anyConstructed<RemoteViews>().setOnClickPendingIntent(any(), any()) } just Runs
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
        verify(exactly = 1) { notificationManager.cancel(NOTIFICATION_ID) }
        assertEquals(
            true,
            MapboxTripNotification.notificationActionButtonChannel.isClosedForReceive
        )
        assertEquals(true, MapboxTripNotification.notificationActionButtonChannel.isClosedForSend)
    }

    @Test
    fun whenGetNotificationCalledThenNavigationNotificationProviderInteractedOnlyOnce() {
        mockNotificationCreation()

        notification.getNotification()

        verify(exactly = 1) { NavigationNotificationProvider.buildNotification(any()) }

        notification.getNotification()
        notification.getNotification()

        verify(exactly = 1) { NavigationNotificationProvider.buildNotification(any()) }
    }

    @Test
    fun whenUpdateNotificationCalledThenBannerInstructionsPrimaryTextInteracted() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(routeProgress, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(routeProgress)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }
    }

    @Test
    fun whenUpdateNotificationCalledTwiceWithSameDataThenRemoteViewAreNotUpdatedTwice() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(routeProgress, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(routeProgress)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.updateNotification(routeProgress)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }
    }

    @Test
    fun whenUpdateNotificationCalledTwiceWithDifferentDataThenRemoteViewUpdatedTwice() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        var primaryText = "Primary Text"
        val primaryTextLambda = { primaryText }
        val bannerText = mockBannerText(routeProgress, primaryTextLambda)
        mockUpdateNotificationAndroidInteractions()

        notification.updateNotification(routeProgress)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) {
            anyConstructed<RemoteViews>().setTextViewText(any(), primaryTextLambda())
        }

        primaryText = "Changed Primary Text"
        notification.updateNotification(routeProgress)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) {
            anyConstructed<RemoteViews>().setTextViewText(any(), primaryTextLambda())
        }
    }

    @Test
    fun whenGoThroughStartUpdateStopCycleThenNotificationCacheDropped() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(routeProgress, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.onTripSessionStarted()
        notification.updateNotification(routeProgress)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.updateNotification(routeProgress)

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.onTripSessionStopped()
        notification.onTripSessionStarted()

        verify(exactly = 2) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.updateNotification(routeProgress)

        verify(exactly = 3) { bannerText.text() }
        verify(exactly = 4) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }
    }

    @Test
    fun whenGoThroughStartUpdateStopCycleThenStartStopSessionDontAffectRemoveViews() {
        val routeProgress = mockk<RouteProgress>(relaxed = true)
        val primaryText = { "Primary Text" }
        val bannerText = mockBannerText(routeProgress, primaryText)
        mockUpdateNotificationAndroidInteractions()

        notification.onTripSessionStarted()
        notification.updateNotification(routeProgress)

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.onTripSessionStopped()
        notification.onTripSessionStarted()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.onTripSessionStopped()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }

        notification.onTripSessionStarted()

        verify(exactly = 1) { bannerText.text() }
        verify(exactly = 2) { anyConstructed<RemoteViews>().setTextViewText(any(), primaryText()) }
    }

    private fun mockUpdateNotificationAndroidInteractions() {
        every { anyConstructed<RemoteViews>().setImageViewResource(any(), any()) } just Runs
        every { anyConstructed<RemoteViews>().setTextViewText(any(), any()) } just Runs
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false
        mockNotificationCreation()
    }

    private fun mockNotificationCreation() {
        mockkObject(NavigationNotificationProvider)
        val notificationMock = mockk<Notification>()
        every { NavigationNotificationProvider.buildNotification(any()) } returns notificationMock
    }

    private fun mockBannerText(
        routeProgress: RouteProgress,
        primaryText: () -> String
    ): BannerText {
        val bannerText = mockk<BannerText>()
        val bannerInstructions = mockk<BannerInstructions>()
        every { routeProgress.bannerInstructions() } returns bannerInstructions
        every { bannerInstructions.primary() } returns bannerText
        every { bannerText.text() } answers { primaryText() }
        return bannerText
    }
}
