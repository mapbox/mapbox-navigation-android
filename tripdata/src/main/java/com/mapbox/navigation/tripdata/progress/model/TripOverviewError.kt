package com.mapbox.navigation.tripdata.progress.model

/**
 * The state is returned if there is an error calculating trip overview.
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class TripOverviewError internal constructor(
    val errorMessage: String?,
    val throwable: Throwable?,
)
