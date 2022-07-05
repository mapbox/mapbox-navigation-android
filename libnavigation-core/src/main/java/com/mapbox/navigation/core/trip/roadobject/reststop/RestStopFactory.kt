package com.mapbox.navigation.core.trip.roadobject.reststop

import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.navigation.base.trip.model.roadobject.UpcomingRoadObject
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.utils.internal.ifNonNull

internal object RestStopFactory {

    fun createWithStepIntersection(
        stepIntersections: List<StepIntersection>?
    ): List<RestStopFromIntersection> {
        return stepIntersections?.mapNotNull { stepIntersection ->
            ifNonNull(
                stepIntersection.location(), stepIntersection.restStop()
            ) { location, restStop ->
                location to restStop
            }
        }?.map { (location, restStop) ->
            RestStopFromIntersection
                .Builder(location)
                .name(restStop.name())
                .type(restStop.type())
                .amenities(restStop.amenities())
                .build()
        } ?: emptyList()
    }

    fun createWithUpcomingRoadObjects(
        upcomingRoadObjects: List<UpcomingRoadObject>
    ): List<RestStopFromRoadObject> {
        return if (upcomingRoadObjects.isNotEmpty()) {
            val restStops = upcomingRoadObjects.mapNotNull { upcomingRoadObject ->
                (upcomingRoadObject.roadObject as? RestStop)?.let {
                    RestStopFromRoadObject
                        .Builder(it)
                        .distanceToStart(upcomingRoadObject.distanceToStart)
                        .build()
                }
            }
            restStops
        } else {
            emptyList()
        }
    }
}
