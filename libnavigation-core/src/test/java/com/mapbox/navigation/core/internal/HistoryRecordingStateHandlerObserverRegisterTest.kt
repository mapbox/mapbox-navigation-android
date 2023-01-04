package com.mapbox.navigation.core.internal

import com.mapbox.navigation.core.HistoryRecordingStateHandler
import com.mapbox.navigation.core.trip.session.TripSessionState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRecordingStateHandlerObserverRegisterTest {

    private val observer = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
    private val stateHandler = HistoryRecordingStateHandler()

    @Test
    fun shouldNotNotifyWithIdleState() {
        stateHandler.registerStateChangeObserver(observer)
        verify(exactly = 0) {
            observer.onShouldStartRecording(any())
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
    }

    @Test
    fun shouldNotifyWithFreeDriveState() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.registerStateChangeObserver(observer)
        verify {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.FreeDrive>())
        }
        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
    }

    @Test
    fun shouldNotifyWithActiveGuidanceState() {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.registerStateChangeObserver(observer)
        verify {
            observer.onShouldStartRecording(ofType<HistoryRecordingSessionState.ActiveGuidance>())
        }
        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
    }
}
