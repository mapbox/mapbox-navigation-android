package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.utils.internal.logI

private const val TAG = "[HistoryRecordingStateHandler]"

@UiThread
internal class HistoryRecordingStateHandler(
    private var currentState: NavigationSessionState
) : NavigationSessionStateObserver {

    private val historyRecordingStateChangeObservers =
        mutableSetOf<HistoryRecordingStateChangeObserver>()

    fun registerHistoryRecordingStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.add(observer)
    }

    fun unregisterHistoryRecordingStateChangeObserver(observer: HistoryRecordingStateChangeObserver) {
        historyRecordingStateChangeObservers.remove(observer)
    }

    override fun onNavigationSessionStateChanged(navigationSession: NavigationSessionState) {
        logI("onNavigationSessionStateChanged from $currentState to $navigationSession", TAG)
        onNavigationSessionStateChanged(navigationSession) { observer, state ->
            observer.onShouldStopRecording(state)
        }
    }

    fun onNavigationSessionStateChangeReverted(toState: NavigationSessionState) {
        logI("onNavigationSessionStateChangeReverted from $currentState to $toState", TAG)
        onNavigationSessionStateChanged(toState) { observer, state ->
            observer.onShouldCancelRecording(state)
        }
    }

    private fun onNavigationSessionStateChanged(
        newState: NavigationSessionState,
        stopRecordingBlock: (HistoryRecordingStateChangeObserver, NavigationSessionState) -> Unit
    ) {
        val prevState = currentState
        if (prevState::class != newState::class) {
            if (prevState !is NavigationSessionState.Idle) {
                historyRecordingStateChangeObservers.forEach {
                    stopRecordingBlock(it, prevState)
                }
            }
            if (newState !is NavigationSessionState.Idle) {
                historyRecordingStateChangeObservers.forEach {
                    it.onShouldStartRecording(newState)
                }
            }
            currentState = newState
        }
    }
}
