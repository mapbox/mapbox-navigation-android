package com.mapbox.navigation.core.history.model

import com.mapbox.navigator.HistoryRecord

/**
 * Base interface event for the [HistoryRecord]
 *
 * @property eventTimestamp timestamp of event seconds
 */
interface HistoryEvent {
    val eventTimestamp: Double
}
