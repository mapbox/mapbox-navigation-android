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
class HistoryRecordingStateHandlerCopilotTest {

    private val copilotObserver = mockk<CopilotSessionObserver>(relaxUnitFun = true)
    private lateinit var stateHandler: HistoryRecordingStateHandler

    @Before
    fun setUp() {
        stateHandler = HistoryRecordingStateHandler()
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
            stateHandler.currentCopilotSession() is HistoryRecordingSessionState.ActiveGuidance,
        )
    }

    @Test
    fun shouldNotifyCopilotObserverOnRegister() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)

        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromIdleToFreeDrive() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.FreeDrive>(),
            )
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromIdleToActiveGuidance() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver, answers = false)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.ActiveGuidance>(),
            )
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromIdleToIdle() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        clearMocks(copilotObserver, answers = false)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify(exactly = 0) { copilotObserver.onCopilotSessionChanged(any()) }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromFreeDriveToIdle() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver, answers = false)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromFreeDriveToFreeDrive() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver, answers = false)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) { copilotObserver.onCopilotSessionChanged(any()) }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromFreeDriveToActiveGuidance() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(copilotObserver, answers = false)

        stateHandler.setRoutes(listOf(mockk()))

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.ActiveGuidance>(),
            )
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromActiveGuidanceToIdle() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver, answers = false)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verify {
            copilotObserver.onCopilotSessionChanged(ofType<HistoryRecordingSessionState.Idle>())
        }
    }

    @Test
    fun shouldNotifyCopilotObserverOnChangeFromActiveGuidanceToFreeDrive() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver, answers = false)

        stateHandler.setRoutes(emptyList())

        verify {
            copilotObserver.onCopilotSessionChanged(
                ofType<HistoryRecordingSessionState.FreeDrive>(),
            )
        }
    }

    @Test
    fun shouldNotNotifyCopilotObserverOnChangeFromActiveGuidanceToActiveGuidance() {
        stateHandler.registerCopilotSessionObserver(copilotObserver)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(copilotObserver, answers = false)

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

    private fun verifyNoInteractions(vararg observers: CopilotSessionObserver) {
        observers.forEach {
            verify(exactly = 0) {
                it.onCopilotSessionChanged(any())
            }
        }
    }
}
