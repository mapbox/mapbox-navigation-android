package com.mapbox.navigation.copilot

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkManager
import com.mapbox.common.MapboxOptions
import com.mapbox.common.MapboxServices
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.options.CopilotOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.copilot.CopilotTestUtils.prepareLifecycleOwnerMockk
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.PeriodicHistoryCleanupWorker
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.history.SaveHistoryCallback
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.lifecycle.CarAppLifecycleOwner
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackObserver
import com.mapbox.navigation.core.internal.telemetry.registerUserFeedbackObserver
import com.mapbox.navigation.core.internal.telemetry.unregisterUserFeedbackObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.logD
import io.mockk.CapturingSlot
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class MapboxCopilotImplTest {

    private val fakeAccessToken = "pk.FAKE.ACCESS_TOKEN"

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggingFrontendTestRule = LoggingFrontendTestRule()

    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    private lateinit var mockedContext: Context
    private lateinit var mockedMapboxNavigation: MapboxNavigation
    private lateinit var mockedProcessLifecycleOwner: LifecycleOwner
    private lateinit var mockedHistoryRecorder: MapboxHistoryRecorder
    private lateinit var stubRecordings: List<String>

    private lateinit var sut: MapboxCopilotImpl

    @Before
    fun setUp() {
        mockkStatic(SystemClock::elapsedRealtime)
        every { SystemClock.elapsedRealtime() } answers { System.currentTimeMillis() }

        mockkStatic(MapboxOptions::class)
        every { MapboxOptions.accessToken } returns fakeAccessToken

        mockkStatic(MapboxOptionsUtil::class)
        every {
            MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        } returns fakeAccessToken

        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns mockk(relaxed = true)

        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
        mockkStatic("com.mapbox.navigation.core.internal.telemetry.TelemetryExKt")
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs

        mockkObject(HistoryAttachmentsUtils)
        every { HistoryAttachmentsUtils.delete(any()) } returns false
        every { HistoryAttachmentsUtils.retrieveOwnerFrom(fakeAccessToken) } returns "owner"

        val tmpFolder = folder.newFolder("copilot-test")
        mockedContext = mockk<Application>(relaxed = true) {
            every { applicationContext } returns this
            every { filesDir } returns tmpFolder
            every {
                packageManager.getPackageInfo(
                    any<String>(),
                    any<Int>(),
                )
            } returns PackageInfo().apply { versionName = "1.0" }
            every { getSystemService(Context.ALARM_SERVICE) } returns mockk<AlarmManager>()
        }

        stubRecordings = (0..20).toList().map { "${tmpFolder.absolutePath}/file$it" }

        mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true) {
            every { navigationOptions.copilotOptions } returns CopilotOptions.Builder().build()
            every { navigationOptions.applicationContext } returns mockedContext
        }

        mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true) {
            var i = 0
            every { startRecording() } answers {
                listOf(stubRecordings[i])
            }
            every { stopRecording(any()) } answers {
                firstArg<SaveHistoryCallback>().onSaved(stubRecordings[i++])
            }
        }
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder

        mockedProcessLifecycleOwner = prepareLifecycleOwnerMockk()

        mockkObject(HistoryUploadWorker)
        every { HistoryUploadWorker.uploadHistory(any(), any()) } answers {}

        mockkObject(PeriodicHistoryCleanupWorker)
        every { PeriodicHistoryCleanupWorker.scheduleWork(any(), any()) } answers {}

        sut = createMapboxCopilotImplementation(mockedMapboxNavigation)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `registerUserFeedbackObserver is called when start`() {
        sut.start()

        verify(exactly = 1) { mockedMapboxNavigation.registerUserFeedbackObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is added to MapboxNavigationApp's LifecycleOwner when start if MapboxNavigationApp is setup`() {
        sut.start()

        val lifecycle = mockedProcessLifecycleOwner.lifecycle
        verify(exactly = 1) { lifecycle.addObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is added to CarAppLifecycleOwner when start if MapboxNavigationApp is not setup`() {
        val mockedAppLifecycle = prepareCarAppLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()

        verify(exactly = 1) { mockedAppLifecycle.addObserver(any()) }
    }

    @Test
    fun `registerHistoryRecordingStateChangeObserver is called when start`() {
        sut.start()

        verify(exactly = 1) {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(any())
        }
    }

    @Test
    fun `should schedule PeriodicHistoryCleanupWorker on start`() {
        sut.start()

        verify(exactly = 1) {
            PeriodicHistoryCleanupWorker.scheduleWork(any(), any())
        }
    }

    @Test
    fun `startRecording is called when HistoryRecordingStateChangeObserver#onShouldStartRecording - FreeDrive, shouldRecordFreeDriveHistories = true`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)

        verify(exactly = 1) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `startRecording is not called when HistoryRecordingStateChangeObserver#onShouldStartRecording - FreeDrive, shouldRecordFreeDriveHistories = false`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder()
            .shouldRecordFreeDriveHistories(false)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions

        sut.start()
        sut.onShouldStartRecording(recordingState)

        verify(exactly = 0) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `startRecording is called when HistoryRecordingStateChangeObserver#onShouldStartRecording - ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)

        verify(exactly = 1) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `stopRecording is called when HistoryRecordingStateChangeObserver#onShouldCancelRecording`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldCancelRecording(recordingState)

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `delete is called when HistoryRecordingStateChangeObserver#onShouldCancelRecording`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val recordingFilepath = "path/to/history/file.pbf.gz"
        givenHistoryRecorderSaveHistoryCallback(recordingFilepath)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldCancelRecording(recordingState)

        val deletedFiles = mutableListOf<File>()
        verify(exactly = 2) { HistoryAttachmentsUtils.delete(capture(deletedFiles)) }
        assertEquals(2, deletedFiles.size)
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("pbf.gz") })
        assertNotNull(deletedFiles.firstOrNull { it.name.endsWith("metadata.json") })
    }

    @Test
    fun `stopRecording is called when HistoryRecordingStateChangeObserver#onShouldStopRecording`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `recording is restarted when a session is longer than maxHistoryFileLengthMillis`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(180000)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions

        sut.start()
        sut.onShouldStartRecording(recordingState)
        advanceTimeBy(560000)
        sut.onShouldStopRecording(recordingState)

        verify(ordering = Ordering.SEQUENCE) {
            mockedHistoryRecorder.startRecording()
            mockedHistoryRecorder.stopRecording(any())
            mockedHistoryRecorder.startRecording()
            mockedHistoryRecorder.stopRecording(any())
            mockedHistoryRecorder.startRecording()
            mockedHistoryRecorder.stopRecording(any())
            mockedHistoryRecorder.startRecording()
            mockedHistoryRecorder.pushHistory(DRIVE_ENDS_EVENT_NAME, any())
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `stopRecording is called when stop`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `unregisterUserFeedbackObserver is called when stop`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        verify(exactly = 1) { mockedMapboxNavigation.unregisterUserFeedbackObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is removed from MapboxNavigationApp's LifecycleOwner when stop if MapboxNavigationApp is setup`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        val lifecycle = mockedProcessLifecycleOwner.lifecycle
        verify(exactly = 1) { lifecycle.removeObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is removed from ProcessLifecycleOwner when stop if MapboxNavigationApp is not setup`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedAppLifecycle = prepareCarAppLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()
        mapboxCopilot.onShouldStartRecording(recordingState)
        mapboxCopilot.stop()

        verify(exactly = 1) { mockedAppLifecycle.removeObserver(any()) }
    }

    @Test
    fun `activity Lifecycle Callbacks is unregistered when stop if MapboxNavigationApp is not setup`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        prepareCarAppLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()
        mapboxCopilot.onShouldStartRecording(recordingState)
        mapboxCopilot.stop()

        verify(exactly = 1) { anyConstructed<CarAppLifecycleOwner>().attachAllActivities(any()) }
        verify(exactly = 1) { anyConstructed<CarAppLifecycleOwner>().detachAllActivities(any()) }
    }

    @Test
    fun `unregisterHistoryRecordingStateChangeObserver is called when stop`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        verify(exactly = 1) {
            mockedMapboxNavigation.unregisterHistoryRecordingStateChangeObserver(sut)
        }
    }

    @Test
    fun `stopRecording is not called when stop if HistoryRecordingSessionState is Idle`() {
        val recordingState = mockk<HistoryRecordingSessionState.Idle>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        verify(exactly = 0) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `GoingToForegroundEvent is pushed when onResume`() {
        val appLifecycleObserver = captureAppLifecycleObserver()

        sut.start()
        appLifecycleObserver.captured.onResume(mockk())

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(GOING_TO_FOREGROUND_EVENT_NAME, "{}")
        }
    }

    @Test
    fun `GoingToBackgroundEvent is pushed when onPause`() {
        val appLifecycleObserver = captureAppLifecycleObserver()

        sut.start()
        appLifecycleObserver.captured.onPause(mockk())

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(GOING_TO_BACKGROUND_EVENT_NAME, "{}")
        }
    }

    @Test
    fun `NavFeedbackSubmittedEvent is pushed when onNewUserFeedback and FreeDrive`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val userFeedbackObserver = captureUserFeedbackObserver()

        sut.start()
        sut.onShouldStartRecording(recordingState)
        userFeedbackObserver.captured.onNewUserFeedback(
            mockk<ExtendedUserFeedback>(relaxed = true),
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `NavFeedbackSubmittedEvent is pushed when onNewUserFeedback and ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val userFeedbackObserver = captureUserFeedbackObserver()
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        every { mockedMapboxNavigation.getNavigationRoutes() } returns listOf(mockedNavigationRoute)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        userFeedbackObserver.captured.onNewUserFeedback(
            mockk<ExtendedUserFeedback>(relaxed = true),
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `Events are not pushed when onNewUserFeedback and Idle`() {
        val recordingState = mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        val userFeedbackObserver = captureUserFeedbackObserver()

        sut.start()
        sut.onShouldStartRecording(recordingState)
        userFeedbackObserver.captured.onNewUserFeedback(
            mockk<ExtendedUserFeedback>(relaxed = true),
        )

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `History file is uploaded - shouldSendHistoryOnlyWithFeedback = false`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val session = slot<CopilotSession>()
        every { HistoryUploadWorker.uploadHistory(any(), capture(session)) } answers {}

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 1) { HistoryUploadWorker.uploadHistory(any(), capture(session)) }
        assertEquals(stubRecordings.first(), session.captured.recording)
    }

    @Test
    fun `History file is not uploaded - shouldSendHistoryOnlyWithFeedback = true, hasFeedback = false`() {
        val copilotOptions = CopilotOptions.Builder()
            .shouldSendHistoryOnlyWithFeedback(true)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 0) { HistoryUploadWorker.uploadHistory(any(), any()) }
    }

    @Test
    fun `History file is uploaded - shouldSendHistoryOnlyWithFeedback = true, hasFeedback = true`() {
        val userFeedbackObserver = captureUserFeedbackObserver()
        val copilotOptions = CopilotOptions.Builder()
            .shouldSendHistoryOnlyWithFeedback(true)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        userFeedbackObserver.captured.onNewUserFeedback(mockk(relaxed = true))
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 1) { HistoryUploadWorker.uploadHistory(any(), any()) }
    }

    @Test
    fun `some history files are not uploaded if their number exceeds maxHistoryFilesPerSession`() {
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(10000)
            .maxHistoryFilesPerSession(3)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        advanceTimeBy(51000)
        sut.onShouldStopRecording(recordingState)

        val sessions = mutableListOf<CopilotSession>()
        verify(exactly = 3) { HistoryUploadWorker.uploadHistory(any(), capture(sessions)) }
        val expectedRecordings = listOf(stubRecordings[3], stubRecordings[4], stubRecordings[5])
        assertEquals(expectedRecordings, sessions.map { it.recording })
    }

    @Test
    fun `some history files are not uploaded if their total size exceeds maxTotalHistoryFilesSizePerSession`() {
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(10000)
            .maxTotalHistoryFilesSizePerSession(1000)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        every { HistoryAttachmentsUtils.size(any()) } returns 300

        sut.start()
        sut.onShouldStartRecording(recordingState)
        advanceTimeBy(50000)
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 3) { HistoryUploadWorker.uploadHistory(any(), any()) }
    }

    @Test
    fun `DriveEndsEvent is pushed when uploadHistory - onShouldStopRecording`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(DRIVE_ENDS_EVENT_NAME, any())
        }
    }

    @Test
    fun `DriveEndsEvent is pushed when uploadHistory - stop`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.stop()

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(DRIVE_ENDS_EVENT_NAME, any())
        }
    }

    @Test
    fun `events are not pushed if MapboxCopilot is not started`() {
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.push(SearchResultsEvent(searchResults))

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()
        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultsEvent(searchResults))
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `Only last SearchResultsEvent is pushed when push - ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val firstSearchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        val secondSearchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test2", null)

        sut.start()
        sut.push(SearchResultsEvent(firstSearchResults))
        sut.push(SearchResultsEvent(secondSearchResults))
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest2"}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - FreeDrive, shouldRecordFreeDriveHistories = true`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultsEvent(searchResults))

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultsEvent is not pushed when push - FreeDrive, shouldRecordFreeDriveHistories = false`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        val copilotOptions = CopilotOptions.Builder()
            .shouldRecordFreeDriveHistories(false)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultsEvent(searchResults))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULTS_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - FreeDrive deferred`() {
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.start()
        sut.onShouldStartRecording(mockk<HistoryRecordingSessionState.Idle>(relaxed = true))
        sut.push(SearchResultsEvent(searchResults))
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultsEvent is not pushed when push - Idle`() {
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.start()
        sut.onShouldStartRecording(mockk<HistoryRecordingSessionState.Idle>(relaxed = true))
        sut.push(SearchResultsEvent(searchResults))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULTS_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - Idle deferred`() {
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        sut.start()
        sut.onShouldStartRecording(mockk<HistoryRecordingSessionState.Idle>(relaxed = true))
        sut.push(SearchResultsEvent(searchResults))
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultUsedEvent is pushed when push - ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultUsedEvent(searchResultUsed))
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","id":"test_id","name":"mapbox_poi","address":"mapbox_address","coordinates":{"latitude":0.0,"longitude":0.0}}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULT_USED_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `Only last SearchResultUsedEvent is pushed when push - ActiveGuidance`() {
        val recordingState = mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val firstSearchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id_1",
                "mapbox_poi_1",
                "mapbox_address_1",
                HistoryPoint(0.0, 0.0),
                null,
            )
        val secondSearchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id_2",
                "mapbox_poi_2",
                "mapbox_address_2",
                HistoryPoint(0.0, 0.0),
                null,
            )

        sut.start()
        sut.push(SearchResultUsedEvent(firstSearchResultUsed))
        sut.push(SearchResultUsedEvent(secondSearchResultUsed))
        sut.onShouldStartRecording(recordingState)
        sut.onShouldStopRecording(recordingState)

        val expectedEventJson = """
            {"provider":"mapbox","id":"test_id_2","name":"mapbox_poi_2","address":"mapbox_address_2","coordinates":{"latitude":0.0,"longitude":0.0}}
        """.trimIndent()
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULT_USED_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultUsedEvent is not pushed when push - FreeDrive`() {
        val recordingState = mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultUsedEvent(searchResultUsed))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULT_USED_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultUsedEvent is not pushed when push - Idle`() {
        val recordingState = mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        sut.start()
        sut.onShouldStartRecording(recordingState)
        sut.push(SearchResultUsedEvent(searchResultUsed))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULT_USED_EVENT_NAME, any())
        }
    }

    private fun prepareCarAppLifecycleOwnerMockk(): Lifecycle {
        mockkStatic(MapboxNavigationApp::class)
        every { MapboxNavigationApp.isSetup() } returns false
        mockkConstructor(CarAppLifecycleOwner::class)
        every { anyConstructed<CarAppLifecycleOwner>().attachAllActivities(any()) } just runs
        val appLifecycle = mockk<Lifecycle>(relaxed = true)
        every { anyConstructed<CarAppLifecycleOwner>().lifecycle } returns appLifecycle
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk {
            every { thread } returns Thread.currentThread()
        }
        return appLifecycle
    }

    private fun createMapboxCopilotImplementation(
        mapboxNavigation: MapboxNavigation,
    ) = MapboxCopilotImpl(mapboxNavigation)

    private fun advanceTimeBy(delayTimeMillis: Long) {
        coroutineRule.testDispatcher.scheduler.apply {
            advanceTimeBy(delayTimeMillis)
            runCurrent()
        }
    }

    private fun givenHistoryRecorderSaveHistoryCallback(recordingFilepath: String) {
        every { mockedHistoryRecorder.stopRecording(any()) } answers {
            firstArg<SaveHistoryCallback>().onSaved(recordingFilepath)
        }
    }

    private fun captureUserFeedbackObserver(): CapturingSlot<UserFeedbackObserver> {
        val userFeedbackCallback = slot<UserFeedbackObserver>()
        every {
            mockedMapboxNavigation.registerUserFeedbackObserver(capture(userFeedbackCallback))
        } just Runs
        return userFeedbackCallback
    }

    private fun captureAppLifecycleObserver(): CapturingSlot<DefaultLifecycleObserver> {
        val appLifecycleObserver = slot<DefaultLifecycleObserver>()
        every {
            mockedProcessLifecycleOwner.lifecycle.addObserver(capture(appLifecycleObserver))
        } just Runs
        return appLifecycleObserver
    }
}
