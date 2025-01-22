package com.mapbox.navigation.ui.maps.guidance.restarea.model

/**
 * The state is returned if there is an error retrieving service/parking area guide map.
 * @param message String
 * @param throwable an optional throwable value expressing the error
 */
class RestAreaGuideMapError internal constructor(
    val message: String?,
    val throwable: Throwable?,
)
