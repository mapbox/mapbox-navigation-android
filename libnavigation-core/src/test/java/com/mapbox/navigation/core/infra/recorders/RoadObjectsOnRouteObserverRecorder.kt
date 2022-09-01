package com.mapbox.navigation.core.infra.recorders

import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.core.trip.session.RoadObjectsOnRouteObserver

class RoadObjectsOnRouteObserverRecorder : RoadObjectsOnRouteObserver {

    private val _records = mutableListOf<List<UpcomingRoadObject>>()
    val records: List<List<UpcomingRoadObject>> get() = _records

    override fun onNewRoadObjectsOnTheRoute(roadObjects: List<UpcomingRoadObject>) {
        _records.add(roadObjects)
    }
}
