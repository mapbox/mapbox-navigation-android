package com.mapbox.navigation.metrics.internal

import com.mapbox.common.TelemetryUtils

object TelemetryUtilsDelegate {

    fun getEventsCollectionState(): Boolean = TelemetryUtils.getEventsCollectionState()

    fun setEventsCollectionState(enableCollection: Boolean) {
        TelemetryUtils.setEventsCollectionState(enableCollection, {})
    }
}
