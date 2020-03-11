package com.mapbox.navigation.core

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.Before

class MapboxNavigationTelemetryTest {
    // private lateinit var mockContext: Context
    private val mockContext = mockk<Context>()
    private val applicationContext: Context = mockk(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private var token = "pk.1234"

    @Before
    fun setup() {
        val alarmManager = mockk<AlarmManager>()
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        mockkConstructor(MapboxTelemetry::class)
        every { applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager } returns alarmManager
        every { mockContext.applicationContext } returns applicationContext
        every { applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager } returns alarmManager
        every {
            applicationContext.getSharedPreferences(
                MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
            )
        } returns sharedPreferences
        every { sharedPreferences.getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"
    }

//    @Test
//    fun telemetryInitTest() {
//        MapboxMetricsReporter.init(mockContext, token, "User agent")
//        assert(MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name, ThreadController.getMainScopeAndRootJob(), NavigationOptions.Builder().build()))
//        assert(!MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name, ThreadController.getMainScopeAndRootJob(), NavigationOptions.Builder().build()))
//    }
//
//    @Test
//    fun NavigationDepartEventTest() {
//    }
//    @Test
//    fun NavigationFeedbackEventTest() {
//    }
//    @Test
//    fun RerouteEventTest() {
//    }
//    @Test
//    fun FasterRouteEventTest() {
//    }
//    @Test
//    fun ArriveEventTest() {
//    }
}
