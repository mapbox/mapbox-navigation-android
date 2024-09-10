package com.mapbox.navigation.tripdata.maneuver.model

/**
 * Represents an error case when obtaining turn icon data
 *
 * @param errorMessage a text error message indicating what went wrong
 * @param type the maneuver type parameter used as input
 * @param degrees the degrees parameter value used as input
 * @param modifier the modifier parameter value used as input
 * @param drivingSide the driving side parameter used as input
 */
class TurnIconError internal constructor(
    val errorMessage: String,
    val type: String?,
    val degrees: Float?,
    val modifier: String?,
    val drivingSide: String?,
)
