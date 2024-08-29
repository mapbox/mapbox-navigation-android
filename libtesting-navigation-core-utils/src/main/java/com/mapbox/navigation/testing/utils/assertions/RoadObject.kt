package com.mapbox.navigation.testing.utils.assertions

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject

fun RoadObject.compareIdWithIncidentId(incidentId: String): Boolean = id.contains(incidentId)
