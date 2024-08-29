package com.mapbox.navigation.core.telemetry

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.common.SdkInformation
import com.mapbox.common.TelemetrySystemUtils
import com.mapbox.common.TurnstileEvent
import com.mapbox.common.UserSKUIdentifier
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.accounts.SkuIdProviderImpl
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.base.trip.model.RouteProgressState.TRACKING
import com.mapbox.navigation.base.trip.model.RouteStepProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.accounts.BillingServiceProvider
import com.mapbox.navigation.core.accounts.BillingServiceProxy
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.accounts.MapboxNavigationAccounts
import com.mapbox.navigation.core.internal.telemetry.NavigationCustomEventType
import com.mapbox.navigation.core.internal.telemetry.UserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.toTelemetryLocation
import com.mapbox.navigation.core.telemetry.events.AppMetadata
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackMetadata
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.START
import com.mapbox.navigation.core.telemetry.events.FreeDriveEventType.STOP
import com.mapbox.navigation.core.telemetry.events.LifecycleStateProvider
import com.mapbox.navigation.core.telemetry.events.MetricsDirectionsRoute
import com.mapbox.navigation.core.telemetry.events.MetricsRouteProgress
import com.mapbox.navigation.core.telemetry.events.NavigationArriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCancelEvent
import com.mapbox.navigation.core.telemetry.events.NavigationCustomEvent
import com.mapbox.navigation.core.telemetry.events.NavigationDepartEvent
import com.mapbox.navigation.core.telemetry.events.NavigationEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.NavigationFreeDriveEvent
import com.mapbox.navigation.core.telemetry.events.NavigationRerouteEvent
import com.mapbox.navigation.core.telemetry.events.PhoneState
import com.mapbox.navigation.core.telemetry.events.TelemetryLocation
import com.mapbox.navigation.core.testutil.createRoutesUpdatedResult
import com.mapbox.navigation.core.testutil.ifCaptured
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionState.ActiveGuidance
import com.mapbox.navigation.core.trip.session.NavigationSessionState.FreeDrive
import com.mapbox.navigation.core.trip.session.NavigationSessionState.Idle
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.metrics.MapboxMetricsReporter
import com.mapbox.navigation.metrics.internal.EventsServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryServiceProvider
import com.mapbox.navigation.metrics.internal.TelemetryUtilsDelegate
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.testing.factories.createRouteOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotSame
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxNavigationTelemetryTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private companion object {
        private const val LAST_LOCATION_LAT = 55.5
        private const val LAST_LOCATION_LON = 88.8
        private const val LAST_LOCATION_SPEED = 10.0
        private const val LAST_LOCATION_BEARING = 90.0
        private const val LAST_LOCATION_ALTITUDE = 15.0
        private const val LAST_LOCATION_TIME = 1000L
        private const val LAST_LOCATION_ACCURACY = 222.0
        private const val LAST_LOCATION_VERTICAL_ACCURACY = 111.0

        private const val ANOTHER_LAST_LOCATION_LAT = 66.6
        private const val ANOTHER_LAST_LOCATION_LON = 77.7

        private const val ORIGINAL_ROUTE_GEOMETRY = ""
        private const val ORIGINAL_ROUTE_DISTANCE = 1.1
        private const val ORIGINAL_ROUTE_DURATION = 2.2
        private const val ORIGINAL_ROUTE_ROUTE_INDEX = 10

        private const val ORIGINAL_ROUTE_OPTIONS_PROFILE = "original_profile"
        private const val ORIGINAL_ROUTE_OPTIONS_REQUEST_UUID = "original_requestUuid"

        private const val ANOTHER_ROUTE_GEOMETRY = ""
        private const val ANOTHER_ROUTE_ROUTE_INDEX = 1
        private const val ANOTHER_ROUTE_DISTANCE = 123.1
        private const val ANOTHER_ROUTE_DURATION = 235.2

        private const val ANOTHER_ROUTE_OPTIONS_PROFILE = "progress_profile"
        private const val ANOTHER_ROUTE_OPTIONS_REQUEST_UUID = "progress_requestUuid"

        private const val ROUTE_PROGRESS_DISTANCE_REMAINING = 11f
        private const val ROUTE_PROGRESS_DURATION_REMAINING = 22.22
        private const val ROUTE_PROGRESS_DISTANCE_TRAVELED = 15f

        private const val STEP_INDEX = 5
        private const val SDK_IDENTIFIER = "mapbox-navigation-android-core"
        private const val ACTIVE_GUIDANCE_SESSION_ID = "active-guidance-session-id"
        private const val FREE_DRIVE_SESSION_ID = "free-drive-session-id"

        private const val DEFAULT_FEEDBACK_ID = "default feedback id"
        private const val FEEDBACK_TYPE = "feedback type"
        private const val DESCRIPTION = "feedback description"
        private const val FEEDBACK_SOURCE = "feedback source"
        private const val SCREENSHOT = "feedback screenshot"
        private val FEEDBACK_SUBTYPE = arrayOf("feedback subtype")

        /**
         * Since [MapboxNavigationAccounts] is a singleton, it will effectively obtain
         * an instance of [BillingServiceInterface] from the mocked [BillingServiceProvider]
         * only once when initializing the first test.
         *
         * That instance of returned [BillingServiceInterface] has to have only one mock
         * as well, otherwise, tests following the first one will try to interact with a different
         * mock than [MapboxNavigationAccounts] singleton uses.
         *
         * To ensure that there's only one mock, we're storing it in a companion object.
         */
        private val billingService = mockk<BillingServiceProxy>(relaxed = true)
    }

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val context: Context = mockk(relaxed = true)
    private val applicationContext: Context = mockk(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
    private val navigationOptions: NavigationOptions = mockk(relaxed = true)
    private val locationsCollector: LocationsCollector = mockk(relaxed = true)
    private val routeProgress = mockk<RouteProgress>()
    private lateinit var originalRoute: NavigationRoute
    private lateinit var anotherRoute: NavigationRoute
    private val lastLocation = mockk<Location>()
    private val originalRouteLeg = mockk<RouteLeg>()
    private val anotherRouteLeg = mockk<RouteLeg>()
    private val originalRouteStep = mockk<LegStep>()
    private val anotherRouteStep = mockk<LegStep>()
    private val legProgress = mockk<RouteLegProgress>()
    private val stepProgress = mockk<RouteStepProgress>()
    private val nextRouteLegProgress = mockk<RouteLegProgress>()
    private val globalUserFeedbackCallback = mockk<UserFeedbackCallback>()
    private val localUserFeedbackCallback = mockk<UserFeedbackCallback>()

    private val routeProgressObserverSlot = slot<RouteProgressObserver>()
    private val sessionStateObserverSlot = slot<NavigationSessionStateObserver>()
    private val arrivalObserverSlot = slot<ArrivalObserver>()
    private val routesObserverSlot = slot<RoutesObserver>()
    private val globalUserFeedbackSlot = slot<UserFeedback>()
    private val localUserFeedbackSlot = slot<UserFeedback>()
    private val lifecycleStateProvider = mockk<LifecycleStateProvider>(relaxed = true)

    @Before
    fun setup() {
        mockkObject(BillingServiceProvider)
        mockkStatic(TelemetrySystemUtils::obtainUniversalUniqueIdentifier)
        mockkObject(EventsServiceProvider)
        mockkObject(TelemetryServiceProvider)
        mockkObject(TelemetryUtilsDelegate)
        mockkObject(LifecycleStateProvider)
        every { LifecycleStateProvider.instance } returns lifecycleStateProvider
        every { BillingServiceProvider.getInstance() } returns billingService
        every {
            mapboxNavigation.registerRouteProgressObserver(capture(routeProgressObserverSlot))
        } just runs

        every {
            mapboxNavigation.registerNavigationSessionStateObserver(
                capture(sessionStateObserverSlot),
            )
        } just runs

        every {
            mapboxNavigation.registerArrivalObserver(capture(arrivalObserverSlot))
        } just runs

        every {
            mapboxNavigation.registerRoutesObserver(capture(routesObserverSlot))
        } just runs

        every {
            globalUserFeedbackCallback.onNewUserFeedback(capture(globalUserFeedbackSlot))
        } just runs

        every {
            localUserFeedbackCallback.onNewUserFeedback(capture(localUserFeedbackSlot))
        } just runs

        every { TelemetryUtilsDelegate.getEventsCollectionState() } returns true
        every { TelemetryUtilsDelegate.setEventsCollectionState(any()) } just runs
    }

    @After
    fun cleanUp() {
        unmockkObject(MapboxMetricsReporter)
        unmockkStatic(TelemetrySystemUtils::obtainUniversalUniqueIdentifier)
        unmockkObject(BillingServiceProvider)
        unmockkObject(EventsServiceProvider)
        unmockkObject(TelemetryServiceProvider)
        unmockkObject(TelemetryUtilsDelegate)
        unmockkObject(LifecycleStateProvider)
    }

    @Test
    fun `telemetry idle before call initialize`() {
        baseMock()
        mockAnotherRoute()

        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRouteProgress()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateRoute(originalRoute)
        updateRouteProgress()
        updateRoute(anotherRoute)
        arrive()

        captureAndVerifyMetricsReporter(0)
    }

    @Test
    fun `telemetry identifier is retained`() {
        baseMock()

        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRoute(originalRoute)
        updateRouteProgress()
        resetTelemetry()

        mockAnotherRoute()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRoute(anotherRoute)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(2)
        checkIdentifiersDifferentNavSessions(events.subList(0, 1), events.subList(1, 2))
        checkEventsInSameSession(events.subList(0, 1))
        checkEventsInSameSession(events.subList(1, 2))
    }

    @Test
    fun `telemetry idle after call destroy`() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        resetTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRouteProgress()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(Idle)
        updateRoute(originalRoute)
        updateRouteProgress()
        updateRoute(anotherRoute)
        arrive()

        captureAndVerifyMetricsReporter(0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun `route set before start session moved to ActiveGuidance`() {
        baseMock()

        initTelemetry()
        updateRoute(originalRoute)
        updateRouteProgress()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRouteProgress()
        postUserFeedback()
        locationsCollector.flushBuffers()
        updateRouteProgress()
        arrive()
        resetTelemetry()

        val events = captureAndVerifyMetricsReporter(3)
        events.checkSequence(
            NavigationDepartEvent::class,
            NavigationFeedbackEvent::class,
            NavigationArriveEvent::class,
        )
    }

    @Test
    fun turnstileEvent_sent_on_telemetry_init() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()

        captureAndVerifyMetricsReporter(exactly = 0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun turnstileEvent_populated_correctly() {
        baseMock()
        val expectedTurnstileEvent = TurnstileEvent(
            UserSKUIdentifier.NAV3_CORE_MAU,
        )
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()

        assertEquals(1, turnstileEvents.size)
        val actualEvent = turnstileEvents[0]
        assertEquals(expectedTurnstileEvent.skuId, actualEvent.skuId)
    }

    @Test
    fun departEvent_sent_on_active_guidance_when_route_and_routeProgress_available() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(Idle)
        updateRoute(originalRoute)
        updateRoute(anotherRoute)
        updateRoute(originalRoute)

        captureAndVerifyMetricsReporter(exactly = 0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun active_guidance_events_are_not_sent_in_idle() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertTrue(events[0] is NavigationDepartEvent)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun departEvent_not_sent_without_route_and_routeProgress() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        captureAndVerifyMetricsReporter(exactly = 0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun departEvent_not_sent_without_route() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRouteProgress()

        captureAndVerifyMetricsReporter(exactly = 0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun departEvent_not_sent_without_routeProgress() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRoute(originalRoute)

        captureAndVerifyMetricsReporter(exactly = 0)
        assertEquals(1, turnstileEvents.size)
    }

    @Test
    fun cancelEvent_sent_on_active_guidance_stop() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationCancelEvent)
        assertTrue(events[2] is NavigationFreeDriveEvent)
        assertEquals(3, events.size)
        verify { locationsCollector.flushBuffers() }
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 2), events.subList(2, 3))
    }

    @Test
    fun arriveEvent_sent_on_arrival() {
        baseMock()
        mockRouteProgress()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        arrive()

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationArriveEvent)
        assertEquals(2, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun cancel_and_depart_events_sent_on_external_route() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationCancelEvent)
        assertTrue(events[2] is NavigationDepartEvent)
        assertEquals(3, events.size)
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 2), events.subList(2, 3))
    }

    @Test
    fun depart_event_not_sent_on_external_route_without_route_progress() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute)

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationCancelEvent)
        assertEquals(2, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun depart_events_are_different_on_external_route() {
        baseMock()
        mockAnotherRoute()
        val events = captureMetricsReporter()

        baseInitialization()
        updateRoute(anotherRoute)
        updateRouteProgress()

        val firstDepart = events[0] as NavigationDepartEvent
        val secondDepart = events[2] as NavigationDepartEvent
        assertNotSame(firstDepart.originalEstimatedDistance, secondDepart.originalEstimatedDistance)
        assertNotSame(firstDepart.originalRequestIdentifier, secondDepart.originalRequestIdentifier)
    }

    @Test
    fun alternative_route() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
    }

    @Test
    fun refresh_route() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REFRESH)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
    }

    @Test
    fun clean_up_routes() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        routesObserverSlot.ifCaptured {
            onRoutesChanged(createRoutesUpdatedResult(emptyList(), ""))
        }
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
    }

    @Test
    fun feedback_and_reroute_events_not_sent_on_arrival() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        postUserFeedback()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        postUserFeedback()
        arrive()

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationArriveEvent)
        assertEquals(2, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun feedback_and_reroute_events_sent_on_free_drive() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        postUserFeedback()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        postUserFeedback()
        postUserFeedback()
        resetTelemetry()

        val events = captureAndVerifyMetricsReporter(exactly = 11)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationFeedbackEvent)
        assertTrue(events[2] is NavigationRerouteEvent)
        assertTrue(events[3] is NavigationFeedbackEvent)
        assertTrue(events[4] is NavigationFeedbackEvent)
        assertTrue(events[5] is NavigationFeedbackEvent)
        assertTrue(events[6] is NavigationFeedbackEvent)
        assertTrue(events[7] is NavigationCancelEvent)
        assertTrue(events[8] is NavigationFreeDriveEvent)
        assertTrue(events[9] is NavigationFeedbackEvent)
        assertTrue(events[10] is NavigationFeedbackEvent)
        assertEquals(11, events.size)
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 8), events.subList(8, 11))
    }

    @Test
    fun feedback_and_reroute_events_sent_on_idle_state() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        postUserFeedback()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        postUserFeedback()
        updateSessionState(Idle)

        val events = captureAndVerifyMetricsReporter(exactly = 9)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationFeedbackEvent)
        assertTrue(events[2] is NavigationFeedbackEvent)
        assertTrue(events[3] is NavigationFeedbackEvent)
        assertTrue(events[4] is NavigationFeedbackEvent)
        assertTrue(events[5] is NavigationFeedbackEvent)
        assertTrue(events[6] is NavigationRerouteEvent)
        assertTrue(events[7] is NavigationFeedbackEvent)
        assertTrue(events[8] is NavigationCancelEvent)
        assertEquals(9, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun cache_feedback_send_on_session_stop() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        updateRouteProgress()
        updateSessionState(Idle)
        postUserFeedbackCached()

        val events = captureAndVerifyMetricsReporter(exactly = 4)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationRerouteEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertTrue(events[3] is NavigationFeedbackEvent)
        assertEquals(4, events.size)
    }

    @Test
    fun cache_feedback_post() {
        val sessionIdentifier = "SESSION_IDENTIFIER"
        val driverModeIdentifier = "DRIVER_MODE_IDENTIFIER"
        val driverMode = FeedbackEvent.DRIVER_MODE_FREE_DRIVE
        val driverModeStartTime = "DATE_TIME_FORMAT"
        val rerouteCount = 1
        val mockLocationsBefore = arrayOf<TelemetryLocation>()
        val mockLocationsAfter = arrayOf<TelemetryLocation>()
        val locationEngine = "LOCATION_ENGINE_NAME_EXTERNAL"
        val percentTimeInPortrait = 50
        val percentTimeInForeground = 20
        val eventVersion = 100
        val lastLocation = Point.fromLngLat(30.0, 40.0)
        val phoneState = PhoneState(
            volumeLevel = 5,
            batteryLevel = 11,
            screenBrightness = 16,
            isBatteryPluggedIn = true,
            connectivity = "CONNECTIVITY_STATE",
            audioType = "AUDIO_TYPE",
            applicationState = "APP_STATE",
            created = "CREATED_DATA",
            feedbackId = "FEEDBACK_ID",
            userId = "USER_ID",
        )
        val appMetadata = AppMetadata(
            name = "APP_METADATA_NAME",
            version = "APP_METADATA_VERSION",
            userId = "APP_METADATA_USER_ID",
            sessionId = "APP_METADATA_SESSION_ID",
        )
        val cachedFeedbackMetadata = FeedbackMetadata(
            sessionIdentifier = sessionIdentifier,
            driverModeStartTime = driverModeStartTime,
            driverModeIdentifier = driverModeIdentifier,
            driverMode = driverMode,
            rerouteCount = rerouteCount,
            locationsBeforeEvent = mockLocationsBefore,
            locationsAfterEvent = mockLocationsAfter,
            locationEngineNameExternal = locationEngine,
            simulation = true,
            percentTimeInPortrait = percentTimeInPortrait,
            percentTimeInForeground = percentTimeInForeground,
            eventVersion = eventVersion,
            lastLocation = lastLocation,
            phoneState = phoneState,
            metricsDirectionsRoute = MetricsDirectionsRoute(route = null),
            metricsRouteProgress = MetricsRouteProgress(routeProgress = null),
            appMetadata = appMetadata,
        )
        baseMock()
        baseInitialization()

        postUserFeedbackCached(cachedFeedbackMetadata)
        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationFeedbackEvent)

        val feedbackEvent = events[1] as NavigationFeedbackEvent
        assertEquals(sessionIdentifier, feedbackEvent.navigatorSessionIdentifier)
        assertEquals(driverModeIdentifier, feedbackEvent.sessionIdentifier)
        assertEquals(driverMode, feedbackEvent.driverMode)
        assertEquals(driverModeStartTime, feedbackEvent.startTimestamp)
        assertEquals(rerouteCount, feedbackEvent.rerouteCount)
        assertEquals(mockLocationsBefore, feedbackEvent.locationsBefore)
        assertEquals(mockLocationsAfter, feedbackEvent.locationsAfter)
        assertEquals(locationEngine, feedbackEvent.locationEngine)
        assertEquals(true, feedbackEvent.simulation)
        assertEquals(percentTimeInPortrait, feedbackEvent.percentTimeInPortrait)
        assertEquals(percentTimeInForeground, feedbackEvent.percentTimeInForeground)
        assertEquals(eventVersion, feedbackEvent.eventVersion)
        assertEquals(lastLocation.latitude(), feedbackEvent.lat)
        assertEquals(lastLocation.longitude(), feedbackEvent.lng)
        assertEquals(appMetadata, feedbackEvent.appMetadata)
    }

    @Test
    fun rerouteEvent_sent_on_offRoute() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        locationsCollector.flushBuffers()

        val events = captureAndVerifyMetricsReporter(exactly = 2)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationRerouteEvent)
        assertEquals(2, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun rerouteEvent_accumulates_distance_traveled_on_offRoute() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        locationsCollector.flushBuffers()
        every { routeProgress.currentState } returns RouteProgressState.OFF_ROUTE
        updateRouteProgress(count = 1)
        every { routeProgress.currentState } returns TRACKING
        updateRouteProgress(count = 1)
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        locationsCollector.flushBuffers()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationRerouteEvent)
        assertTrue(events[2] is NavigationRerouteEvent)
        assertEquals(
            (ROUTE_PROGRESS_DISTANCE_TRAVELED * 2).toInt(),
            (events[2] as NavigationRerouteEvent).distanceCompleted,
        )
    }

    @Test
    fun departEvent_populated_correctly() {
        baseMock()
        val events = captureMetricsReporter()

        baseInitialization()

        val departEvent = events[0] as NavigationDepartEvent
        checkOriginalParams(departEvent, originalRoute)
        assertEquals(0, departEvent.distanceCompleted)
    }

    @Test
    fun rerouteEvent_populated_correctly() {
        baseMock()
        mockAnotherRoute()
        mockRouteProgress()
        every { routeProgress.navigationRoute } returns anotherRoute
        val events = captureMetricsReporter()

        baseInitialization()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        locationsCollector.flushBuffers()

        val rerouteEvent = events[1] as NavigationRerouteEvent
        checkOriginalParams(rerouteEvent, anotherRoute)
        assertEquals(routeProgress.distanceTraveled.toInt(), rerouteEvent.distanceCompleted)
    }

    @Test
    fun events_sent_correctly_on_multi_waypoints() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        nextWaypoint()
        updateRouteProgress()
        nextWaypoint()
        updateRouteProgress()
        nextWaypoint()
        updateRouteProgress()
        arrive()

        val events = captureAndVerifyMetricsReporter(exactly = 11)
        assertEquals(1, turnstileEvents.size)
        // origin
        assertTrue(events[0] is NavigationDepartEvent)
        // waypoint1
        assertTrue(events[1] is NavigationArriveEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertTrue(events[3] is NavigationDepartEvent)
        // waypoint2
        assertTrue(events[4] is NavigationArriveEvent)
        assertTrue(events[5] is NavigationCancelEvent)
        assertTrue(events[6] is NavigationDepartEvent)
        // waypoint3
        assertTrue(events[7] is NavigationArriveEvent)
        assertTrue(events[8] is NavigationCancelEvent)
        assertTrue(events[9] is NavigationDepartEvent)
        // destination
        assertTrue(events[10] is NavigationArriveEvent)

        assertEquals(11, events.size)
        checkEventsInSameSession(events)
    }

    @Test
    fun feedback_events_sent_correctly_on_multi_waypoints() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        postUserFeedback()
        nextWaypoint()
        updateRouteProgress()
        mockFlushBuffers()
        postUserFeedback()
        postUserFeedback()
        nextWaypoint()
        updateRouteProgress()
        arrive()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))

        val events = captureAndVerifyMetricsReporter(exactly = 13)
        assertEquals(1, turnstileEvents.size)
        // origin
        assertTrue(events[0] is NavigationDepartEvent)
        // waypoint1
        assertTrue(events[1] is NavigationArriveEvent)
        assertTrue(events[2] is NavigationFeedbackEvent)
        assertTrue(events[3] is NavigationCancelEvent)
        assertTrue(events[4] is NavigationDepartEvent)
        // waypoint2
        assertTrue(events[5] is NavigationArriveEvent)
        assertTrue(events[6] is NavigationFeedbackEvent)
        assertTrue(events[7] is NavigationFeedbackEvent)
        assertTrue(events[8] is NavigationCancelEvent)
        assertTrue(events[9] is NavigationDepartEvent)
        // destination
        assertTrue(events[10] is NavigationArriveEvent)
        assertTrue(events[11] is NavigationCancelEvent)
        // free drive
        assertTrue(events[12] is NavigationFreeDriveEvent)

        assertEquals(13, events.size)
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 12), events.subList(12, 13))
    }

    @Test
    fun reroute_event_sent_correctly_on_multi_waypoints() {
        baseMock()
        mockAnotherRoute()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        nextWaypoint()
        updateRouteProgress()
        nextWaypoint()
        updateRouteProgress()
        mockFlushBuffers()
        updateRoute(anotherRoute, RoutesExtra.ROUTES_UPDATE_REASON_REROUTE)
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))

        val events = captureAndVerifyMetricsReporter(exactly = 10)
        assertEquals(1, turnstileEvents.size)
        // origin
        assertTrue(events[0] is NavigationDepartEvent)
        // waypoint1
        assertTrue(events[1] is NavigationArriveEvent)
        assertTrue(events[2] is NavigationCancelEvent)
        assertTrue(events[3] is NavigationDepartEvent)
        // waypoint2
        assertTrue(events[4] is NavigationArriveEvent)
        assertTrue(events[5] is NavigationCancelEvent)
        assertTrue(events[6] is NavigationDepartEvent)
        // destination
        assertTrue(events[7] is NavigationRerouteEvent)
        assertTrue(events[8] is NavigationCancelEvent)
        // free drive
        assertTrue(events[9] is NavigationFreeDriveEvent)

        assertEquals(10, events.size)
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 9), events.subList(9, 10))
    }

    @Test
    fun freeDrive_sent_when_state_changes_from_idle_to_free_drive() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))

        val events = captureAndVerifyMetricsReporter(exactly = 1)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationFreeDriveEvent)
        checkEventsInSameSession(events)
    }

    @Test
    fun freeDrive_sent_when_state_changes_from_active_guidance_to_free_drive() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        baseInitialization()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationDepartEvent)
        assertTrue(events[1] is NavigationCancelEvent)
        assertTrue(events[2] is NavigationFreeDriveEvent)
        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 2), events.subList(2, 3))
    }

    @Test
    fun freeDrive_sent_when_state_changes_from_free_drive_to_active_guidance() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRoute(originalRoute)
        updateRouteProgress()

        val events = captureAndVerifyMetricsReporter(exactly = 3)
        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationFreeDriveEvent) // start free drive
        assertTrue(events[1] is NavigationFreeDriveEvent) // stop free drive
        assertTrue(events[2] is NavigationDepartEvent)

        checkEventsDividedBySessionsInSameNavSession(events.subList(0, 2), events.subList(2, 3))
    }

    @Test
    fun freeDrive_sent_when_state_changes_from_free_drive_to_idle() {
        baseMock()
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(Idle)

        val events = captureAndVerifyMetricsReporter(exactly = 2)

        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationFreeDriveEvent) // start free drive
        assertTrue(events[1] is NavigationFreeDriveEvent) // stop free drive
        checkEventsInSameSession(events)
    }

    @Test
    fun freeDrive_sent_when_location_not_available() {
        baseMock()
        every { locationsCollector.lastLocation } returns null
        val turnstileEvents = captureTurnstileEvents()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(Idle)

        val events = captureAndVerifyMetricsReporter(exactly = 2)

        assertEquals(1, turnstileEvents.size)
        assertTrue(events[0] is NavigationFreeDriveEvent) // start free drive
        assertTrue(events[1] is NavigationFreeDriveEvent) // stop free drive
        checkEventsInSameSession(events)
    }

    @Test
    fun freeDrive_sent_with_null_location_when_location_not_available_from_free_drive_to_idle() {
        baseMock()
        every { locationsCollector.lastLocation } returns null
        val events = captureMetricsReporter()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        every { locationsCollector.lastLocation } returns lastLocation
        updateSessionState(Idle)

        val freeDriveStart = events[0] as NavigationFreeDriveEvent
        val freeDriveStop = events[1] as NavigationFreeDriveEvent

        assertEquals(null, freeDriveStart.location)
        assertEquals(lastLocation.toTelemetryLocation(), freeDriveStop.location)
    }

    @Test
    fun freeDrive_sent_with_null_location_when_location_not_available_from_free_drive_to_active() {
        baseMock()
        every { locationsCollector.lastLocation } returns null
        val events = captureMetricsReporter()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        every { locationsCollector.lastLocation } returns lastLocation
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val freeDriveStart = events[0] as NavigationFreeDriveEvent
        val freeDriveStop = events[1] as NavigationFreeDriveEvent

        assertEquals(null, freeDriveStart.location)
        assertEquals(lastLocation.toTelemetryLocation(), freeDriveStop.location)
    }

    @Test
    fun freeDrive_start_and_stop_sent_when_state_changes_from_free_drive_to_idle() {
        baseMock()
        val events = captureMetricsReporter()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(Idle)

        val startFreeDriveEvent = events[0] as NavigationFreeDriveEvent
        val stopFreeDriveEvent = events[1] as NavigationFreeDriveEvent
        assertEquals(START.type, startFreeDriveEvent.eventType)
        assertEquals(STOP.type, stopFreeDriveEvent.eventType)
    }

    @Test
    fun freeDrive_start_and_stop_sent_when_state_changes_from_free_drive_to_active_guidance() {
        baseMock()
        val events = captureMetricsReporter()

        initTelemetry()
        updateSessionState(FreeDrive(FREE_DRIVE_SESSION_ID))
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val startFreeDriveEvent = events[0] as NavigationFreeDriveEvent
        val stopFreeDriveEvent = events[1] as NavigationFreeDriveEvent
        assertEquals(START.type, startFreeDriveEvent.eventType)
        assertEquals(STOP.type, stopFreeDriveEvent.eventType)
    }

    @Test
    fun onInit_registerRouteProgressObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerRouteProgressObserver(any()) } }
    }

    @Test
    fun onInit_registerLocationObserver_called() {
        baseMock()
        initMapboxMetricsReporter()

        onInit { verify(exactly = 1) { mapboxNavigation.registerLocationObserver(any()) } }
    }

    @Test
    fun onInit_registerRoutesObserver_called() {
        baseMock()

        onInit { verify(exactly = 1) { mapboxNavigation.registerRoutesObserver(any()) } }
    }

    @Test
    fun onInit_registerNavigationSessionObserver_called() {
        baseMock()

        onInit {
            verify(exactly = 1) { mapboxNavigation.registerNavigationSessionStateObserver(any()) }
        }
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
        initMapboxMetricsReporter()

        onUnregister { verify(exactly = 1) { mapboxNavigation.unregisterRoutesObserver(any()) } }
    }

    @Test
    fun onUnregisterListener_unregisterNavigationSessionObserver_called() {
        baseMock()
        initMapboxMetricsReporter()

        onUnregister {
            verify(exactly = 1) { mapboxNavigation.unregisterNavigationSessionStateObserver(any()) }
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
        verify(exactly = 0) { mapboxNavigation.registerOffRouteObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.registerNavigationSessionStateObserver(any()) }
        verify(exactly = 2) { mapboxNavigation.skuIdProvider }

        resetTelemetry()
    }

    @Test
    fun `user feedback observers are invoked for event without metadata`() {
        baseMock()
        baseInitialization()
        mockLocationCollector()
        val feedbackId = "feedback id"
        every { TelemetrySystemUtils.obtainUniversalUniqueIdentifier() } returns feedbackId

        MapboxNavigationTelemetry.registerUserFeedbackCallback(globalUserFeedbackCallback)
        postUserFeedback()

        verify(exactly = 1) { globalUserFeedbackCallback.onNewUserFeedback(any()) }
        verify(exactly = 1) { localUserFeedbackCallback.onNewUserFeedback(any()) }
        val userFeedback = globalUserFeedbackSlot.captured
        assertEquals(feedbackId, userFeedback.feedbackId)
        assertEquals(FEEDBACK_TYPE, userFeedback.feedbackType)
        assertEquals(DESCRIPTION, userFeedback.description)
        assertEquals(FEEDBACK_SOURCE, userFeedback.source)
        assertEquals(SCREENSHOT, userFeedback.screenshot)
        assertTrue(userFeedback.feedbackSubType.contentEquals(FEEDBACK_SUBTYPE))
        assertEquals(Point.fromLngLat(LAST_LOCATION_LON, LAST_LOCATION_LAT), userFeedback.location)
        assertEquals(userFeedback, localUserFeedbackSlot.captured)
    }

    @Test
    fun `user feedback observers are invoked for event with metadata`() {
        baseMock()
        baseInitialization()
        mockLocationCollector()

        MapboxNavigationTelemetry.registerUserFeedbackCallback(globalUserFeedbackCallback)
        postUserFeedbackCached()

        verify(exactly = 1) { globalUserFeedbackCallback.onNewUserFeedback(any()) }
        val userFeedback = globalUserFeedbackSlot.captured
        assertEquals(DEFAULT_FEEDBACK_ID, userFeedback.feedbackId)
        assertEquals(FEEDBACK_TYPE, userFeedback.feedbackType)
        assertEquals(DESCRIPTION, userFeedback.description)
        assertEquals(FEEDBACK_SOURCE, userFeedback.source)
        assertEquals(SCREENSHOT, userFeedback.screenshot)
        assertTrue(userFeedback.feedbackSubType.contentEquals(FEEDBACK_SUBTYPE))
        assertEquals(
            Point.fromLngLat(ANOTHER_LAST_LOCATION_LON, ANOTHER_LAST_LOCATION_LAT),
            userFeedback.location,
        )
        assertEquals(userFeedback, localUserFeedbackSlot.captured)
    }

    @Test
    fun `user feedback observer is not invoked after unregistering`() {
        baseMock()
        baseInitialization()

        MapboxNavigationTelemetry.registerUserFeedbackCallback(globalUserFeedbackCallback)
        MapboxNavigationTelemetry.unregisterUserFeedbackCallback(globalUserFeedbackCallback)
        postUserFeedback()

        verify(exactly = 0) { globalUserFeedbackCallback.onNewUserFeedback(any()) }
    }

    @Test
    fun `custom event is dispatched when posted`() {
        baseMock()
        baseInitialization()

        postCustomEvent()

        val events = captureAndVerifyMetricsReporter(2)
        events.checkSequence(
            NavigationDepartEvent::class,
            NavigationCustomEvent::class,
        )
    }

    @Test
    fun isSimulationReal() {
        every { navigationOptions.locationOptions } returns LocationOptions.Builder().build()
        baseMock()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val metadata = MapboxNavigationTelemetry.provideFeedbackMetadataWrapper().get()

        assertEquals(false, metadata.simulation)
    }

    @Test
    fun isSimulationMocked() {
        every { navigationOptions.locationOptions } returns LocationOptions.Builder()
            .locationProviderFactory(mockk(), LocationOptions.LocationProviderType.MOCKED)
            .build()
        baseMock()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val metadata = MapboxNavigationTelemetry.provideFeedbackMetadataWrapper().get()

        assertEquals(true, metadata.simulation)
    }

    @Test
    fun isSimulationMixed() {
        every { navigationOptions.locationOptions } returns LocationOptions.Builder()
            .locationProviderFactory(mockk(), LocationOptions.LocationProviderType.MIXED)
            .build()
        baseMock()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val metadata = MapboxNavigationTelemetry.provideFeedbackMetadataWrapper().get()

        assertEquals(true, metadata.simulation)
    }

    @Test
    fun locationProviderDefault() {
        every { navigationOptions.locationOptions } returns LocationOptions.Builder().build()
        baseMock()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val metadata = MapboxNavigationTelemetry.provideFeedbackMetadataWrapper().get()

        assertEquals("default", metadata.locationEngineNameExternal)
    }

    @Test
    fun locationProviderCustom() {
        every { navigationOptions.locationOptions } returns LocationOptions.Builder()
            .locationProviderFactory(mockk(), LocationOptions.LocationProviderType.MIXED)
            .build()
        baseMock()
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))

        val metadata = MapboxNavigationTelemetry.provideFeedbackMetadataWrapper().get()

        assertEquals("custom", metadata.locationEngineNameExternal)
    }

    @Test
    fun telemetryInitializationInitsLifecycleStateProvider() {
        baseMock()
        initTelemetry()

        verify { lifecycleStateProvider.init() }
    }

    @Test
    fun telemetryDestructionDestroysLifecycleStateProvider() {
        baseMock()
        initTelemetry()
        MapboxNavigationTelemetry.destroy(mapboxNavigation)

        verify { lifecycleStateProvider.destroy() }
    }

    private fun baseInitialization() {
        initTelemetry()
        updateSessionState(ActiveGuidance(ACTIVE_GUIDANCE_SESSION_ID))
        updateRoute(originalRoute)
        updateRouteProgress()
    }

    private fun updateSessionState(state: NavigationSessionState) {
        sessionStateObserverSlot.ifCaptured {
            onNavigationSessionStateChanged(state)
        }
    }

    private fun updateRoute(
        route: NavigationRoute,
        @RoutesExtra.RoutesUpdateReason reason: String =
            RoutesExtra.ROUTES_UPDATE_REASON_NEW,
    ) {
        routesObserverSlot.ifCaptured {
            onRoutesChanged(createRoutesUpdatedResult(listOf(route), reason))
        }
    }

    private fun updateRouteProgress(count: Int = 10) {
        routeProgressObserverSlot.ifCaptured {
            repeat(count) {
                onRouteProgressChanged(routeProgress)
            }
        }
    }

    private fun nextWaypoint() {
        arrivalObserverSlot.captured.onNextRouteLegStart(nextRouteLegProgress)
        // mock locationsCollector to do nothing
        // because buffers will be empty after handleSessionCanceled on nextLeg
        every { locationsCollector.flushBuffers() } just Runs
    }

    private fun arrive() {
        arrivalObserverSlot.ifCaptured {
            onFinalDestinationArrival(routeProgress)
        }
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

    private fun captureTurnstileEvents(): List<TurnstileEvent> {
        val turnstileEvents = mutableListOf<TurnstileEvent>()
        every {
            MapboxMetricsReporter.sendTurnstileEvent(capture(turnstileEvents))
        } just runs
        return turnstileEvents
    }

    private fun baseMock() {
        every { mapboxNavigation.skuIdProvider } returns SkuIdProviderImpl()

        mockMetricsReporter()
        mockContext()
        mockTelemetryUtils()

        mockLocationCollector()
        mockOriginalRoute()
        mockRouteProgress()
    }

    private fun mockOriginalRoute() {
        originalRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                routeOptions = createRouteOptions(
                    profile = ORIGINAL_ROUTE_OPTIONS_PROFILE,
                    geometries = DirectionsCriteria.GEOMETRY_POLYLINE6,
                ),
                requestUuid = ORIGINAL_ROUTE_OPTIONS_REQUEST_UUID,
            ),
        )
    }

    private fun mockAnotherRoute() {
        anotherRoute = createNavigationRoute(
            directionsRoute = createDirectionsRoute(
                routeOptions = createRouteOptions(
                    profile = ANOTHER_ROUTE_OPTIONS_PROFILE,
                    geometries = DirectionsCriteria.GEOMETRY_POLYLINE6,
                ),
                requestUuid = ANOTHER_ROUTE_OPTIONS_REQUEST_UUID,
                duration = ANOTHER_ROUTE_DURATION,
                distance = ANOTHER_ROUTE_DISTANCE,
                geometry = ANOTHER_ROUTE_GEOMETRY,
            ),
        )
    }

    private fun mockRouteProgress() {
        every { routeProgress.navigationRoute } returns originalRoute
        every { routeProgress.currentState } returns TRACKING
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
        every { locationsCollector.lastLocation } returns lastLocation
        every { lastLocation.latitude } returns LAST_LOCATION_LAT
        every { lastLocation.longitude } returns LAST_LOCATION_LON
        every { lastLocation.speed } returns LAST_LOCATION_SPEED
        every { lastLocation.bearing } returns LAST_LOCATION_BEARING
        every { lastLocation.altitude } returns LAST_LOCATION_ALTITUDE
        every { lastLocation.timestamp } returns LAST_LOCATION_TIME
        every { lastLocation.horizontalAccuracy } returns LAST_LOCATION_ACCURACY
        every { lastLocation.verticalAccuracy } returns LAST_LOCATION_VERTICAL_ACCURACY

        mockFlushBuffers()
    }

    private fun mockFlushBuffers() {
        val onBufferFull = mutableListOf<LocationsCollector.LocationsCollectorListener>()
        every { locationsCollector.collectLocations(capture(onBufferFull)) } just Runs
        every { locationsCollector.flushBuffers() } answers {
            onBufferFull.forEach { it.onBufferFull(listOf(), listOf()) }
            onBufferFull.clear()
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

        val activityManager = mockk<ActivityManager> {
            every { runningAppProcesses } returns listOf()
            every { getRunningTasks(any()) } returns listOf()
            every { getRunningServices(any()) } returns listOf()
        }
        every {
            applicationContext.getSystemService(Context.ACTIVITY_SERVICE)
        } returns activityManager
    }

    private fun initTelemetry() {
        MapboxNavigationTelemetry.initialize(
            mapboxNavigation,
            navigationOptions,
            MapboxMetricsReporter,
            locationsCollector,
        )
    }

    private fun resetTelemetry() {
        MapboxNavigationTelemetry.destroy(mapboxNavigation)
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

    private fun postCustomEvent() {
        MapboxNavigationTelemetry.postCustomEvent(
            "testPayload",
            NavigationCustomEventType.ANALYTICS,
            "1.2.3",
        )
    }

    private fun postUserFeedback() {
        MapboxNavigationTelemetry.postUserFeedback(
            FEEDBACK_TYPE,
            DESCRIPTION,
            FEEDBACK_SOURCE,
            SCREENSHOT,
            FEEDBACK_SUBTYPE,
            feedbackMetadata = null,
            localUserFeedbackCallback,
        )
    }

    private fun postUserFeedbackCached(
        feedbackMetadata: FeedbackMetadata = FeedbackMetadata(
            sessionIdentifier = "SESSION_ID",
            eventVersion = 0,
            phoneState = PhoneState(
                1, 2, 3, true, "connectivity", "audioType",
                "appState", "01-01-2000", DEFAULT_FEEDBACK_ID, "6",
            ),
            metricsDirectionsRoute = MetricsDirectionsRoute(route = null),
            metricsRouteProgress = MetricsRouteProgress(routeProgress = null),
            lastLocation = Point.fromLngLat(ANOTHER_LAST_LOCATION_LON, ANOTHER_LAST_LOCATION_LAT),
        ),
    ) {
        MapboxNavigationTelemetry.postUserFeedback(
            FEEDBACK_TYPE,
            DESCRIPTION,
            FEEDBACK_SOURCE,
            SCREENSHOT,
            FEEDBACK_SUBTYPE,
            feedbackMetadata,
            localUserFeedbackCallback,
        )
    }

    /**
     * Inside MapboxNavigationTelemetry.initialize method we call postTurnstileEvent and build
     * AppUserTurnstile. It checks a static context field inside MapboxTelemetry.
     * To set that context field we need to init MapboxTelemetry.
     * It is done inside MapboxMetricsReporter.
     * After that method we mock MapboxMetricsReporter to use it in tests.
     */
    private fun initMapboxMetricsReporter() {
        every {
            EventsServiceProvider.provideEventsService(any())
        } returns mockk(relaxUnitFun = true)
        every {
            TelemetryServiceProvider.provideTelemetryService()
        } returns mockk(relaxUnitFun = true)
        val alarmManager = mockk<AlarmManager>()
        every {
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        } returns alarmManager
        every { context.applicationContext } returns applicationContext

        MapboxMetricsReporter.init(SdkInformation("name", "2.16.0", null))
    }

    /**
     * @param kClass must be TargetClass::class. Set [Nothing] to skip check for particular index
     * @param skipTail if true [kClass] might size might be less than target List<MetricEvent>.
     */
    private fun List<MetricEvent>.checkSequence(
        vararg kClass: Any,
        skipTail: Boolean = false,
    ) {
        if (kClass.size > this.size) {
            throw IllegalStateException(
                "clazzes.size(=${kClass.size}) > this.size(=${this.size})",
            )
        }
        if (!skipTail && this.size != kClass.size) {
            throw IllegalStateException(
                "this.size(=${this.size}) must be equal to clazzes.size(=${kClass.size})",
            )
        }
        this.forEachIndexed { index, metricEvent ->
            kClass.getOrNull(index)?.let { clazz ->
                if (clazz != Nothing::class) {
                    assertEquals(metricEvent::class, clazz)
                }
            }
        }
    }

    private fun checkOriginalParams(event: NavigationEvent, currentRoute: NavigationRoute) {
        assertEquals(SDK_IDENTIFIER, event.sdkIdentifier)
        assertEquals(obtainStepCount(originalRoute.directionsRoute), event.originalStepCount)
        assertEquals(
            originalRoute.directionsRoute.distance().toInt(),
            event.originalEstimatedDistance,
        )
        assertEquals(
            originalRoute.directionsRoute.duration().toInt(),
            event.originalEstimatedDuration,
        )
        assertEquals(originalRoute.responseUUID, event.originalRequestIdentifier)
        assertEquals(originalRoute.directionsRoute.geometry(), event.originalGeometry)
        assertEquals(locationsCollector.lastLocation?.latitude, event.lat)
        assertEquals(locationsCollector.lastLocation?.longitude, event.lng)
        assertEquals(false, event.simulation)
        assertEquals(7, event.eventVersion)

        assertEquals(
            routeProgress.currentLegProgress?.currentStepProgress?.stepIndex,
            event.stepIndex,
        )
        assertEquals(routeProgress.distanceRemaining.toInt(), event.distanceRemaining)
        assertEquals(routeProgress.durationRemaining.toInt(), event.durationRemaining)
        assertEquals(currentRoute.directionsRoute.geometry(), event.geometry)
        assertEquals(currentRoute.routeOptions.profile(), event.profile)
        assertEquals(currentRoute.routeIndex.toInt(), event.legIndex)
        assertEquals(obtainStepCount(currentRoute.directionsRoute), event.stepCount)
        assertEquals(currentRoute.directionsRoute.legs()?.size, event.legCount)

        if (event is NavigationRerouteEvent) {
            assertEquals(
                anotherRoute.directionsRoute.distance().toInt(),
                event.newDistanceRemaining,
            )
            assertEquals(
                anotherRoute.directionsRoute.duration().toInt(),
                event.newDurationRemaining,
            )
            assertEquals(anotherRoute.directionsRoute.geometry(), event.newGeometry)
            assertEquals(1, event.rerouteCount)
        } else {
            assertEquals(0, event.rerouteCount)
        }

        assertEquals(
            obtainAbsoluteDistance(
                lastLocation,
                obtainRouteDestination(currentRoute.directionsRoute),
            ),
            event.absoluteDistanceToDestination,
        )
        assertEquals(currentRoute.directionsRoute.distance().toInt(), event.estimatedDistance)
        assertEquals(currentRoute.directionsRoute.duration().toInt(), event.estimatedDuration)
        assertEquals(obtainStepCount(currentRoute.directionsRoute), event.totalStepCount)
    }

    /**
     * Check that nav session identifiers the same for the same telemetry session and
     * different for different telemetry sessions
     */
    private fun checkIdentifiersDifferentNavSessions(
        firstSessionEvents: List<MetricEvent>,
        secondSessionEvents: List<MetricEvent>,
    ) {
        fun List<MetricEvent>.toPair(): List<Pair<String, String>> {
            return this.mapNotNull { event ->
                when (event) {
                    is NavigationEvent -> event.navigatorSessionIdentifier!! to event.toString()
                    is NavigationFreeDriveEvent ->
                        event.navigatorSessionIdentifier!! to event.toString()

                    else ->
                        throw IllegalArgumentException("Unknown event: ${event.javaClass.name}")
                }
            }
        }

        val groupSessions = listOf(firstSessionEvents.toPair(), secondSessionEvents.toPair())
            .filter { it.isNotEmpty() }

        val sessionsIds = mutableListOf<String>()

        groupSessions.forEach { sessionsElements ->
            sessionsElements.reduce { acc, pair ->
                assertEquals(
                    "navSessionIdentifier equals for all events under this session",
                    acc.first,
                    pair.first,
                )
                return@reduce pair
            }.also { (navSessionId, _) ->
                sessionsIds.add(navSessionId)
            }
        }

        if (sessionsIds.size > 1) {
            assertNotSame(
                sessionsIds[0],
                sessionsIds[1],
            )
        }
    }

    private fun checkEventsDividedBySessionsInSameNavSession(vararg events: List<MetricEvent>) {
        val reducedSessionEvents = events.map { checkEventsInSameSession(it) }

        reducedSessionEvents.reduce { acc, sessionEventCompareData ->
            assertSame(
                acc.navigatorSessionIdentifier,
                sessionEventCompareData.navigatorSessionIdentifier,
            )
            return@reduce sessionEventCompareData
        }
    }

    private fun checkEventsInSameSession(events: List<MetricEvent>): SessionEventCompareData {
        val compareData = events.asSessionEventCompareData()

        return compareData.reduce { acc, sessionEventCompareData ->
            assertEquals(
                acc.navigatorSessionIdentifier,
                sessionEventCompareData.navigatorSessionIdentifier,
            )
            assertEquals(
                acc.sessionIdentifier,
                sessionEventCompareData.sessionIdentifier,
            )
            assertEquals(
                acc.startTimestamp,
                sessionEventCompareData.startTimestamp,
            )
            if (
                sessionEventCompareData.driverModeName != SessionEventCompareData.NO_DRIVER_MODE &&
                acc.driverModeName != SessionEventCompareData.NO_DRIVER_MODE
            ) {
                assertEquals(acc.driverModeName, sessionEventCompareData.driverModeName)
            }
            return@reduce sessionEventCompareData
        }
    }

    private fun List<MetricEvent>.asSessionEventCompareData(): List<SessionEventCompareData> {
        return this.mapNotNull { event ->
            when (event) {
                is NavigationEvent -> SessionEventCompareData(
                    event.navigatorSessionIdentifier!!,
                    event.sessionIdentifier!!,
                    event.driverMode!!,
                    event.startTimestamp!!,
                    event.toString(),
                )

                is NavigationFreeDriveEvent -> SessionEventCompareData(
                    event.navigatorSessionIdentifier!!,
                    event.sessionIdentifier!!,
                    SessionEventCompareData.NO_DRIVER_MODE,
                    event.startTimestamp!!,
                    event.toString(),
                )

                else -> throw IllegalArgumentException("Unknown event: ${event.javaClass.name}")
            }
        }
    }

    private data class SessionEventCompareData(
        val navigatorSessionIdentifier: String,
        val sessionIdentifier: String, // driver mode id
        val driverModeName: String = NO_DRIVER_MODE, // freedrive event doesn't have mode name
        val startTimestamp: String, // driver mode start time
        val metadata: String, // debug info: event.toString()
    ) {
        companion object {
            const val NO_DRIVER_MODE = "NO_DRIVER_MODE"
        }
    }
}
