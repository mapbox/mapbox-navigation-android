package com.mapbox.navigation.trip.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.options.NavigationOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale
import org.junit.Before

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification
    private val navigationOptionBuilder: NavigationOptions.Builder = mockk(relaxed = true)
    private val distanceFormatter: DistanceFormatter = mockk()
    private val navigationNotificationProvider: NavigationNotificationProvider = mockk()

    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkStatic(PendingIntent::class)
        val mockedContext = createContext()
        notification = MapboxTripNotification(
            mockedContext,
            distanceFormatter,
            navigationOptionBuilder,
            navigationNotificationProvider
        )
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
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
        every { PendingIntent.getActivity(mockedContext, any(), any(), any()) } returns (mockPendingIntentForActivity)
        every { PendingIntent.getBroadcast(mockedContext, any(), any(), any()) } returns (mockPendingIntentForBroadcast)
        return mockedContext
    }
}
