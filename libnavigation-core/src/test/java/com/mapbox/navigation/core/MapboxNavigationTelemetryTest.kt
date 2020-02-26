package com.mapbox.navigation.core

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.telemetry.MapboxTelemetry
import com.mapbox.android.telemetry.MapboxTelemetryConstants
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
    private val mockLocationEngine = mockk<LocationEngine>(relaxUnitFun = true)
    private val mockLocationEngineRequest = mockk<LocationEngineRequest>()
    private val telemetry = mockk<MapboxTelemetry>()
    private var token = "pk.1234.PABLO'S-FAKE-TOKEN"
    private val mockedSharedPreferences: SharedPreferences = mockk()
    val mockedEditor: SharedPreferences.Editor = mockk()

    private var expectedJson = "{\"metricName\":\"navigation.feedback\",\"userFeedback\":{\"feedbackType\":\"FEEDBACK_TYPE_ACCIDENT\",\"description\":\"big bad accident\",\"source\":\"FEEDBACK_SOURCE_USER\",\"screenShot\":\"screen shot\"},\"userId\":\"b1962a72-58eb-42f9-b76f-0cbd363950de\",\"audio\":\"unknown\",\"locationsBefore\":[],\"locationsAfter\":[],\"feedbackId\":\"779c8b02-06fd-4073-adb2-dbfc7c66b860\",\"screenshot\":\"screen shot\",\"step\":{\"upcomingType\":\"\",\"upcomingModifier\":\"\",\"upcomingName\":\"\",\"previousType\":\"\",\"previousModifier\":\"\",\"previousName\":\"\",\"distance\":0,\"duration\":0,\"distanceRemaining\":0,\"durationRemaining\":0}}"
    // @Before
    // fun setUp() {
    //     every { telemetry.enable() } returns true
    //     mockContext = createContext("com.mapbox.android.telemetry")
    //     every { mockNavigation.registerRouteProgressObserver(any()) } answers {}
    //     every { mockLocationEngine.requestLocationUpdates(any(), any<LocationEngineCallback<LocationEngineResult>>(), null) } just Runs
    //     mockkConstructor(MapboxTelemetry::class)
    //     every { anyConstructed<MapboxTelemetry>().enable() } returns true
    // }
    //
    // @After
    // fun tearDown() {
    //     clearAllMocks()
    // }

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
        assert(MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name))
        assert(!MapboxNavigationTelemetry.initialize(mockContext, token, mockNavigation, MapboxMetricsReporter, LocationEngine::javaClass.name))
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
