package com.mapbox.navigation.ui.maps.guidance.junction.model

/**
 * The state is returned when the junction is ready to be rendered on the UI
 * @property bytes contains the junction
 */
class JunctionValue internal constructor(
    val bytes: ByteArray
)
