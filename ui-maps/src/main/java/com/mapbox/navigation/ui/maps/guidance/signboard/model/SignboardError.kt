package com.mapbox.navigation.ui.maps.guidance.signboard.model

/**
 * The state is returned if there is an error retrieving signboard.
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class SignboardError internal constructor(
    val errorMessage: String?,
    val throwable: Throwable?,
)
