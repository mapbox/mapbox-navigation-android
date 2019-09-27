package com.mapbox.services.android.navigation.v5.location.replay

import android.location.Location
import android.os.Build

import java.util.ArrayList

private class ReplayJsonRouteLocationMapper(private val replayLocations: List<ReplayLocationDto>) {
    private val NON_NULL_AND_NON_EMPTY_REPLAY_LOCATION_LIST_REQUIRED = "Non-null and non-empty replay " + "location list required."
    private val REPLAY = "ReplayLocation"

    init {
        checkValidInput(replayLocations)
    }

    fun toLocations(): List<Location> {
        return mapReplayLocations()
    }

    private fun checkValidInput(locations: List<ReplayLocationDto>?) {
        val isValidInput = locations == null || locations.isEmpty()
        require(!isValidInput) { NON_NULL_AND_NON_EMPTY_REPLAY_LOCATION_LIST_REQUIRED }
    }

    private fun mapReplayLocations(): List<Location> {
        val locations = ArrayList<Location>(replayLocations.size)
        for (sample in replayLocations) {
            val location = Location(REPLAY)
            location.longitude = sample.longitude
            location.accuracy = sample.horizontalAccuracyMeters
            location.bearing = sample.bearing.toFloat()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.verticalAccuracyMeters = sample.verticalAccuracyMeters
            }
            location.speed = sample.speed.toFloat()
            location.latitude = sample.latitude
            location.altitude = sample.altitude
            val date = sample.date
            if (date != null) {
                location.time = date.time
            }
            locations.add(location)
        }
        return locations
    }
}
