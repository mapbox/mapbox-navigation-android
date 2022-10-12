package com.mapbox.navigation.core.internal

import com.mapbox.navigation.core.CopilotSessionObserver
import com.mapbox.navigation.core.HistoryRecordingStateHandler
import com.mapbox.navigation.core.trip.session.TripSessionState
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRecordingStateHandlerTest {

    private val observer = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
    private val copilotObserver = mockk<CopilotSessionObserver>(relaxUnitFun = true)
    private lateinit var stateHandler: HistoryRecordingStateHandler

    @Before
    fun setUp() {
        stateHandler = HistoryRecordingStateHandler()
        stateHandler.registerStateChangeObserver(observer)
        stateHandler.registerCopilotSessionObserver(copilotObserver)
    }

    @Test
    fun currentCopilotSessionShouldBeIdle() {
        assertTrue(stateHandler.currentCopilotSession() is HistoryRecordingSessionState.Idle)
    }

    @Test
    fun currentCopilotSessionShouldBeFreeDrive() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        assertTrue(stateHandler.currentCopilotSession() is HistoryRecordingSessionState.FreeDrive)
    }

    @Test
    fun currentCopilotSessionShouldBeActiveGuidance() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        assertTrue(
            stateHandler.currentCopilotSession() is HistoryRecordingSessionState.ActiveGuidance
        )
    }

    @Test
    fun shouldNotifyCopilotObserverOnRegister() {
        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromIdleToFreeDrive() {
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.FreeDrive>()
            )
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromIdleToActiveGuidance() {
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.ActiveGuidance>()
            )
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromIdleToIdle() {
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) { copilotObserver.onCopilotSessionChanged(any()) }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromFreeDriveToIdle() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromFreeDriveToFreeDrive() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) { copilotObserver.onCopilotSessionChanged(any()) }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromFreeDriveToActiveGuidance() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver)

        stateHandler.setRoutes(listOf(mockk()))

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.ActiveGuidance>()
            )
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromActiveGuidanceToIdle() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromActiveGuidanceToFreeDrive() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver)

        stateHandler.setRoutes(emptyList())

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.FreeDrive>()
            )
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromActiveGuidanceToActiveGuidance() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver)

        stateHandler.setRoutes(listOf(mockk(), mockk()))

        verify(exactly = 0) { copilotObserver.onCopilotSessionChanged(any()) }
    }

    @Test
    fun copilotObserversNotification() {
        val observer1 = mockk<CopilotSessionObserver>(relaxUnitFun = true)
        val observer2 = mockk<CopilotSessionObserver>(relaxUnitFun = true)
        val observer3 = mockk<CopilotSessionObserver>(relaxUnitFun = true)
        stateHandler = HistoryRecordingStateHandler()

        // no observers
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer1, observer2, observer3)

        // 1 observer
        stateHandler.registerCopilotSessionObserver(observer1)
        verify(exactly = 1) {
            observer1.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2, observer3)

        // 3 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.registerCopilotSessionObserver(observer2)
        stateHandler.registerCopilotSessionObserver(observer3)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 1) {
            observer1.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
            observer2.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
            observer3.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }

        // 2 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterCopilotSessionObserver(observer2)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.ActiveGuidance>())
            observer3.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2)

        // no observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterAllCopilotSessionObservers()
        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer1, observer2, observer3)
    }

    @Test
    fun observersNotification() {
        val observer1 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        val observer2 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        val observer3 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        stateHandler = HistoryRecordingStateHandler()

        // no observers
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer1, observer2, observer3)

        // 1 observer
        stateHandler.registerStateChangeObserver(observer1)
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
            observer1.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2, observer3)

        // 3 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.registerStateChangeObserver(observer2)
        stateHandler.registerStateChangeObserver(observer3)
        stateHandler.setRoutes(emptyList())

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
            observer1.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
            observer2.onShouldStopRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
            observer2.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
            observer3.onShouldStopRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
            observer3.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }

        // 2 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterStateChangeObserver(observer2)
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
            observer1.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
            observer3.onShouldStopRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
            observer3.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2)

        // no observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterAllStateChangeObservers()
        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer1, observer2, observer3)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesNotDriving() {
        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesFailed() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyFailedRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyFailedRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesNotDriving() {
        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        val activeGuidances = mutableListOf<HistoryRecordingSessionState.ActiveGuidance>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(activeGuidances))
        }
        verify(exactly = 1) {
            observer.onShouldCancelRecording(activeGuidances[0])
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesNotDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesNotDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(emptyList())

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<HistoryRecordingSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesNotDriving() {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val activeGuidances = mutableListOf<HistoryRecordingSessionState.ActiveGuidance>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(activeGuidances))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(activeGuidances[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesStoppedDriving() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun observerUnregistersItselfFromCallback() {
        val unregisteringObserver = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
                stateHandler.unregisterStateChangeObserver(this)
            }

            override fun onShouldStopRecording(state: HistoryRecordingSessionState) { }

            override fun onShouldCancelRecording(state: HistoryRecordingSessionState) { }
        }
        stateHandler = HistoryRecordingStateHandler()
        stateHandler.registerStateChangeObserver(unregisteringObserver)
        stateHandler.registerStateChangeObserver(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        // no ConcurrentModificationException
    }

    private fun verifyNoInteractions(vararg observers: HistoryRecordingStateChangeObserver) {
        observers.forEach {
            verify(exactly = 0) {
                it.onShouldStartRecording(any())
                it.onShouldStopRecording(any())
                it.onShouldCancelRecording(any())
            }
        }
    }

    private fun verifyNoInteractions(vararg observers: CopilotSessionObserver) {
        observers.forEach {
            verify(exactly = 0) {
                it.onCopilotSessionChanged(any())
            }
        }
    }
}
