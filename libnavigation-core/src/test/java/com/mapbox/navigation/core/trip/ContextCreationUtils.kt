package com.mapbox.navigation.core.trip

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.location.LocationManager
import android.media.AudioManager
import com.mapbox.android.accounts.v1.AccountsConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.util.Locale

const val MAPBOX_TEST_VENDOR_ID = "mapboxVendorId"
const val MAPBOX_TEST_TELEMETRY_STATE = "mapboxTelemetryState"
const val MAPBOX_TEST_TELEMETRY_ENABLED = "ENABLED"
fun createContext(packageName: String): Context {
    val localLocationEngine: LocationEngine = mockk(relaxed = true)
    val mockedContext = mockk<Context>()
    val mockedBroadcastReceiverIntent = mockk<Intent>()
    val mockedConfiguration = Configuration()

    mockedConfiguration.locale = Locale("en")
    val mockedResources = mockk<Resources>(relaxed = true)
    every { mockedResources.configuration } returns (mockedConfiguration)
    every { mockedContext.resources } returns (mockedResources)
    every { mockedContext.packageName } returns (packageName)
    every { mockedContext.getString(any()) } returns "FORMAT_STRING"
    val notificationManager = mockk<NotificationManager>(relaxed = true)
    every { mockedContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns (notificationManager)
    every { mockedContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager } returns mockk(relaxed = true)
    every { LocationEngineProvider.getBestLocationEngine(mockedContext) } returns mockk(relaxed = true)
    every {
        mockedContext.registerReceiver(
                any(),
                any()
        )
    } returns (mockedBroadcastReceiverIntent)
    every { mockedContext.unregisterReceiver(any()) } just Runs

    every { mockedContext.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE) } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE) } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE).getString(MAPBOX_TEST_TELEMETRY_STATE, any()) } returns MAPBOX_TEST_TELEMETRY_ENABLED
    every { mockedContext.applicationContext.getSharedPreferences(MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE).getString(MAPBOX_TEST_VENDOR_ID, any()) } returns MAPBOX_TEST_VENDOR_ID

    every { mockedContext.applicationContext.registerReceiver(any(), any()) } returns mockk(relaxed = true)

    every { mockedContext.applicationContext.getString(any()) } returns "some string"
    every { mockedContext.applicationContext.getResources() } returns mockk(relaxed = true)
    every { localLocationEngine.requestLocationUpdates(mockk(), any(), null) } returns mockk(relaxed = true)
    every { mockedContext.getMainLooper() } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.getMainLooper() } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.contentResolver } returns mockk(relaxed = true)

    every { mockedContext.applicationContext.getPackageManager() } returns mockk(relaxed = true)
    every { mockedContext.getPackageManager() } returns mockk(relaxed = true)

    every { mockedContext.applicationContext.getPackageName() } returns packageName
    every { mockedContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.applicationContext } returns mockk(relaxed = true)
    every { mockedContext.getContentResolver() } returns mockk(relaxed = true)

    every { mockedContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager } returns mockk(relaxed = true)

    every { mockedContext.applicationContext } returns mockk(relaxed = true)
    every { mockedContext.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockk<NotificationManager>()
    return mockedContext
}
