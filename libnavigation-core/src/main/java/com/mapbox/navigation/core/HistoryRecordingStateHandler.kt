package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionUtils
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

@UiThread
internal class HistoryRecordingStateHandler(
    private val scope: CoroutineScope
): TripSessionStateObserver {

    private var currentState: NavigationSessionState = NavigationSessionState.Idle
    private val _sessionIdFlow: MutableSharedFlow<String> = MutableSharedFlow<String>(
        replay = 1,
    ).also {
        it.tryEmit(currentState.sessionId)
    }
    val sessionIdFlow: SharedFlow<String> = _sessionIdFlow.asSharedFlow()

    private val historyRecordingStateChangeObservers =
        CopyOnWriteArraySet<HistoryRecordingStateChangeObserver>()

    private var savedHasRoutes = false
    private var hasRoutes = false

    private var isDriving = false

    fun registerStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.add(observer)
    }

    fun unregisterStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.remove(observer)
    }

    fun unregisterAllStateChangeObservers() {
        historyRecordingStateChangeObservers.clear()
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
        finishRecordingBlock: (HistoryRecordingStateChangeObserver, NavigationSessionState) -> Unit
    ) {
        val newState = NavigationSessionUtils.getNewState(isDriving, hasRoutes)
        if (newState::class != currentState::class) {
            val oldState = currentState
            currentState = newState
            scope.launch {
                if (oldState !is NavigationSessionState.Idle) {
                    historyRecordingStateChangeObservers.forEach {
                        finishRecordingBlock(it, oldState)
                    }
                }
                if (newState !is NavigationSessionState.Idle) {
                    historyRecordingStateChangeObservers.forEach { it.onShouldStartRecording(newState) }
                }
                _sessionIdFlow.emit(newState.sessionId)
            }
        }
    }
}
