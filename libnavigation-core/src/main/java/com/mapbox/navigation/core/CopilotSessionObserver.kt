package com.mapbox.navigation.core

import com.mapbox.navigation.core.internal.HistoryRecordingSessionState

internal fun interface CopilotSessionObserver {

    fun onCopilotSessionChanged(session: HistoryRecordingSessionState)
}
