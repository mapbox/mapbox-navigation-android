package com.mapbox.navigation.core.internal.replay

import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase

/**
 * Function to replay events without changing the state of the MapboxReplayer and its ReplayEventSimulator.
 *
 * Used for replaying simulated locations during replay pause, for example, when pressing the pause button
 * on the UI or when approaching a drop-off point.
 */
fun MapboxReplayer.replayEvents(events: List<ReplayEventBase>) {
    this.replayEvents(events)
}
