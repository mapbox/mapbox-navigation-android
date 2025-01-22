package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionUtils
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import java.util.concurrent.CopyOnWriteArraySet

@UiThread
internal class HistoryRecordingStateHandler : TripSessionStateObserver {

    private var currentState: HistoryRecordingSessionState = HistoryRecordingSessionState.Idle

    private val historyRecordingStateChangeObservers =
        CopyOnWriteArraySet<HistoryRecordingStateChangeObserver>()
    private val copilotSessionObservers = CopyOnWriteArraySet<CopilotSessionObserver>()

    private var savedHasRoutes = false
    private var hasRoutes = false

    private var isDriving = false

    fun registerStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.add(observer)
        if (shouldNotifyOnStart(currentState)) {
            observer.onShouldStartRecording(currentState)
        }
    }

    fun unregisterStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.remove(observer)
    }

    fun registerCopilotSessionObserver(observer: CopilotSessionObserver) {
        copilotSessionObservers.add(observer)
        observer.onCopilotSessionChanged(currentState)
    }

    fun unregisterCopilotSessionObserver(observer: CopilotSessionObserver) {
        copilotSessionObservers.remove(observer)
    }

    fun unregisterAllStateChangeObservers() {
        historyRecordingStateChangeObservers.clear()
    }

    fun unregisterAllCopilotSessionObservers() {
        copilotSessionObservers.clear()
    }

    fun currentCopilotSession(): HistoryRecordingSessionState {
        return currentState
    }

    fun setRoutes(routes: List<NavigationRoute>) {
        savedHasRoutes = hasRoutes
        if (updateHasRoutes(routes.isNotEmpty())) {
            updateStateAndNotifyObservers { observer, state ->
                observer.onShouldStopRecording(state)
            }
        }
    }

    fun lastSetRoutesFailed() {
        if (updateHasRoutes(savedHasRoutes)) {
            updateStateAndNotifyObservers { observer, state ->
                observer.onShouldCancelRecording(state)
            }
        }
    }

    override fun onSessionStateChanged(tripSessionState: TripSessionState) {
        if (updateIsDriving(NavigationSessionUtils.isDriving(tripSessionState))) {
            updateStateAndNotifyObservers { observer, state ->
                observer.onShouldStopRecording(state)
            }
        }
    }

    private fun updateHasRoutes(newValue: Boolean): Boolean {
        return if (hasRoutes != newValue) {
            hasRoutes = newValue
            true
        } else {
            false
        }
    }

    private fun updateIsDriving(newValue: Boolean): Boolean {
        return if (isDriving != newValue) {
            isDriving = newValue
            true
        } else {
            false
        }
    }

    private fun updateStateAndNotifyObservers(
        finishRecordingBlock: (
            HistoryRecordingStateChangeObserver,
            HistoryRecordingSessionState,
        ) -> Unit,
    ) {
        val newState = NavigationSessionUtils.getNewHistoryRecordingSessionState(
            isDriving,
            hasRoutes,
        )
        if (newState::class != currentState::class) {
            val oldState = currentState
            currentState = newState
            if (oldState !is HistoryRecordingSessionState.Idle) {
                historyRecordingStateChangeObservers.forEach {
                    finishRecordingBlock(it, oldState)
                }
            }
            if (shouldNotifyOnStart(newState)) {
                historyRecordingStateChangeObservers.forEach { it.onShouldStartRecording(newState) }
            }
            copilotSessionObservers.forEach { it.onCopilotSessionChanged(newState) }
        }
    }

    private fun shouldNotifyOnStart(state: HistoryRecordingSessionState): Boolean {
        return state !is HistoryRecordingSessionState.Idle
    }
}
