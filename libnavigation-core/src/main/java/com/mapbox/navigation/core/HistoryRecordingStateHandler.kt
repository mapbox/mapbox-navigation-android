package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.utils.internal.logD

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
        val prevState = currentState
        logD("onNavigationSessionStateChanged from $prevState to $navigationSession", TAG)
        if (prevState::class != navigationSession::class) {
            if (prevState !is NavigationSessionState.Idle) {
                historyRecordingStateChangeObservers.forEach {
                    it.onShouldStopRecording(prevState)
                }
            }
            if (navigationSession !is NavigationSessionState.Idle) {
                historyRecordingStateChangeObservers.forEach {
                    it.onShouldStartRecording(navigationSession)
                }
            }
            currentState = navigationSession
        }
    }

}
