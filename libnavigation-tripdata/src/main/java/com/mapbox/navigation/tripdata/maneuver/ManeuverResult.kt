package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.navigation.tripdata.maneuver.model.Maneuver

internal sealed class ManeuverResult {

    sealed class GetManeuverList : ManeuverResult() {
        data class Failure(val error: String?) : GetManeuverList()
        data class Success(val maneuvers: List<Maneuver>) : GetManeuverList()
    }

    sealed class GetManeuverListWithProgress : ManeuverResult() {
        data class Failure(val error: String?) : GetManeuverListWithProgress()
        data class Success(val maneuvers: List<Maneuver>) : GetManeuverListWithProgress()
    }
}
