package com.mapbox.navigation.copilot

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import android.os.SystemClock
import android.util.Base64
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.common.UploadOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.CopilotOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.copilot.CopilotTestUtils.prepareLifecycleOwnerMockk
import com.mapbox.navigation.copilot.CopilotTestUtils.retrieveAttachments
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.copyToAndRemove
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.size
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.history.SaveHistoryCallback
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.lifecycle.CarAppLifecycleOwner
import com.mapbox.navigation.core.internal.telemetry.UserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.registerUserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.unregisterUserFeedbackCallback
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.utils.internal.logD
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.coEvery
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
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * MapboxCopilotImplTest
 *
 * NOTE FOR FUTURE SECURITY AUDITS:
 * The fakeAccessToken used below in the tests, although it seems legitimate it is a
 * fake one manually generated so that the owner associated to is copilot-test-owner.
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxCopilotImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Before
    fun setUp() {
        mockkStatic(SystemClock::elapsedRealtime)
        every { SystemClock.elapsedRealtime() } answers { System.currentTimeMillis() }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `registerUserFeedbackCallback is called when start`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()

        verify(exactly = 1) { registerUserFeedbackCallback(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is added to MapboxNavigationApp's LifecycleOwner when start if MapboxNavigationApp is setup`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedProcessLifecycleOwner = prepareLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()

        val lifecycle = mockedProcessLifecycleOwner.lifecycle
        verify(exactly = 1) { lifecycle.addObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is added to CarAppLifecycleOwner when start if MapboxNavigationApp is not setup`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedAppLifecycle = prepareCarAppLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()

        verify(exactly = 1) { mockedAppLifecycle.addObserver(any()) }
    }

    @Test
    fun `registerHistoryRecordingStateChangeObserver is called when start`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)

        mapboxCopilot.start()

        verify(exactly = 1) {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(any())
        }
    }

    @Test
    fun `startRecording is called when HistoryRecordingStateChangeObserver#onShouldStartRecording - FreeDrive, shouldRecordFreeDriveHistories = true`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `startRecording is not called when HistoryRecordingStateChangeObserver#onShouldStartRecording - FreeDrive, shouldRecordFreeDriveHistories = false`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .shouldRecordFreeDriveHistories(false)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )

        verify(exactly = 0) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `startRecording is called when HistoryRecordingStateChangeObserver#onShouldStartRecording - ActiveGuidance`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.startRecording()
        }
    }

    @Test
    fun `stopRecording is called when HistoryRecordingStateChangeObserver#onShouldCancelRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        historyRecordingStateChangeObserver.captured.onShouldCancelRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `delete is called when HistoryRecordingStateChangeObserver#onShouldCancelRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        mockkObject(HistoryAttachmentsUtils)
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        val fileSlot = slot<File>()

        historyRecordingStateChangeObserver.captured.onShouldCancelRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            HistoryAttachmentsUtils.delete(capture(fileSlot))
        }
        assertEquals("path/to/history/file", fileSlot.captured.toString())
    }

    @Test
    fun `stopRecording is called when HistoryRecordingStateChangeObserver#onShouldStopRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `recording is restarted when a session is longer than maxHistoryFileLengthMillis`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(180000)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        coroutineRule.testDispatcher.advanceTimeBy(560000)

        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `unregisterUserFeedbackCallback is called when stop`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) { unregisterUserFeedbackCallback(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is removed from MapboxNavigationApp's LifecycleOwner when stop if MapboxNavigationApp is setup`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedProcessLifecycleOwner = prepareLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        val lifecycle = mockedProcessLifecycleOwner.lifecycle
        verify(exactly = 1) { lifecycle.removeObserver(any()) }
    }

    @Test
    fun `foregroundBackgroundLifecycleObserver is removed from ProcessLifecycleOwner when stop if MapboxNavigationApp is not setup`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedAppLifecycle = prepareCarAppLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) { mockedAppLifecycle.removeObserver(any()) }
    }

    @Test
    fun `unregisterHistoryRecordingStateChangeObserver is called when stop`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        mockkStatic("com.mapbox.navigation.core.internal.extensions.MapboxNavigationExtensions")
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) {
            mockedMapboxNavigation.unregisterHistoryRecordingStateChangeObserver(
                historyRecordingStateChangeObserver.captured
            )
        }
    }

    @Test
    fun `stopRecording is not called when stop if HistoryRecordingSessionState is Idle`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val idleGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            idleGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 0) {
            mockedHistoryRecorder.stopRecording(any())
        }
    }

    @Test
    fun `GoingToForegroundEvent is pushed when onResume`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedProcessLifecycleOwner = prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val foregroundBackgroundLifecycleObserver = slot<DefaultLifecycleObserver>()
        every {
            mockedProcessLifecycleOwner.lifecycle.addObserver(
                capture(foregroundBackgroundLifecycleObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedLifecycleOwner = mockk<LifecycleOwner>()

        foregroundBackgroundLifecycleObserver.captured.onResume(mockedLifecycleOwner)

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(GOING_TO_FOREGROUND_EVENT_NAME, "{}")
        }
    }

    @Test
    fun `GoingToBackgroundEvent is pushed when onPause`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val mockedProcessLifecycleOwner = prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val foregroundBackgroundLifecycleObserver = slot<DefaultLifecycleObserver>()
        every {
            mockedProcessLifecycleOwner.lifecycle.addObserver(
                capture(foregroundBackgroundLifecycleObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedLifecycleOwner = mockk<LifecycleOwner>()

        foregroundBackgroundLifecycleObserver.captured.onPause(mockedLifecycleOwner)

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(GOING_TO_BACKGROUND_EVENT_NAME, "{}")
        }
    }

    @Test
    fun `NavFeedbackSubmittedEvent is pushed when onNewUserFeedback and FreeDrive`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )
        val mockedUserFeedback = mockk<UserFeedback>(relaxed = true)

        userFeedbackCallback.captured.onNewUserFeedback(mockedUserFeedback)

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `NavFeedbackSubmittedEvent is pushed when onNewUserFeedback and ActiveGuidance`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        every { mockedMapboxNavigation.getNavigationRoutes() } returns listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        val mockedUserFeedback = mockk<UserFeedback>(relaxed = true)

        userFeedbackCallback.captured.onNewUserFeedback(mockedUserFeedback)

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `Events are not pushed when onNewUserFeedback and Idle`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        every { mockedMapboxNavigation.getNavigationRoutes() } returns listOf(
            mockedNavigationRoute
        )
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val idleHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            idleHistoryRecordingSessionState
        )

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, any())
        }
    }

    @Test
    fun `History file is uploaded - shouldSendHistoryOnlyWithFeedback = false`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        val mockedUserFeedback = mockk<UserFeedback>(relaxed = true)
        userFeedbackCallback.captured.onNewUserFeedback(mockedUserFeedback)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `History file is uploaded - shouldSendHistoryOnlyWithFeedback = true, hasFeedback = false`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        every { registerUserFeedbackCallback(any()) } just Runs
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        val copilotOptions = CopilotOptions.Builder()
            .shouldSendHistoryOnlyWithFeedback(true)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 0) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
        verify(exactly = 1) {
            HistoryAttachmentsUtils.delete(File("path/to/history/file"))
        }
    }

    @Test
    fun `History file is not uploaded - shouldSendHistoryOnlyWithFeedback = true, hasFeedback = true`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        val copilotOptions = CopilotOptions.Builder()
            .shouldSendHistoryOnlyWithFeedback(true)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        val mockedUserFeedback = mockk<UserFeedback>(relaxed = true)
        userFeedbackCallback.captured.onNewUserFeedback(mockedUserFeedback)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `some history files are not uploaded if their number exceeds maxHistoryFilesPerSession`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(180000)
            .maxHistoryFilesPerSession(2)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        var historyFileNumber = 0
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file/${historyFileNumber++}")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        coroutineRule.testDispatcher.advanceTimeBy(560000)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 2) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `some history files are not uploaded if their total size exceeds maxTotalHistoryFilesSizePerSession`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(180000)
            .maxTotalHistoryFilesSizePerSession(3200)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        var historyFileNumber = 0
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file/${historyFileNumber++}")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        coroutineRule.testDispatcher.advanceTimeBy(560000)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 3) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `one history file is uploaded even if its size exceeds maxTotalHistoryFilesSizePerSession`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .maxHistoryFileLengthMillis(180000)
            .maxTotalHistoryFilesSizePerSession(200)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        var historyFileNumber = 0
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file/${historyFileNumber++}")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        coroutineRule.testDispatcher.advanceTimeBy(560000)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `InitRouteEvent is pushed when startRecordingHistory and initial route`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState,
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    @Test
    fun `InitRouteEvent is not pushed when startRecordingHistory if not initial route - not Active Guidance`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState,
        )

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    @Test
    fun `InitRouteEvent is not pushed when startRecordingHistory if not initial route - empty navigation routes`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoutes = emptyList<NavigationRoute>()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState,
        )

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    @Test
    fun `InitRouteEvent is not pushed when startRecordingHistory if not initial route - init route true`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)

        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState,
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    @Test
    fun `History file is uploaded through UploadService - onShouldStopRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `History file is uploaded through UploadService - stop`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `AttachmentMetadata size`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        val attachmentsMetadata = mockedUploadOptions.captured.metadata
        val attachments = retrieveAttachments(attachmentsMetadata)
        assertEquals(1, attachments.size)
    }

    @Test
    fun `AttachmentMetadata created is set to startedAt`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val startedAt = "2022-05-12T17:47:42.353Z"
        every {
            HistoryAttachmentsUtils.utcTimeNow(eq("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), eq(Locale.US))
        } returns startedAt
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        val attachmentsMetadata = mockedUploadOptions.captured.metadata
        val attachments = retrieveAttachments(attachmentsMetadata)
        assertEquals(startedAt, attachments[0].created)
    }

    @Test
    fun `AttachmentMetadata sessionId - debug appMode`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder().build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedApplicationContext = mockk<Context>(relaxed = true)
        every {
            mockedContext.applicationContext
        } returns mockedApplicationContext
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.0"
        every {
            mockedContext.packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns packageInfo
        val applicationInfo = ApplicationInfo()
        applicationInfo.flags = 820559686
        every { mockedContext.applicationInfo } returns applicationInfo
        every {
            mockedApplicationContext.getSystemService(Context.ALARM_SERVICE)
        } returns mockk<AlarmManager>()
        every {
            mockedMapboxNavigation.navigationOptions.applicationContext
        } returns mockedContext
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedSessionId = slot<String>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                capture(mockedSessionId),
            )
        } just Runs
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        mockkStatic("com.mapbox.navigation.core.internal.telemetry.TelemetryExKt")
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        prepareLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals(
            "co-pilot/copilot-test-owner/1.x/mbx-debug/-/-/active-guidance/-/",
            mockedSessionId.captured,
        )
    }

    @Test
    fun `AttachmentMetadata sessionId - prod appMode`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder().build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedApplicationContext = mockk<Context>(relaxed = true)
        every {
            mockedContext.applicationContext
        } returns mockedApplicationContext
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.0"
        every {
            mockedContext.packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns packageInfo
        val applicationInfo = ApplicationInfo()
        applicationInfo.flags = 0
        every { mockedContext.applicationInfo } returns applicationInfo
        every {
            mockedApplicationContext.getSystemService(Context.ALARM_SERVICE)
        } returns mockk<AlarmManager>()
        every {
            mockedMapboxNavigation.navigationOptions.applicationContext
        } returns mockedContext
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedSessionId = slot<String>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                capture(mockedSessionId),
            )
        } just Runs
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        mockkStatic("com.mapbox.navigation.core.internal.telemetry.TelemetryExKt")
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        prepareLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals(
            "co-pilot/copilot-test-owner/1.x/mbx-prod/-/-/active-guidance/-/",
            mockedSessionId.captured,
        )
    }

    @Test
    fun `AttachmentMetadata sessionId - Active Guidance`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedSessionId = slot<String>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                capture(mockedSessionId),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals(
            "co-pilot/copilot-test-owner/1.x/mbx-prod/-/-/active-guidance/-/",
            mockedSessionId.captured,
        )
    }

    @Test
    fun `AttachmentMetadata sessionId - Free Drive`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedSessionId = slot<String>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                capture(mockedSessionId),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals(
            "co-pilot/copilot-test-owner/1.x/mbx-prod/-/-/free-drive/-/",
            mockedSessionId.captured,
        )
    }

    @Test
    fun `AttachmentMetadata sessionId - driveId`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedSessionId = slot<String>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                any(),
                capture(mockedSessionId),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        every {
            mockedHistoryRecordingSessionState.sessionId
        } returns "3e48fd7a-fc82-42a8-9bae-baeb724f92ce"
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals(
            "co-pilot/copilot-test-owner/1.x/mbx-prod/-/-" +
                "/active-guidance/-/3e48fd7a-fc82-42a8-9bae-baeb724f92ce",
            mockedSessionId.captured,
        )
    }

    @Test
    fun `AttachmentMetadata name - default`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        every { mockedMapboxNavigation.navigationOptions.eventsAppMetadata?.userId } returns null
        every { mockedMapboxNavigation.navigationOptions.eventsAppMetadata?.sessionId } returns null
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        every {
            HistoryAttachmentsUtils.utcTimeNow(eq("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), eq(Locale.US))
        } returns "2022-05-12T17:47:42.353Z" andThen "2022-05-12T17:48:12.504Z"
        every { HistoryAttachmentsUtils.retrieveNavSdkVersion() } returns "2.x.x"
        every { HistoryAttachmentsUtils.retrieveNavNativeSdkVersion() } returns "999.x.x"
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        val expectedFilename = "2022-05-12T17:47:42.353Z__2022-05-12T17:48:12.504Z__android__" +
            "2.x.x__999.x.x_____1.0______.pbf.gz"
        val attachmentsMetadata = mockedUploadOptions.captured.metadata
        val attachments = retrieveAttachments(attachmentsMetadata)
        assertEquals(expectedFilename, attachments[0].name)
    }

    @Test
    fun `AttachmentMetadata name - EventsAppMetadata`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        every {
            mockedMapboxNavigation.navigationOptions.eventsAppMetadata?.userId
        } returns "wBzYwfK0oCYMTNYPIFHhYuYOLLs1"
        every {
            mockedMapboxNavigation.navigationOptions.eventsAppMetadata?.sessionId
        } returns "3e48fd7b-ac82-42a8-9abe-aaeb724f92ce"
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        every { HistoryAttachmentsUtils.retrieveSpecVersion() } returns "1.x"
        every {
            HistoryAttachmentsUtils.utcTimeNow(eq("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"), eq(Locale.US))
        } returns "2022-05-12T17:47:42.353Z" andThen "2022-05-12T17:48:12.504Z"
        every { HistoryAttachmentsUtils.retrieveNavSdkVersion() } returns "2.x.x"
        every { HistoryAttachmentsUtils.retrieveNavNativeSdkVersion() } returns "999.x.x"
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        val expectedFilename = "2022-05-12T17:47:42.353Z__2022-05-12T17:48:12.504Z__android__" +
            "2.x.x__999.x.x_____1.0__" +
            "wBzYwfK0oCYMTNYPIFHhYuYOLLs1__3e48fd7b-ac82-42a8-9abe-aaeb724f92ce.pbf.gz"
        val attachmentsMetadata = mockedUploadOptions.captured.metadata
        val attachments = retrieveAttachments(attachmentsMetadata)
        assertEquals(expectedFilename, attachments[0].name)
    }

    @Test
    fun `AttachmentMetadata format is gz`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        val attachmentsMetadata = mockedUploadOptions.captured.metadata
        val attachments = retrieveAttachments(attachmentsMetadata)
        assertEquals("gz", attachments[0].format)
    }

    @Test
    fun `UploadOptions Media Type type is application zip`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertEquals("application/zip", mockedUploadOptions.captured.mediaType)
    }

    @Test
    fun `UploadOptions URL is production if not DEBUG`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        prepareUploadMockks()
        every { HistoryAttachmentsUtils.retrieveIsDebug() } returns false
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertTrue(
            mockedUploadOptions.captured.url == "https://events.mapbox.com" +
                "/attachments/v1?access_token=$fakeAccessToken",
        )
    }

    @Test
    fun `UploadOptions URL is staging if DEBUG`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder().build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val mockedContext = mockk<Context>(relaxed = true)
        val mockedApplicationContext = mockk<Context>(relaxed = true)
        every {
            mockedContext.applicationContext
        } returns mockedApplicationContext
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.0"
        every {
            mockedContext.packageManager.getPackageInfo(any<String>(), any<Int>())
        } returns packageInfo
        every {
            mockedApplicationContext.getSystemService(Context.ALARM_SERVICE)
        } returns mockk<AlarmManager>()
        every {
            mockedMapboxNavigation.navigationOptions.applicationContext
        } returns mockedContext
        val fakeAccessToken = "pk.eyJ1IjoiY29waWxvdC10ZXN0LW93bmVyIiwiYSI6ImZha2UifQ.8badf00d"
        every { mockedMapboxNavigation.navigationOptions.accessToken } returns fakeAccessToken
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        prepareUploadMockks()
        every { HistoryAttachmentsUtils.retrieveIsDebug() } returns true
        val mockedUploadOptions = slot<UploadOptions>()
        mockkObject(HistoryUploadWorker)
        every {
            HistoryUploadWorker.uploadHistory(
                any(),
                any(),
                capture(mockedUploadOptions),
                any(),
            )
        } just Runs
        val saveHistoryCallback = slot<SaveHistoryCallback>()
        every { mockedHistoryRecorder.stopRecording(capture(saveHistoryCallback)) } answers {
            saveHistoryCallback.captured.onSaved("path/to/history/file")
        }
        prepareLifecycleOwnerMockk()
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        assertTrue(
            mockedUploadOptions.captured.url == "https://api-events-staging.tilestream.net/" +
                "attachments/v1?access_token=$fakeAccessToken",
        )
    }

    @Test
    fun `DriveEndsEvent is pushed when uploadHistory - onShouldStopRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockedHistoryRecordingSessionState
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(DRIVE_ENDS_EVENT_NAME, any())
        }
    }

    @Test
    fun `DriveEndsEvent is pushed when uploadHistory - stop`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )

        mapboxCopilot.stop()

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(DRIVE_ENDS_EVENT_NAME, any())
        }
    }

    @Test
    fun `events are not pushed if MapboxCopilot is not started`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        mapboxCopilot.push(SearchResultsEvent(searchResults))
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
    fun `startRecording as soon as onShouldStartRecording`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        mapboxCopilot.push(SearchResultsEvent(searchResults))
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )
        val expectedEventJson = """
            {"provider":"mapbox","request":"https://mapbox.com","searchQuery":"?query\u003dtest1"}
        """.trimIndent()

        verifyOrder {
            mockedHistoryRecorder.startRecording()
            mockedHistoryRecorder.pushHistory(
                SEARCH_RESULTS_EVENT_NAME,
                expectedEventJson,
            )
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - ActiveGuidance`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        mapboxCopilot.push(SearchResultsEvent(searchResults))
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val firstSearchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        val secondSearchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test2", null)

        mapboxCopilot.push(SearchResultsEvent(firstSearchResults))
        mapboxCopilot.push(SearchResultsEvent(secondSearchResults))
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        mapboxCopilot.push(SearchResultsEvent(searchResults))

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
        val mockedMapboxNavigation = prepareBasicMockks()
        val copilotOptions = CopilotOptions.Builder()
            .shouldRecordFreeDriveHistories(false)
            .build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        mapboxCopilot.push(SearchResultsEvent(searchResults))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULTS_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - FreeDrive deferred`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveGuidanceHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        mapboxCopilot.push(SearchResultsEvent(searchResults))
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val idleDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            idleDriveHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)

        mapboxCopilot.push(SearchResultsEvent(searchResults))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULTS_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultsEvent is pushed when push - Idle deferred`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val idleGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            idleGuidanceHistoryRecordingSessionState
        )
        val searchResults =
            SearchResults("mapbox", "https://mapbox.com", null, null, "?query=test1", null)
        mapboxCopilot.push(SearchResultsEvent(searchResults))
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        mapboxCopilot.push(SearchResultUsedEvent(searchResultUsed))
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val activeGuidanceHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
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

        mapboxCopilot.push(SearchResultUsedEvent(firstSearchResultUsed))
        mapboxCopilot.push(SearchResultUsedEvent(secondSearchResultUsed))
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            activeGuidanceHistoryRecordingSessionState
        )
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            activeGuidanceHistoryRecordingSessionState,
        )

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
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val freeDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            freeDriveHistoryRecordingSessionState
        )
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        mapboxCopilot.push(SearchResultUsedEvent(searchResultUsed))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULT_USED_EVENT_NAME, any())
        }
    }

    @Test
    fun `SearchResultUsedEvent is not pushed when push - Idle`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val userFeedbackCallback = slot<UserFeedbackCallback>()
        every { registerUserFeedbackCallback(capture(userFeedbackCallback)) } just Runs
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val idleDriveHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.Idle>(relaxed = true)
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            idleDriveHistoryRecordingSessionState
        )
        val searchResultUsed =
            SearchResultUsed(
                "mapbox",
                "test_id",
                "mapbox_poi",
                "mapbox_address",
                HistoryPoint(0.0, 0.0),
                null,
            )

        mapboxCopilot.push(SearchResultUsedEvent(searchResultUsed))

        verify(exactly = 0) {
            mockedHistoryRecorder.pushHistory(SEARCH_RESULT_USED_EVENT_NAME, any())
        }
    }

    @Test
    fun `two active guidance sessions are started and stopped, computation dispatcher doesn't work during the first session`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)
        coroutineRule.testDispatcher.pauseDispatcher()
        val mapboxCopilot = createMapboxCopilotImplementation(mockedMapboxNavigation)
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        )
        coroutineRule.testDispatcher.resumeDispatcher()
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        )

        // Init route event is supposed to be recorded only during the second session
        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    @Test
    fun `The first active guidance session is cancelled, the second is stopped`() {
        val mockedMapboxNavigation = prepareBasicMockks()
        prepareLifecycleOwnerMockk()
        val mockedHistoryRecorder = mockk<MapboxHistoryRecorder>(relaxed = true)
        every {
            mockedMapboxNavigation.retrieveCopilotHistoryRecorder()
        } returns mockedHistoryRecorder
        val historyRecordingStateChangeObserver = slot<HistoryRecordingStateChangeObserver>()
        every {
            mockedMapboxNavigation.registerHistoryRecordingStateChangeObserver(
                capture(historyRecordingStateChangeObserver)
            )
        } just Runs
        val routesObserver = slot<RoutesObserver>()
        every {
            mockedMapboxNavigation.registerRoutesObserver(capture(routesObserver))
        } just Runs
        val mockedNavigationRoute = mockk<NavigationRoute>(relaxed = true)
        val mockedNavigationRoutes = listOf(mockedNavigationRoute)

        val mapboxCopilot = createMapboxCopilotImplementation(
            mockedMapboxNavigation
        )
        mapboxCopilot.start()
        val mockedHistoryRecordingSessionState =
            mockk<HistoryRecordingSessionState.ActiveGuidance>(relaxed = true)
        val mockedRoutesResult = mockk<RoutesUpdatedResult>()
        every { mockedRoutesResult.navigationRoutes } returns mockedNavigationRoutes

        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldCancelRecording(
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        )
        historyRecordingStateChangeObserver.captured.onShouldStartRecording(
            mockedHistoryRecordingSessionState
        )
        routesObserver.captured.onRoutesChanged(mockedRoutesResult)
        historyRecordingStateChangeObserver.captured.onShouldStopRecording(
            mockk<HistoryRecordingSessionState.FreeDrive>(relaxed = true)
        )

        verify(exactly = 1) {
            mockedHistoryRecorder.pushHistory(INIT_ROUTE_EVENT_NAME, any())
        }
    }

    private fun prepareBasicMockks(): MapboxNavigation {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        val copilotOptions = CopilotOptions.Builder().build()
        every { mockedMapboxNavigation.navigationOptions.copilotOptions } returns copilotOptions
        val mockedContext = mockk<Application>(relaxed = true)
        every {
            mockedContext.applicationContext
        } returns mockedContext
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1.0"
        every {
            mockedContext.packageManager.getPackageInfo(
                any<String>(),
                any<Int>(),
            )
        } returns packageInfo
        every {
            mockedContext.getSystemService(Context.ALARM_SERVICE)
        } returns mockk<AlarmManager>()
        every {
            mockedMapboxNavigation.navigationOptions.applicationContext
        } returns mockedContext
        mockkStatic("com.mapbox.navigation.core.internal.telemetry.TelemetryExKt")
        mockkStatic("com.mapbox.navigation.utils.internal.LoggerProviderKt")
        every { logD(msg = any(), category = any()) } just Runs
        return mockedMapboxNavigation
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

    private fun prepareUploadMockks() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        mockkObject(HistoryAttachmentsUtils)
        val fileSlot = slot<File>()
        val mockedFile = mockk<File>(relaxed = true)
        coEvery { copyToAndRemove(capture(fileSlot), any()) } coAnswers {
            every { mockedFile.absolutePath } returns fileSlot.captured.toString()
            mockedFile
        }
        every { size(any()) } returns 1000
    }

    private fun createMapboxCopilotImplementation(
        mapboxNavigation: MapboxNavigation,
    ) = MapboxCopilotImpl(mapboxNavigation, coroutineRule.testDispatcher)
}
