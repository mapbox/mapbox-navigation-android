package com.mapbox.navigation.core

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.telemetry.MapboxNavigationTelemetry
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test

class MapboxNavigationTelemetryTest {
    // private lateinit var mockContext: Context
    private val mockContext = mockk<Context>()
    private val applicationContext: Context = mockk(relaxed = true)
    private val mockNavigation = mockk<MapboxNavigation>(relaxUnitFun = true)
    private var token = "pk.1234.PABLO'S-FAKE-TOKEN"

    private fun mockIOScopeAndRootJob(): CoroutineScope {
        mockkObject(ThreadController)
        val parentJob = SupervisorJob()
        val testScope = CoroutineScope(parentJob + TestCoroutineDispatcher())
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        every { ThreadController.IODispatcher } returns TestCoroutineDispatcher()
        every { ThreadController.getIOScopeAndRootJob() } returns JobControl(parentJob, testScope)
        return testScope
    }

    // TODO added @Ignore because was causing test failures - init creates MapboxTelemetry which assigns static Context but mockks don't survive across tests
    @Test
    fun telemetryInitTest() {
        every { mockContext.applicationContext } returns applicationContext
        val alarmManager = mockk<AlarmManager>()
        every { applicationContext.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every { applicationContext.getSharedPreferences(MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES, Context.MODE_PRIVATE); } returns sharedPreferences
        every { sharedPreferences.getString("mapboxTelemetryState", "ENABLED"); } returns "DISABLED"
        // TODO commented out because was causing test failures - init creates MapboxTelemetry which assigns static Context but mockks don't survive across tests
        // MapboxMetricsReporter.init(mockContext, token, "User agent")
        assert(MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name, ThreadController.getMainScopeAndRootJob(), NavigationOptions.Builder().build()))
        assert(!MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name, ThreadController.getMainScopeAndRootJob(), NavigationOptions.Builder().build()))
    }

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
