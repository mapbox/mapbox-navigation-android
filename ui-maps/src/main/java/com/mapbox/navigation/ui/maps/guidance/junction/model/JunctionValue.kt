package com.mapbox.navigation.ui.maps.guidance.junction.model

import android.graphics.Bitmap

/**
 * The state is returned when the junction is ready to be rendered on the UI
 * @property bitmap contains the junction.
 */
class JunctionValue internal constructor(
    val bitmap: Bitmap,
)
