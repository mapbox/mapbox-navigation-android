package com.mapbox.navigation.core.telemetry

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.android.telemetry.MapboxTelemetryConstants
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState.LOCATION_TRACKING
import com.mapbox.navigation.base.trip.model.RouteProgressState.ROUTE_COMPLETE
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.NavigationSession
import com.mapbox.navigation.core.NavigationSession.State.ACTIVE_GUIDANCE
import com.mapbox.navigation.core.NavigationSession.State.FREE_DRIVE
import com.mapbox.navigation.core.NavigationSession.State.IDLE
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.event.NavigationAppUserTurnstileEvent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Test

class MapboxNavigationTelemetryTest {

    private companion object {
        private const val LAST_LOCATION_LAT = 55.5
        private const val LAST_LOCATION_LON = 88.8

        private const val ORIGINAL_ROUTE_GEOMETRY = ""
        private const val ORIGINAL_ROUTE_DISTANCE = 1.1
        private const val ORIGINAL_ROUTE_DURATION = 2.2
        private const val ORIGINAL_ROUTE_ROUTE_INDEX = "10"

        private const val ORIGINAL_ROUTE_OPTIONS_PROFILE = "original_profile"
        private const val ORIGINAL_ROUTE_OPTIONS_REQUEST_UUID = "original_requestUuid"

        private const val ANOTHER_ROUTE_GEOMETRY = ""
        private const val ANOTHER_ROUTE_ROUTE_INDEX = "1"
        private const val ANOTHER_ROUTE_DISTANCE = 123.1
        private const val ANOTHER_ROUTE_DURATION = 235.2

        private const val ANOTHER_ROUTE_OPTIONS_PROFILE = "progress_profile"
        private const val ANOTHER_ROUTE_OPTIONS_REQUEST_UUID = "progress_requestUuid"

        private const val ROUTE_PROGRESS_DISTANCE_REMAINING = 11f
        private const val ROUTE_PROGRESS_DURATION_REMAINING = 22.22
        private const val ROUTE_PROGRESS_DISTANCE_TRAVELED = 15f

        private const val ORIGINAL_STEP_MANEUVER_LOCATION_LATITUDE = 135.21
        private const val ORIGINAL_STEP_MANEUVER_LOCATION_LONGITUDE = 436.5
        private const val ANOTHER_STEP_MANEUVER_LOCATION_LATITUDE = 42.2
        private const val ANOTHER_STEP_MANEUVER_LOCATION_LONGITUDE = 12.4

        private const val STEP_INDEX = 5
        private const val SDK_IDENTIFIER = "mapbox-navigation-android"
    }

    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val navigationOptions: NavigationOptions = mockk(relaxed = true)
    private val locationsCollector: LocationsCollector = mockk()
    private val routeProgress = mockk<RouteProgress>()
    private val originalRoute = mockk<DirectionsRoute>()
    private val anotherRoute = mockk<DirectionsRoute>()
    private val lastLocation = mockk<Location>()
    private val originalRouteOptions = mockk<RouteOptions>()
    private val anotherRouteOptions = mockk<RouteOptions>()
    private val originalRouteLeg = mockk<RouteLeg>()
    private val anotherRouteLeg = mockk<RouteLeg>()
    private val originalRouteStep = mockk<LegStep>()
    private val anotherRouteStep = mockk<LegStep>()
    private val originalRouteSteps = listOf(originalRouteStep)
    private val progressRouteSteps = listOf(anotherRouteStep)
    private val originalRouteLegs = listOf(originalRouteLeg)
    private val progressRouteLegs = listOf(anotherRouteLeg)
    private val originalStepManeuver = mockk<StepManeuver>()
    private val anotherStepManeuver = mockk<StepManeuver>()
    private val originalStepManeuverLocation = mockk<Point>()
    private val anotherStepManeuverLocation = mockk<Point>()
    private val legProgress = mockk<RouteLegProgress>()
    private val stepProgress = mockk<RouteStepProgress>()

    @After
    fun cleanUp() {
        updateSessionState(IDLE)
        unmockkObject(MapboxMetricsReporter)
    }

    @Test
    fun turnstileEvent_sent_on_telemetry_init() {
        baseMock()

        initTelemetry()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
    }

    @Test
    fun turnstileEvent_populated_correctly() {
        baseMock()
        val events = captureMetricsReporter()

        initTelemetry()

        val actualEvent = events[0] as NavigationAppUserTurnstileEvent
        val expectedTurnstileEvent = AppUserTurnstile("mock", "mock").also { it.setSkuId("08") }
        assertEquals(expectedTurnstileEvent.skuId, actualEvent.event.skuId)
    }

    @Test
    fun departEvent_sent_on_active_guidance_when_route_and_routeProgress_available() {
        baseMock()

        baseInitialization()

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
    }

    @Test
    fun departEvent_not_sent_without_route_and_routeProgress() {
        baseMock()

        initTelemetry()
        updateSessionState(ACTIVE_GUIDANCE)

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
    }

    @Test
    fun departEvent_not_sent_without_route() {
        baseMock()

        initTelemetry()
        updateSessionState(ACTIVE_GUIDANCE)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
    }

    @Test
    fun departEvent_not_sent_without_routeProgress() {
        baseMock()

        initTelemetry()
        updateSessionState(ACTIVE_GUIDANCE)
        updateRoute(originalRoute)

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
    }

    @Test
    fun cancelEvent_sent_on_active_guidance_stop() {
        baseMock()

        baseInitialization()
        updateSessionState(FREE_DRIVE)

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertEquals(3, events.size)
        verify { locationsCollector.flushBuffers() }
    }

    @Test
    fun arriveEvent_sent_on_arrival() {
        baseMock()
        mockRouteProgress()
        every { routeProgress.currentState } returns ROUTE_COMPLETE

        baseInitialization()
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationArriveEvent)
        assertEquals(3, events.size)
    }

    @Test
    fun cancel_and_depart_events_sent_on_external_route() {
        baseMock()
        mockAnotherRoute()

        baseInitialization()
        updateRoute(anotherRoute)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 4)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertTrue(events[3] is NavigationDepartEvent)
        assertEquals(4, events.size)
    }

    @Test
    fun depart_event_not_sent_on_external_route_without_route_progress() {
        baseMock()
        mockAnotherRoute()

        baseInitialization()
        updateRoute(anotherRoute)

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertEquals(3, events.size)
    }

    @Test
    fun depart_events_are_different_on_external_route() {
        baseMock()
        mockAnotherRoute()
        val events = captureMetricsReporter()

        baseInitialization()
        updateRoute(anotherRoute)
        updateRouteProgress()

        val firstDepart = events[1] as NavigationDepartEvent
        val secondDepart = events[3] as NavigationDepartEvent
        assertNotSame(firstDepart.originalEstimatedDistance, secondDepart.originalEstimatedDistance)
        assertNotSame(firstDepart.originalRequestIdentifier, secondDepart.originalRequestIdentifier)
    }

    @Test
    fun feedback_and_reroute_events_not_sent_on_arrival() {
        baseMock()
        mockAnotherRoute()
        every { routeProgress.currentState } returns ROUTE_COMPLETE

        baseInitialization()
        postUserFeedback()
        offRoute()
        updateRoute(anotherRoute)
        updateRouteProgress()
        postUserFeedback()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationArriveEvent)
        assertEquals(3, events.size)
    }

    @Test
    fun feedback_and_reroute_events_sent_on_free_drive() {
        baseMock()
        mockAnotherRoute()

        baseInitialization()
        postUserFeedback()
        offRoute()
        updateRoute(anotherRoute)
        updateRouteProgress()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        updateSessionState(FREE_DRIVE)

        val events = captureAndVerifyMetricsReporter(exactly = 9)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationFeedbackEvent)
        assertTrue(events[3] is NavigationRerouteEvent)
        assertTrue(events[4] is NavigationFeedbackEvent)
        assertTrue(events[5] is NavigationFeedbackEvent)
        assertTrue(events[6] is NavigationFeedbackEvent)
        assertTrue(events[7] is NavigationFeedbackEvent)
        assertTrue(events[8] is NavigationCancelEvent)
        assertEquals(9, events.size)
    }

    @Test
    fun feedback_and_reroute_events_sent_on_idle_state() {
        baseMock()
        mockAnotherRoute()

        baseInitialization()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        offRoute()
        updateRoute(anotherRoute)
        updateRouteProgress()
        postUserFeedback()
        updateSessionState(IDLE)

        val events = captureAndVerifyMetricsReporter(exactly = 10)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationFeedbackEvent)
        assertTrue(events[3] is NavigationFeedbackEvent)
        assertTrue(events[4] is NavigationFeedbackEvent)
        assertTrue(events[5] is NavigationFeedbackEvent)
        assertTrue(events[6] is NavigationFeedbackEvent)
        assertTrue(events[7] is NavigationRerouteEvent)
        assertTrue(events[8] is NavigationFeedbackEvent)
        assertTrue(events[9] is NavigationCancelEvent)
        assertEquals(10, events.size)
    }

    @Test
    fun rerouteEvent_sent_on_offRoute() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()

        baseInitialization()
        offRoute()
        updateRoute(anotherRoute)
        updateRouteProgress()
        locationsCollector.flushBuffers()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertTrue(events[2] is NavigationRerouteEvent)
        assertEquals(3, events.size)
    }

    @Test
    fun rerouteEvent_not_sent_on_offRoute_without_routeProgress() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()

        baseInitialization()
        offRoute()
        updateRoute(anotherRoute)
        locationsCollector.flushBuffers()

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertTrue(events[0] is NavigationAppUserTurnstileEvent)
        assertTrue(events[1] is NavigationDepartEvent)
        assertEquals(2, events.size)
    }

    @Test
    fun departEvent_populated_correctly() {
        baseMock()
        val events = captureMetricsReporter()

        baseInitialization()

        val departEvent = events[1] as NavigationDepartEvent
        checkOriginalParams(departEvent, originalRoute)
    }

    @Test
    fun rerouteEvent_populated_correctly() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()
        every { routeProgress.route } returns anotherRoute
        val events = captureMetricsReporter()

        baseInitialization()
        offRoute()
        updateRoute(anotherRoute)
        updateRouteProgress()
        locationsCollector.flushBuffers()

        val rerouteEvent = events[2] as NavigationRerouteEvent
        checkOriginalParams(rerouteEvent, anotherRoute)
    }

    @Test
    fun onInit_registerRouteProgressObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerRouteProgressObserver(any()) } }
    }

    @Test
    fun onInit_registerLocationObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerLocationObserver(any()) } }
    }

    @Test
    fun onInit_registerRoutesObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerRoutesObserver(any()) } }
    }

    @Test
    fun onInit_registerOffRouteObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerOffRouteObserver(any()) } }
    }

    @Test
    fun onInit_registerNavigationSessionObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerNavigationSessionObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterRouteProgressObserver_called() {
        baseMock()

        onUnregister {
            verify(exactly = 1) { mapboxNavigation.unregisterRouteProgressObserver(any()) }
        }
    }

    @Test
    fun onUnregisterListener_unregisterLocationObserver_called() {
        baseMock()

        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterLocationObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterRoutesObserver_called() {
        baseMock()

        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterRoutesObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterOffRouteObserver_called() {
        baseMock()

        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterOffRouteObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterNavigationSessionObserver_called() {
        baseMock()

        onUnregister {
            verify(exactly = 1) { mapboxNavigation.unregisterNavigationSessionObserver(any()) }
        }
    }

    @Test
    fun after_unregister_onInit_registers_all_listeners_again() {
        baseMock()

        initTelemetry()
        resetTelemetry()
        initTelemetry()

        verify(exactly = 2) { mapboxNavigation.registerRouteProgressObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerLocationObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerRoutesObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerOffRouteObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerNavigationSessionObserver(any()) }

        resetTelemetry()
    }

    private fun baseInitialization() {
        initTelemetry()
        updateSessionState(ACTIVE_GUIDANCE)
        updateRoute(originalRoute)
        updateRouteProgress()
    }

    private fun updateSessionState(state: NavigationSession.State) {
        MapboxNavigationTelemetry.onNavigationSessionStateChanged(state)
    }

    private fun updateRoute(route: DirectionsRoute) {
        MapboxNavigationTelemetry.onRoutesChanged(listOf(route))
    }

    private fun updateRouteProgress(count: Int = 10) {
        repeat(count) {
            MapboxNavigationTelemetry.onRouteProgressChanged(routeProgress)
        }
    }

    private fun offRoute() {
        MapboxNavigationTelemetry.onOffRouteStateChanged(true)
    }

    private fun captureMetricsReporter(): List<MetricEvent> {
        val events = mutableListOf<MetricEvent>()
        every { MapboxMetricsReporter.addEvent(capture(events)) } just Runs
        return events
    }

    private fun captureAndVerifyMetricsReporter(exactly: Int): List<MetricEvent> {
        val events = mutableListOf<MetricEvent>()
        verify(exactly = exactly) { MapboxMetricsReporter.addEvent(capture(events)) }
        return events
    }

    private fun baseMock() {
        mockMetricsReporter()
        mockContext()
        mockTelemetryUtils()

        mockLocationCollector()
        mockOriginalRoute()
        mockRouteProgress()
    }

    private fun mockOriginalRoute() {
        every { originalRoute.geometry() } returns ORIGINAL_ROUTE_GEOMETRY
        every { originalRoute.legs() } returns originalRouteLegs
        every { originalRoute.distance() } returns ORIGINAL_ROUTE_DISTANCE
        every { originalRoute.duration() } returns ORIGINAL_ROUTE_DURATION
        every { originalRoute.routeOptions() } returns originalRouteOptions
        every { originalRoute.routeIndex() } returns ORIGINAL_ROUTE_ROUTE_INDEX
        every { originalRouteOptions.profile() } returns ORIGINAL_ROUTE_OPTIONS_PROFILE
        every { originalRouteLeg.steps() } returns originalRouteSteps
        every { originalRouteStep.maneuver() } returns originalStepManeuver
        every { originalStepManeuver.location() } returns originalStepManeuverLocation
        every { originalStepManeuverLocation.latitude() } returns
            ORIGINAL_STEP_MANEUVER_LOCATION_LATITUDE
        every { originalStepManeuverLocation.longitude() } returns
            ORIGINAL_STEP_MANEUVER_LOCATION_LONGITUDE
        every { originalRouteOptions.requestUuid() } returns
            ORIGINAL_ROUTE_OPTIONS_REQUEST_UUID
    }

    private fun mockAnotherRoute() {
        every { anotherRoute.geometry() } returns ANOTHER_ROUTE_GEOMETRY
        every { anotherRoute.distance() } returns ANOTHER_ROUTE_DISTANCE
        every { anotherRoute.duration() } returns ANOTHER_ROUTE_DURATION
        every { anotherRoute.legs() } returns progressRouteLegs
        every { anotherRoute.routeIndex() } returns ANOTHER_ROUTE_ROUTE_INDEX
        every { anotherRoute.routeOptions() } returns anotherRouteOptions
        every { anotherRouteOptions.profile() } returns ANOTHER_ROUTE_OPTIONS_PROFILE
        every { anotherRouteOptions.requestUuid() } returns ANOTHER_ROUTE_OPTIONS_REQUEST_UUID
        every { anotherRouteLeg.steps() } returns progressRouteSteps
        every { anotherRouteStep.maneuver() } returns anotherStepManeuver
        every { anotherStepManeuver.location() } returns anotherStepManeuverLocation
        every { anotherStepManeuverLocation.latitude() } returns
            ANOTHER_STEP_MANEUVER_LOCATION_LATITUDE
        every { anotherStepManeuverLocation.longitude() } returns
            ANOTHER_STEP_MANEUVER_LOCATION_LONGITUDE
    }

    private fun mockRouteProgress() {
        every { routeProgress.route } returns originalRoute
        every { routeProgress.currentState } returns LOCATION_TRACKING
        every { routeProgress.currentLegProgress } returns legProgress
        every { routeProgress.distanceRemaining } returns ROUTE_PROGRESS_DISTANCE_REMAINING
        every { routeProgress.durationRemaining } returns ROUTE_PROGRESS_DURATION_REMAINING
        every { routeProgress.distanceTraveled } returns ROUTE_PROGRESS_DISTANCE_TRAVELED
        every { legProgress.currentStepProgress } returns stepProgress
        every { legProgress.upcomingStep } returns null
        every { legProgress.legIndex } returns 0
        every { legProgress.routeLeg } returns null
        every { stepProgress.stepIndex } returns STEP_INDEX
        every { stepProgress.step } returns null
        every { stepProgress.distanceRemaining } returns 0f
        every { stepProgress.durationRemaining } returns 0.0
    }

    private fun mockMetricsReporter() {
        initMapboxMetricsReporter()
        mockkObject(MapboxMetricsReporter)
        every { MapboxMetricsReporter.addEvent(any()) } just Runs
    }

    private fun mockContext() {
        every { navigationOptions.applicationContext } returns applicationContext
        every { context.applicationContext } returns applicationContext
    }

    private fun mockLocationCollector() {
        every { locationsCollector.flushBuffers() } just Runs
        every { locationsCollector.lastLocation } returns lastLocation
        every { lastLocation.latitude } returns LAST_LOCATION_LAT
        every { lastLocation.longitude } returns LAST_LOCATION_LON

        val onBufferFull = mutableListOf<(List<Location>, List<Location>) -> Unit>()
        every { locationsCollector.collectLocations(capture(onBufferFull)) } just Runs
        every { locationsCollector.flushBuffers() } answers {
            onBufferFull.forEach { it.invoke(listOf(), listOf()) }
        }
    }

    private fun mockTelemetryUtils() {
        val audioManager = mockk<AudioManager>()
        every {
            applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        } returns audioManager
        every { audioManager.getStreamVolume(any()) } returns 1
        every { audioManager.getStreamMaxVolume(any()) } returns 2
        every { audioManager.isBluetoothScoOn } returns true

        val telephonyManager = mockk<TelephonyManager>()
        every {
            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        } returns telephonyManager
        every { telephonyManager.dataNetworkType } returns 5
        every { telephonyManager.networkType } returns 6

        val activityManager = mockk<ActivityManager>()
        every {
            applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
        } returns activityManager
        every { activityManager.runningAppProcesses } returns listOf()
    }

    private fun initTelemetry() {
        MapboxNavigationTelemetry.initialize(
            mapboxNavigation,
            navigationOptions,
            MapboxMetricsReporter,
            mockk(relaxed = true),
            locationsCollector
        )
    }

    private fun resetTelemetry() {
        MapboxNavigationTelemetry.unregisterListeners(mapboxNavigation)
    }

    private fun onInit(block: () -> Unit) {
        initTelemetry()
        block()
        resetTelemetry()
    }

    private fun onUnregister(block: () -> Unit) {
        initTelemetry()
        resetTelemetry()
        block()
    }

    private fun postUserFeedback() {
        MapboxNavigationTelemetry.postUserFeedback("", "", "", null, null, null)
    }

    /**
     * Inside MapboxNavigationTelemetry.initialize method we call postTurnstileEvent and build
     * AppUserTurnstile. It checks a static context field inside MapboxTelemetry.
     * To set that context field we need to init MapboxTelemetry.
     * It is done inside MapboxMetricsReporter.
     * After that method we mock MapboxMetricsReporter to use it in tests.
     */
    private fun initMapboxMetricsReporter() {
        val alarmManager = mockk<AlarmManager>()
        every {
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        } returns alarmManager
        every { context.applicationContext } returns applicationContext

        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        every {
            applicationContext.getSharedPreferences(
                MapboxTelemetryConstants.MAPBOX_SHARED_PREFERENCES,
                Context.MODE_PRIVATE
            )
        } returns sharedPreferences
        every {
            sharedPreferences.getString("mapboxTelemetryState", "ENABLED")
        } returns "DISABLED"

        MapboxMetricsReporter.init(context, "pk.token", "userAgent")
    }

    private fun checkOriginalParams(event: NavigationEvent, currentRoute: DirectionsRoute) {
        assertEquals(SDK_IDENTIFIER, event.sdkIdentifier)
        assertEquals(obtainStepCount(originalRoute), event.originalStepCount)
        assertEquals(originalRoute.distance().toInt(), event.originalEstimatedDistance)
        assertEquals(originalRoute.duration().toInt(), event.originalEstimatedDuration)
        assertEquals(originalRoute.routeOptions()?.requestUuid(), event.originalRequestIdentifier)
        assertEquals(originalRoute.geometry(), event.originalGeometry)
        assertEquals(locationsCollector.lastLocation?.latitude, event.lat)
        assertEquals(locationsCollector.lastLocation?.longitude, event.lng)
        assertEquals(false, event.simulation)
        assertEquals(7, event.eventVersion)

        assertEquals(
            routeProgress.currentLegProgress?.currentStepProgress?.stepIndex,
            event.stepIndex
        )
        assertEquals(routeProgress.distanceRemaining.toInt(), event.distanceRemaining)
        assertEquals(routeProgress.durationRemaining.toInt(), event.durationRemaining)
        assertEquals(routeProgress.distanceTraveled.toInt(), event.distanceCompleted)
        assertEquals(currentRoute.geometry(), event.geometry)
        assertEquals(currentRoute.routeOptions()?.profile(), event.profile)
        assertEquals(currentRoute.routeIndex()?.toInt(), event.legIndex)
        assertEquals(obtainStepCount(currentRoute), event.stepCount)
        assertEquals(currentRoute.legs()?.size, event.legCount)

        if (event is NavigationRerouteEvent) {
            assertEquals(anotherRoute.distance().toInt(), event.newDistanceRemaining)
            assertEquals(anotherRoute.duration().toInt(), event.newDurationRemaining)
            assertEquals(anotherRoute.geometry(), event.newGeometry)
            assertEquals(1, event.rerouteCount)
        } else {
            assertEquals(0, event.rerouteCount)
        }

        assertEquals(
            obtainAbsoluteDistance(lastLocation, obtainRouteDestination(currentRoute)),
            event.absoluteDistanceToDestination
        )
        assertEquals(currentRoute.distance().toInt(), event.estimatedDistance)
        assertEquals(currentRoute.duration().toInt(), event.estimatedDuration)
        assertEquals(obtainStepCount(currentRoute), event.totalStepCount)
    }
}
