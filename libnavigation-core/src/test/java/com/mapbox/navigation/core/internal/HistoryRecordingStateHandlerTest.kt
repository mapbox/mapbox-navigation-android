package com.mapbox.navigation.core.internal

import com.mapbox.navigation.core.HistoryRecordingStateHandler
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionUtils
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRecordingStateHandlerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val observer = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
    private lateinit var stateHandler: HistoryRecordingStateHandler

    @Before
    fun setUp() {
        stateHandler = HistoryRecordingStateHandler(coroutineRule.coroutineScope)
        stateHandler.registerStateChangeObserver(observer)
    }

    @Test
    fun observersNotification() = coroutineRule.runBlockingTest {
        val observer1 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        val observer2 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        val observer3 = mockk<HistoryRecordingStateChangeObserver>(relaxUnitFun = true)
        stateHandler = HistoryRecordingStateHandler(coroutineRule.coroutineScope)

        // no observers
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer1, observer2, observer3)

        // 1 observer
        stateHandler.registerStateChangeObserver(observer1)
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<NavigationSessionState.FreeDrive>())
            observer1.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2, observer3)

        // 3 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.registerStateChangeObserver(observer2)
        stateHandler.registerStateChangeObserver(observer3)
        stateHandler.setRoutes(emptyList())

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<NavigationSessionState.ActiveGuidance>())
            observer1.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
            observer2.onShouldStopRecording(ofType<NavigationSessionState.ActiveGuidance>())
            observer2.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
            observer3.onShouldStopRecording(ofType<NavigationSessionState.ActiveGuidance>())
            observer3.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }

        // 2 observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterStateChangeObserver(observer2)
        stateHandler.setRoutes(listOf(mockk()))

        verify(exactly = 1) {
            observer1.onShouldStopRecording(ofType<NavigationSessionState.FreeDrive>())
            observer1.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
            observer3.onShouldStopRecording(ofType<NavigationSessionState.FreeDrive>())
            observer3.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
        verifyNoInteractions(observer2)

        // no observers
        clearMocks(observer1, observer2, observer3)

        stateHandler.unregisterAllStateChangeObservers()
        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer1, observer2, observer3)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setEmptyRoutesOverEmptyRoutesFailed() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(emptyList())

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyFailedRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyFailedRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
            observer.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun setNonEmptyRoutesOverEmptyRoutesFailedStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setNonEmptyRoutesOverNonEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.setRoutes(listOf(mockk()))

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverEmptyRoutesFailedStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        val activeGuidances = mutableListOf<NavigationSessionState.ActiveGuidance>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(activeGuidances))
        }
        verify(exactly = 1) {
            observer.onShouldCancelRecording(activeGuidances[0])
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun setRoutesFailedOverNonEmptyRoutesTwiceStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.lastSetRoutesFailed()

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyFailedRoutesStoppedDriving() = coroutineRule.runBlockingTest {
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
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverEmptyExplicitRoutesStoppedDriving() = coroutineRule.runBlockingTest {
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
            observer.onShouldStartRecording(ofType<NavigationSessionState.FreeDrive>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verify(exactly = 0) {
            observer.onShouldStopRecording(any())
            observer.onShouldCancelRecording(any())
        }
        verify(exactly = 1) {
            observer.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStartedOverNonEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
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
            observer.onShouldStartRecording(ofType<NavigationSessionState.ActiveGuidance>())
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.lastSetRoutesFailed()

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyFailedRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.lastSetRoutesFailed()
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.setRoutes(listOf(mockk()))
        clearMocks(observer)
        stateHandler.setRoutes(emptyList())

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val freeDrives = mutableListOf<NavigationSessionState.FreeDrive>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(freeDrives))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(freeDrives[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverEmptyExplicitRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        stateHandler.setRoutes(listOf(mockk()))
        stateHandler.setRoutes(emptyList())
        clearMocks(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesNotDriving() = coroutineRule.runBlockingTest {
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        val activeGuidances = mutableListOf<NavigationSessionState.ActiveGuidance>()
        verify(exactly = 1) {
            observer.onShouldStartRecording(capture(activeGuidances))
        }
        verify(exactly = 1) {
            observer.onShouldStopRecording(activeGuidances[0])
        }
    }

    @Test
    fun onSessionStateChangedToStoppedOverNonEmptyRoutesStoppedDriving() = coroutineRule.runBlockingTest {
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)
        clearMocks(observer)
        stateHandler.setRoutes(listOf(mockk()))

        stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

        verifyNoInteractions(observer)
    }

    @Test
    fun observerUnregistersItselfFromCallback() = coroutineRule.runBlockingTest {
        val unregisteringObserver = object : HistoryRecordingStateChangeObserver {
            override fun onShouldStartRecording(state: NavigationSessionState) {
                stateHandler.unregisterStateChangeObserver(this)
            }

            override fun onShouldStopRecording(state: NavigationSessionState) { }

            override fun onShouldCancelRecording(state: NavigationSessionState) { }
        }
        stateHandler = HistoryRecordingStateHandler(coroutineRule.coroutineScope)
        stateHandler.registerStateChangeObserver(unregisteringObserver)
        stateHandler.registerStateChangeObserver(observer)
        stateHandler.onSessionStateChanged(TripSessionState.STARTED)
        // no ConcurrentModificationException
    }

    @Test
    fun flowUpdates() = coroutineRule.runBlockingTest {
        mockkObject(NavigationSessionUtils) {
            // initial state
            every {
                NavigationSessionUtils.getNewState(any(), any())
            } returns NavigationSessionState.Idle
            val idleId = ""
            stateHandler = HistoryRecordingStateHandler(coroutineRule.coroutineScope)
            stateHandler.registerStateChangeObserver(observer)

            val collectedSessionIds = mutableListOf<String>()
            val job = coroutineRule.coroutineScope.launch {
                stateHandler.sessionIdFlow.collect {
                    collectedSessionIds.add(it)
                }
            }

            // Free Drive
            val freeDriveId1 = "456-654"
            every {
                NavigationSessionUtils.getNewState(any(), any())
            } returns NavigationSessionState.FreeDrive(freeDriveId1)
            stateHandler.onSessionStateChanged(TripSessionState.STARTED)
            // Active Guidance
            val activeGuidanceId = "222-333"
            every {
                NavigationSessionUtils.getNewState(any(), any())
            } returns NavigationSessionState.ActiveGuidance(activeGuidanceId)
            stateHandler.setRoutes(listOf(mockk()))
            // Free Drive
            val freeDriveId2 = "444-555"
            every {
                NavigationSessionUtils.getNewState(any(), any())
            } returns NavigationSessionState.FreeDrive(freeDriveId2)
            stateHandler.setRoutes(emptyList())
            // Idle
            every {
                NavigationSessionUtils.getNewState(any(), any())
            } returns NavigationSessionState.Idle
            stateHandler.onSessionStateChanged(TripSessionState.STOPPED)

            coroutineRule.testDispatcher.advanceUntilIdle()
            job.cancel()

            assertEquals(
                listOf(idleId, freeDriveId1, activeGuidanceId, freeDriveId2, idleId),
                collectedSessionIds
            )
        }
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
}
