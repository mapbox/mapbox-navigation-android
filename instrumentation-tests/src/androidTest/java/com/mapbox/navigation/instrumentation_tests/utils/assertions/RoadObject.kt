package com.mapbox.navigation.instrumentation_tests.utils.assertions

import com.mapbox.navigation.base.trip.model.roadobject.RoadObject

fun RoadObject.compareIdWithIncidentId(incidentId: String): Boolean = id.contains(incidentId)
