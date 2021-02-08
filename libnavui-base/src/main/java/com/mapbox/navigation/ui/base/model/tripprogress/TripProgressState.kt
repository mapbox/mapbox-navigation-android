package com.mapbox.navigation.ui.base.model.tripprogress

import com.mapbox.navigation.ui.base.MapboxState

/**
 * Represents a trip progress state to be rendered
 */
sealed class TripProgressState : MapboxState {

    /**
     * Represents a trip progress update state to be rendered
     *
     * @param tripProgressUpdate update data
     * @param formatter an object containing various types of formatters
     */
    class Update(
        val tripProgressUpdate: TripProgressUpdate,
        val formatter: TripProgressUpdateFormatter
    ) : TripProgressState()
}
