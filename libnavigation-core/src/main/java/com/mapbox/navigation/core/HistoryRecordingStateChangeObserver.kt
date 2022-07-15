package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.trip.session.NavigationSessionState

@UiThread
interface HistoryRecordingStateChangeObserver {

    fun onShouldStartRecording(state: NavigationSessionState)

    fun onShouldStopRecording(state: NavigationSessionState)

    fun onShouldCancelRecording(state: NavigationSessionState)
}
