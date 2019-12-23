package com.mapbox.navigation.trip.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale
import org.junit.Before

class MapboxTripNotificationTest {

    private lateinit var notification: MapboxTripNotification
    @Before
    fun setUp() {
        mockkStatic(DateFormat::class)
        mockkStatic(PendingIntent::class)
        val context = createContext()
        notification = MapboxTripNotification(context)
    }

    private fun createContext(): Context {
        val mockedContext = mockk<Context>()
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
        return mockedContext
    }
}
