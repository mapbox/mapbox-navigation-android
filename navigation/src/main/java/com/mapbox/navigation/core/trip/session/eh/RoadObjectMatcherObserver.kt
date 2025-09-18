package com.mapbox.navigation.core.trip.session.eh

import androidx.annotation.WorkerThread
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.trip.model.roadobject.RoadObject
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectMatcherError

/**
 * Road objects matching listener. Callbacks are fired when matching is finished.
 * Notifications are delivered on background thread.
 */
fun interface RoadObjectMatcherObserver {
    /**
     * Road object matching result.
     */
    @WorkerThread
    fun onRoadObjectMatched(result: Expected<RoadObjectMatcherError, RoadObject>)
}
