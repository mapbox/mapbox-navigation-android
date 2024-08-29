package com.mapbox.navigation.ui.maps.guidance.restarea.model

import android.graphics.Bitmap

/**
 * The state is returned when the service/parking area guide map is ready to be rendered on the UI.
 * @property bitmap contains the service/parking area guide map.
 */
class RestAreaGuideMapValue internal constructor(
    val bitmap: Bitmap,
)
