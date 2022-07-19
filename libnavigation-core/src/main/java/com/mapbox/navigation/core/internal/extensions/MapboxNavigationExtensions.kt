@file:JvmName("MapboxNavigationExtensions")

package com.mapbox.navigation.core.internal.extensions

import androidx.annotation.UiThread
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver

/**
 * Register [HistoryRecordingStateChangeObserver]. Use this method to receive notifications
 * regarding history recording: when to start, stop or cancel recording.
 * NOTE: call this method before [MapboxNavigation.startTripSession] and
 * [MapboxNavigation.setNavigationRoutes] invocations.
 *
 * @param observer callback to receive notifications.
 */
@UiThread
fun MapboxNavigation.registerHistoryRecordingStateChangeObserver(
    observer: HistoryRecordingStateChangeObserver
) {
    historyRecordingStateHandler.registerStateChangeObserver(observer)
}

/**
 * Unregister [HistoryRecordingStateChangeObserver].
 * See [MapboxNavigation.registerHistoryRecordingStateChangeObserver] for more info.
 *
 * @param observer callback to stop receiving notifications.
 */
@UiThread
fun MapboxNavigation.unregisterHistoryRecordingStateChangeObserver(
    observer: HistoryRecordingStateChangeObserver
) {
    historyRecordingStateHandler.unregisterStateChangeObserver(observer)
}
