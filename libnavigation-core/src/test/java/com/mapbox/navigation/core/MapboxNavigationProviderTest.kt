package com.mapbox.navigation.core

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Context.MODE_PRIVATE
import android.content.Context.NOTIFICATION_SERVICE
import android.net.ConnectivityManager
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import com.mapbox.android.telemetry.MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.common.MapboxSDKCommon
import com.mapbox.common.module.provider.MapboxModuleProvider
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.routealternatives.RouteAlternativesControllerProvider
import com.mapbox.navigation.core.routerefresh.RouteRefreshControllerProvider
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.Locale

class MapboxNavigationProviderTest {

    private lateinit var applicationContext: Context

    @Before
    fun setup() {
        mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        mockkObject(MapboxSDKCommon)
        every {
            MapboxSDKCommon.getContext().getSystemService(Context.CONNECTIVITY_SERVICE)
        } returns mockk<ConnectivityManager>()
        mockkObject(MapboxModuleProvider)

        val hybridRouter: Router = mockk(relaxUnitFun = true)
        every {
            MapboxModuleProvider.createModule<Router>(
                MapboxModuleType.NavigationRouter,
                any()
            )
        } returns hybridRouter
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxed = true)
        every {
            MapboxModuleProvider.createModule<TripNotification>(
                MapboxModuleType.NavigationTripNotification,
                any()
            )
        } returns mockk()

        mockkObject(NavigationComponentProvider)
        mockkObject(RouteRefreshControllerProvider)
        every {
            RouteRefreshControllerProvider.createRouteRefreshController(
                any(), any(), any(), any()
            )
        } returns mockk(relaxed = true)
        mockkObject(RouteAlternativesControllerProvider)
        every {
            RouteAlternativesControllerProvider.create(any(), any(), any(), any(), any())
        } returns mockk(relaxed = true)

        applicationContext = mockk(relaxed = true) {
            every { inferDeviceLocale() } returns java.util.Locale.US
            every {
                getSystemService(NOTIFICATION_SERVICE)
            } returns mockk<NotificationManager>()
            every { getSystemService(ALARM_SERVICE) } returns mockk<AlarmManager>()
            every {
                getSharedPreferences(
                    MAPBOX_SHARED_PREFERENCES,
                    MODE_PRIVATE
                )
            } returns mockk(relaxed = true) {
                every { getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"
            }
            every { packageManager } returns mockk(relaxed = true)
            every { packageName } returns "com.mapbox.navigation.core.MapboxNavigationTest"
            every { filesDir } returns File("some/path")
        }

        every { applicationContext.applicationContext } returns applicationContext
    }

    @Test(/*expected = RuntimeException::class*/)
    fun `destroying the instance directly cleans the provider reference`() {
        val mapboxNavigation = MapboxNavigationProvider.create(
            NavigationOptions.Builder(applicationContext).build()
        )
        mapboxNavigation.onDestroy()

        MapboxNavigationProvider.retrieve()
    }
}
