package com.mapbox.navigation.core.internal.history

import androidx.annotation.RestrictTo
import com.mapbox.navigation.core.history.MapboxHistoryRecorder

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface HistoryRecordingEnabledObserver {

    fun onEnabled(historyRecorderHandle: MapboxHistoryRecorder)

    fun onDisabled(historyRecorderHandle: MapboxHistoryRecorder)
}
