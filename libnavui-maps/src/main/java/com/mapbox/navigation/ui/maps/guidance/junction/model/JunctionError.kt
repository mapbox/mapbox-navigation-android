package com.mapbox.navigation.ui.maps.guidance.junction.model

/**
 * The state is returned if there is an error retrieving junction.
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class JunctionError internal constructor(
    val errorMessage: String?,
    val throwable: Throwable?,
)
