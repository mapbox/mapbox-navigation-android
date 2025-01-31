package com.mapbox.navigation.base.internal.route

import com.mapbox.api.directions.v5.models.LegAnnotation

@Throws(IllegalArgumentException::class)
internal fun LegAnnotation?.size(): Int {
    if (this == null) {
        throw IllegalArgumentException("LegAnnotation is null")
    }
    listOf(
        duration(),
        speed(),
        distance(),
        congestion(),
        congestionNumeric(),
        maxspeed(),
        freeflowSpeed(),
        currentSpeed(),
        trafficTendency(),
    ).forEach {
        if (it != null) {
            return it.size
        }
    }
    unrecognizedJsonProperties?.forEach {
        if (it.value.isJsonArray) {
            return it.value.asJsonArray.size()
        }
    }
    throw IllegalArgumentException("LegAnnotation is empty")
}
